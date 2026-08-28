# App ↔ Server Sync — plan

**Date:** 20-Aug-2026 · **Repos:** `Pustakm` (KMM app) + `PustakmServer` (Express) · **Status:** both phases written, **not compiled**. Test guide: `20aug2026-FBIALBD-sync-test-guide.md`

---

## Context

The server side of sync is **already built**. `PustakmServer` exposes `POST /sync/:userId/push` and
`GET /sync/:userId/pull` (`middleware/syncHandler.js` → `services/syncService.js`), media upload at
`POST /images` and download at `GET /media/:userId/:assetId`. The app has none of it: `ApiRoute`
knows only `LOGIN/REGISTER/NOTES/PROFILE/USERS`, and every remote call inside `NoteRepository` is
commented out. `known-issues.md` K3 is exactly this — "no sync engine".

So this work is almost entirely **client-side**. The local SQLite DB stays the source of truth; the
engine reconciles it with the server in the background and never blocks the UI.

Three problems have to be solved before any of that works:

1. **Auth is not actually functional over the wire.** `currentTokenOrNull()` reads
   `userPreferenceStateFlow`, a `stateIn(..., WhileSubscribed())` that nothing ever collects — so it
   is stuck on its initial empty value and the `Authorization` header is likely never sent. The
   access token expires in 15 minutes, the app never reads `data.refreshToken` from the login
   response, and `/auth/refresh` is never called. Android's `BaseViewModel` force-logs-out on *any*
   401. A background sync engine on top of that would 401 and log the user out.
2. **Deletes are destructive.** `deleteNoteByIdFromDb` runs `DELETE FROM Notes`. A hard delete can
   never reach another device — the deletion has to survive as a tombstone.
3. **`localPath` is device state on the wire.** `MediaContent.localPath` is a full absolute path
   (`/data/user/0/...`, `/var/mobile/Containers/...`) and is currently serialized outbound. The
   server already strips it (`backend.md` S1-11 / E22), but the app should not send it at all.

**Outcome:** a note edited offline on one device appears on the other after both come back online,
with its media, without the user doing anything, and without any existing flow changing behaviour.

---

## Decisions

| Question | Decision |
|---|---|
| Auth | Fix properly — store both tokens, fix the provider, add refresh + single retry |
| Media bytes | **Eager both ways** — upload with the note, download on arrival in the background |
| `localPath` / `thumbnailPath` | **Device-only.** Stripped client-side before push; never read from a pull |
| Delivery | **Phased.** Phase 1 notes, Phase 2 media. Each phase compiles and runs on its own |
| Base URL | Unchanged (still the ngrok tunnel in `NetworkClient.kt`) |
| Conflict | Note-granular last-write-wins on **server** time; loser archived server-side to `note_versions` |
| Architecture | Unchanged. Shared Kotlin data/domain, native UI per platform. No new Gradle module |

---

## The wire contract (already fixed by the server)

Timestamps agree today and need no breaking change: the client writes `"${getCurrentTimestamp()}"`
(epoch millis as a String) and the server's `lib/time.js` `toWire()` emits `String(millis)`.
**`Note.updatedAt` stays `String?`** — the `String?` → `Long?` migration in `backend.md` is not needed.

**Push** — `POST /sync/:userId/push`, body `{ deviceId?, lastPulledAt?, notes: [...] }`, max
`SYNC_MAX_NOTES_PER_PUSH` per batch. Response:

```
{ serverTime, accepted: [{id, version, serverUpdatedAt}],
  conflicts: [{id, reason, server: <note>}],
  rejected:  [{id, code, message, fields}] }
```

Per-note results, never all-or-nothing, and idempotent on `(noteId, version)`.

**Pull** — `GET /sync/:userId/pull?since=<serverUpdatedAt>&sinceId=<id>&limit=<n>`. Response:

```
{ serverTime, notes: [...], hasMore, nextSince, nextSinceId }
```

`since` is always a **server** timestamp the server previously handed us — never a device clock
reading. Paging is on `(serverUpdatedAt, _id)`; the id half is the tie-breaker inside one
millisecond. A `since` older than the tombstone-retention window returns
`PULL_WATERMARK_TOO_OLD` → the client must do a full resync from `since=0`.

**Auth** — `/login`, `/register` and `/auth/refresh` all return
`data = { ...user, accessToken, refreshToken }` and *also* set an `authorization: Bearer <jwt>`
response header. Access token TTL 15m; refresh token is opaque, 30 days, and **rotates** on every
use — reusing an old one kills the whole session family. Every protected route needs
`Authorization: Bearer <accessToken>`, and where the route has `:userId` it must equal the JWT `sub`
or the server answers **403, not 401**.

---

## Phase 1 — notes sync

### 1. Auth (`:core:database`, `:core:network`, `:core:model`, `:androidApp`)

- `User.kt` — add `accessToken: String?` and `refreshToken: String?` (optional, so nothing else breaks).
- `IAppPreferences` / `BasePreferences` — add `setRefreshToken` / `getRefreshToken`, and change
  `userPreferenceStateFlow` from `WhileSubscribed()` to `Eagerly` so `currentTokenOrNull()` is real.
  This alone is what makes every authenticated call start working.
- `AuthRepository.loginUser` / `registerUser` — persist `accessToken` **and** `refreshToken` from the
  body, and set `userId` + auth in one place. Register currently sets neither; it will now match login.
- `BaseClient` — on 401, call `/auth/refresh` once, store the rotated pair, replay the original
  request once. Only if the refresh itself fails does the error become the new
  `NetworkError.SESSION_EXPIRED`.
- `NetworkError` — add `SESSION_EXPIRED`, `FORBIDDEN`, `BAD_REQUEST`. Fix the status mapping: 400
  currently maps to `NOT_FOUND`, 403 and 429 are unmapped (429 falls to `UNKNOWN` despite
  `TOO_MANY_REQUESTS` existing).
- `BaseViewModel.onFailure` — log out on `SESSION_EXPIRED` only, not on every `UNAUTHORIZED`.
  Same intent on iOS via the bridge's error code.

### 2. Local DB (`:core:database`) — migration `7.sqm`

Additive only; no column is dropped or retyped.

- `Notes`: `+ deletedAt TEXT`, `+ serverUpdatedAt TEXT`, `+ lastSyncedAt TEXT`
- `NoteContent`: `+ assetId TEXT`, `+ checksum TEXT` (added now so Phase 2 needs no second migration)
- New `SyncState` table: one row per user — `lastPulledAt`, `lastPulledId`, `deviceId`

New queries in `NotesDatabase.sq`: `selectDirtyNotes`, `markNoteSynced`, `softDeleteNoteById`,
`selectSyncState` / `upsertSyncState`, `hardDeleteSyncedTombstones`.

> **The regression risk in this phase.** Making delete a tombstone means every existing read must
> stop returning deleted rows. `selectAll`, `selectById`, `selectWithAllContent`,
> `selectNoteIdsPage`, `selectNoteSummariesPage`, `searchTitles` and `searchContentTextLike` all get
> `deleted = 0`. Miss one and deleted notes reappear in the list — this is the single thing to check
> hardest during verification.

`NotesDao` gains `selectDirtyNotes()`, `applyServerNote()` (writes a pulled note **without** bumping
`version`/`syncStatus`, otherwise pulling would immediately re-dirty every note and loop), and
`softDeleteNoteById()`.

### 3. Wire DTOs and API calls (`:core:model`, `:core:network`)

- `core/model/.../sync/` — `SyncPushRequest`, `SyncPushResponse`, `SyncAccepted`, `SyncConflict`,
  `SyncRejected`, `SyncPullResponse`.
- `ApiRoute` — add `SYNC`, `AUTH_REFRESH` (and `IMAGES`, `MEDIA` for Phase 2).
- `ApiCallClient` — `syncPush()`, `syncPull()`, `refreshSession()`.
- `NoteWireMapper` in `:feature:notes` — the one place that knows the wire shape. Strips `localPath`
  and `thumbnailPath` on the way out; on the way in, **preserves the local row's `localPath` /
  `thumbnailPath`** rather than nulling them, so a pull never orphans a file that is already on disk.

### 4. The engine (`:feature:notes`)

`data/sync/SyncEngine.kt` — named to avoid the existing `INoteSyncRepository`, which is an in-app
content event bus, not server sync.

- **Single-flight.** A `Mutex` means overlapping triggers coalesce instead of double-pushing.
- **Order:** push local dirty work first, then pull, so a just-made local edit is never overwritten
  by a stale pull.
- **Push:** `selectDirtyNotes()` → batches → on `accepted`, `markNoteSynced(version, serverUpdatedAt)`;
  on `conflicts`, apply the server note (server wins; the losing edit is already archived server-side);
  on `rejected`, leave dirty and log — a rejected note must never be silently marked clean.
- **Pull:** loop on `nextSince`/`nextSinceId` while `hasMore`, `applyServerNote()` each, tombstones
  applied as local soft deletes, watermark saved **only after the page is committed** so a crash
  mid-page re-pulls rather than skips. `PULL_WATERMARK_TOO_OLD` → restart from `since=0`.
- **Backoff:** exponential with a cap, reset on success. Offline is not an error — it parks.
- **State:** `SyncState` flow (`Idle` / `Syncing` / `Success(at)` / `Failed(error)` / `Offline`)
  for the UI, plus `syncNow()` and `onConnectivityChanged(isOnline)` as the only entry points.
- Use cases `SyncNowUseCase` / `ObserveSyncStateUseCase`, registered in `NotesModule`.

`NoteRepository` keeps its `_notes` / `_noteSummaries` StateFlows correct by having the engine write
**through the repository**, not straight to the DAO — otherwise the list screen shows stale data
until it is re-queried.

### 5. Triggers — native per platform

Connectivity stays on the platform side; shared code only receives `onConnectivityChanged`.

- **Android:** a `SyncWorker` with `NetworkType.CONNECTED`, enqueued as unique periodic work from
  `PustakmApplication`, plus one-shot expedited work after a save. WorkManager is already a
  dependency (`libs.androidx.work.runtime.ktx`) and handles "came back online" natively.
- **iOS:** `SyncBridge.kt` in `feature/notes/iosMain` + a Swift `SyncController` using `NWPathMonitor`
  for connectivity and `BGTaskScheduler` (`requiresNetworkConnectivity`) for background runs, wired
  in `iOSApp.swift` on scene-phase change.

Trigger set, identical on both: app start · connectivity regained · debounced after a local save ·
periodic · manual pull-to-refresh.

---

## Phase 2 — media ✅ written

Same engine, one more stage. `MediaSyncer` (`:feature:notes/data/sync/`).

- **Up:** any `MediaContent` with a `localPath` and no `assetId` uploads to `POST /images`
  (multipart, field name `files`) → `{assetId, url, mimeType, sizeBytes, checksum}` is written onto
  the content row, then the note is pushed. Media uploads **before** its note, so a note never
  references an asset the server does not have yet.
- **Down:** eager. On a pulled note, any content with an `assetId` and no local file is queued and
  fetched from `GET /media/:userId/:assetId` in the background.
- **Landing bytes correctly:** write via `PathPolicy.importPath(noteId, fileName)`, then store the
  resulting **absolute** path in `localPath` — that is the only form `resolveLocalFilePath()` and
  every existing playback call site expect. Thumbnails are regenerated locally, never transferred.
- Dedupe by `checksum` — the server already returns the existing asset for a duplicate hash.
- **Nothing throws.** A file that cannot move is skipped and counted; one oversized video must not
  stop a hundred notes syncing. Cap is `SyncConfig.MAX_MEDIA_BYTES` (25 MB), matched to the server's
  `UPLOAD_MAX_BYTES` default. Raise both together.
- Backfill is budgeted per cycle (`MEDIA_FILES_PER_CYCLE`), driven by `selectNoteIdsNeedingMedia`,
  so leftovers from a capped cycle are picked up next run rather than lost.

### Two things Phase 2 needed that did not exist

**A path seam.** `localPath` is absolute; every `:core:filesys` interface takes a path relative to
app storage, and nothing in the repo converted between them. Added `StoragePaths` (`rootPath()` is
the only platform-specific part; the arithmetic is shared and unit-tested). `:feature:notes` now
depends on `:core:filesys`.

**Server-side upload types.** `text/plain` and `text/markdown` have no magic bytes, so
`fileTypeFromBuffer` could never identify them and every `.txt`/`.md` attachment would have been
rejected as an unsupported type. `.epub` was simply not on the allowlist. Both fixed in
`constants/mimeType.js` + `fileupload/upload.js`, with the text fallback gated on the bytes actually
decoding as UTF-8 — the client's claimed MIME alone is not enough.

### Known gap

Thumbnails are generated natively per platform (`FileOps.kt` / `FileOps.swift`) and are **not**
transferred. A freshly downloaded image renders from the file itself and has no thumbnail until the
platform makes one. Not a data-loss issue; worth a follow-up.

---

## Files

**Changed:** `core/model/.../response/User.kt` · `core/database/.../preferences/{IAppPrefrences,Basepreferences}.kt` ·
`core/database/.../sqldelight/.../NotesDatabase.sq` · `core/database/.../localdb/database/NotesDao.kt` ·
`core/network/.../{ApiRoute,ApiCallClient,BaseClient}.kt` · `core/common/.../util/NetworkError.kt` ·
`feature/auth/.../repository/AuthRepository.kt` · `feature/notes/.../repositoryImpl/NoteRepository.kt` ·
`feature/notes/.../di/NotesModule.kt` · `androidApp/.../screen/base/BaseViewModel.kt` ·
`androidApp/.../PustakmApplication.kt` · `iosApp/iosApp/iOSApp.swift`

**New:** `core/database/.../sqldelight/.../7.sqm` · `core/model/.../sync/Sync*.kt` ·
`feature/notes/.../data/sync/{SyncEngine,NoteWireMapper}.kt` ·
`feature/notes/.../domain/repository/IRemoteSyncRepository.kt` ·
`feature/notes/.../domain/usecase/SyncUseCase.kt` · `feature/notes/iosMain/.../bridge/SyncBridge.kt` ·
`androidApp/.../sync/SyncWorker.kt` · `iosApp/iosApp/network/SyncController.swift`

Nothing is deleted. Old entry points stay as delegates, per project rule.

---

## Verification

```bash
cd Pustakam/Pustakm
./gradlew build                                        # compiles
./gradlew :core:filesys:allTests                       # existing suite must stay green
./gradlew :feature:notes:allTests                      # new SyncEngine + mapper tests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # THE iOS check — a missing export() only fails here
./gradlew :androidApp:assembleDebug
```

Then Xcode → Clean Build Folder → Build.

The two decoupling greps must both still return nothing:

```bash
grep -rn "BasePreferences\|get<Note.*Repository>" androidApp/src --include=*.kt
grep -rln "repositoryImpl" androidApp/src
```

**New unit tests** (pure Kotlin, no device): wire mapper strips `localPath`/`thumbnailPath` and
preserves them on inbound merge · conflict resolution picks the server note · watermark only advances
on a committed page · a rejected note stays dirty · backoff resets on success.

**End-to-end, by hand:** `cd PustakmServer && npm start` (and `npm test` with Mongo up — 49 tests
that have never been run on real Mongo). Then: sign in on two devices → edit offline on A → airplane
mode off → the note appears on B. Delete on B → it disappears on A. Edit the same note on both while
offline → the newer edit wins and the older is in `note_versions`, not gone.

**Regression checks, specifically:** the note list, search, open, delete and export flows after the
soft-delete change — deleted notes must not appear in the list, in search results, or in an export.

---

## Not doing

- Changing the base URL, or turning off `AuthConfig.BYPASS_AUTH` (already `false`)
- Per-content-block conflict merging — note-granular LWW is the locked decision
- E2EE, TypeScript migration, `src/` restructure on the server — all separately deferred
- Any change to the UI/presentation layer beyond surfacing sync state
