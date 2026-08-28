# Sync — what to test

Companion to `20aug2026-FBIALBD-app-server-sync.md`. Both phases are written; **nothing has been
compiled or run.** Work top to bottom — if step 0 fails there is no point doing step 5.

You need **two devices** (or one device + one simulator/emulator) signed into the **same account**,
and the server running.

---

## 0 — It builds

```bash
cd PustakmServer && npm start          # server up first
```

```bash
cd Pustakam/Pustakm
./gradlew build
./gradlew :core:filesys:allTests                       # 64 existing + 5 new StoragePaths tests
./gradlew :feature:notes:allTests                      # 8 new wire-mapper tests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # the only thing that catches a missing export()
./gradlew :androidApp:assembleDebug
```

Then Xcode → Clean Build Folder → Build.

If `verifyMigrations` complains, run `./gradlew generateNotesDatabaseSchema` first — the
`databases/` snapshots stop at `3.db`.

> `npm test` on the server: **70 pass, 1 fails, and that failure is not mine.**
> `auth.test.js:123` expects `fields.email` on an empty login body. The 19-Aug change made `email`
> optional behind a `.refine()`, and Zod skips the refine when `password` already failed — so only
> `password` is reported. The test is stale, not the code. Fix by asserting on `password` only, or
> by moving the either/or check into `superRefine`.

---

## 1 — Nothing broke (do this before any sync test)

This is the part most likely to bite, because **delete is now a tombstone instead of a row deletion**.
On ONE device, with the server irrelevant:

| Check | Expected |
|---|---|
| Open the notes list | Every note still there, same order |
| Delete a note | Gone from the list, and **still gone after force-quit and relaunch** |
| Search for text that was in the deleted note | **No hit** |
| Search for text in a live note | Hit, as before |
| Export a note to PDF / DOCX | Same output as before |
| Open a note, edit, back out, reopen | Edit persisted |
| Add a photo, audio, PDF, link, location | All render as before |
| Open a book/document reader, close, reopen | Resumes on the same page |
| Delete a brand-new note you never saved | Succeeds, no error toast |

If a deleted note reappears in **any** list, search result or export, a query is missing its
`deleted = 0` — tell me which screen and I will find it.

---

## 2 — Auth actually authenticates

This never worked before, so treat it as new.

1. **Log in.** Should succeed as usual.
2. **Sign up a brand new account.** Previously registering stored no session at all; you should now
   land signed in, with sync working immediately.
3. **Leave the app open for ~20 minutes**, then edit a note.
   - The access token expires at 15 minutes. You should see **no logout and no error** — a refresh
     happens silently underneath.
   - Before this change, this is where you would have been kicked to the login screen.
4. **Log out.** Should still work, and should not wipe your theme or reading-mode preference.

---

## 3 — Notes sync, both directions

Device A and device B, same account.

1. On A, create a note "hello from A". Wait ~5 seconds (there is a 3-second debounce after a save).
2. On B, pull to refresh / reopen the app. **"hello from A" appears.**
3. On B, edit it to "edited on B". On A, refresh. **A shows "edited on B".**
4. On B, delete it. On A, refresh. **It disappears on A too.** (This is the tombstone doing its job —
   before, a delete could never leave the device.)

---

## 4 — Offline, then online (the main event)

1. On A, turn on **airplane mode**.
2. Create two notes and edit an existing one. Everything should work normally — the local DB is
   still the source of truth.
3. Turn airplane mode **off**. **Do not touch the app.**
4. Within a minute or so, check B. **All three changes are there.**

On Android that flush is WorkManager's `NetworkType.CONNECTED` constraint firing. On iOS it is
`NWPathMonitor`. If it does not fire, put the app in the foreground and see whether it syncs then —
that tells us whether the engine or the trigger is at fault.

---

## 5 — Conflict

1. Both A and B offline.
2. Edit the **same note** on both, to different text.
3. Bring A online, wait for it to sync. Then bring B online.

**Expected:** the later edit wins, and the losing one is **not gone** — it is archived server-side.
Check with:

```js
// mongo shell
db.note_versions.find({ noteId: "<the note id>" }).sort({ archivedAt: -1 }).limit(2)
```

If the losing edit is not in `note_versions`, that is a real bug — tell me.

---

## 6 — Media

1. On A, add **a photo, an audio recording and a PDF** to a note. Wait for a sync.
2. On B, refresh. The note arrives; the files download in the background.
3. On B, **open the photo, play the audio, open the PDF.** All three should work.
4. Force-quit B, reopen, open them again — they should come from disk, not re-download.

Also worth trying, because these are the cases that were broken or unsupported:

| Case | Expected |
|---|---|
| Attach a `.txt` or `.md` file | Uploads (the server used to reject text — magic-byte sniffing cannot see it; there is now a UTF-8 fallback) |
| Attach an `.epub` | Uploads (was not on the server's allowlist at all) |
| Attach a file **over 25 MB** | Note syncs, **file does not**, and the log says why. This is the server's `UPLOAD_MAX_BYTES` — raise it there and in `SyncConfig.MAX_MEDIA_BYTES` together if you want bigger |
| Add the same photo to two notes | Uploads once — the server dedupes on checksum |
| A note that already has its photo locally, pulled again | The photo is **not** re-downloaded and **not** orphaned |

**On B, thumbnails may be missing at first** for downloaded media. That is expected in this pass —
thumbnails are generated natively per platform and are not transferred. Images still render from the
file itself.

---

## What to send me if something fails

The log lines are tagged. Filter for these:

```
SyncRepository : sync done: pushed=… pulled=… mediaUp=… mediaDown=…
SyncRepository : push rejected …          ← a note the server refused
SyncRepository : kept local unpushed edit ← a pull declined to overwrite your edit
MediaSyncer    : skipped …                ← a file that did not move, and why
SyncWorker     : sync work finished: …    ← Android background run
```

Plus: which step number, which device, and whether the server logged anything at the same moment.

---

## 28-Aug-2026 — five defects found after the first real two-device run

Symptom reported: *"a note created on Android reached iOS once, then nothing — and reopening the app
does not help."* Plus: *"sign in with a phone number does not work."*

Every one of these is **app-side**. The server's push/pull, its `(serverUpdatedAt, _id)` paging and
its conflict archival were read line by line and are correct.

| # | Where | What was wrong |
|---|---|---|
| 1 | `SyncRepository.runCycle` | A push error `return`ed before the pull ran. One note the server would not take stopped **every incoming change**, permanently. |
| 2 | `SyncRepository.pullChanges` | The watermark advanced past notes the engine had **declined to write**. The server never offers the same note twice, so those notes were gone from that device for good. This is the one that matches the symptom exactly. |
| 3 | `SyncRepository.pushDirtyNotes` | On a conflict the server's winning copy was applied through the same guard that refuses dirty notes — and a conflicted note is dirty by definition. So it was refused, stayed dirty, and was re-pushed and re-conflicted forever. With a full batch the `while(true)` never terminated, which the server's 120-requests/hour sync limiter then answered with 429. |
| 4 | `BaseClient.refreshSession` | **Any** non-2xx from `/auth/refresh` cleared both tokens. A 502 from the ngrok tunnel was enough to silently end the session: the app still looked signed in, and nothing synced again, across restarts. |
| 5 | `String+ext.kt` / `AuthRepository` | Sign-up stored the phone as typed there; sign-in matched the phone as typed here. One space or a `+91` on one of the two screens turned a correct number into "invalid credentials". `isValidPhone` also rejected any formatting outright, so the request never left the device. |

### What changed

- Push and pull are independent halves; a failure in one no longer cancels the other.
- A pull stops at the first note it may not overwrite and leaves the watermark **behind** it. After
  `SyncConfig.MAX_STALLED_CYCLES` (3) the server copy is taken anyway, so one unsendable note can
  never block the device's inbound stream forever.
- Conflicts apply the server's note unconditionally — the losing edit is already in `note_versions`.
- Every note gets exactly one push attempt per cycle. The loop cannot spin.
- Only a 401/403 from `/auth/refresh` ends a session. A gateway or network error leaves the tokens.
- Phone numbers are normalised to one shape (`+` plus digits) before they are validated, sent, or
  matched — client-side in `AuthRepository`, server-side in `db/usersdb.js` on both write and read.
  Existing rows still match, because the lookup tries every spelling of the same number.
- `SyncConfig.RESYNC_GENERATION` — **both your devices currently hold a watermark the old code
  advanced past.** The first cycle on the new build resets it once and re-pulls everything, so the
  notes that were skipped come back. No reinstall, no re-login.

### Verifying

`test/sync-and-phone-login.test.js` in `PustakmServer` is the regression test for both reports:
sign-in by phone in three different spellings, and a *second* note reaching the other device after
the first one already did. It needs Mongo (`npm test`).

On the devices, the log lines to watch are:

```
SyncRepository : watermark reset for sync generation 1 — re-pulling everything once
SyncRepository : sync done: pushed=… pulled=…
SyncRepository : pull paused at <noteId>      ← now pauses instead of skipping
```

---

## 28-Aug-2026 (2) — why nothing synced after the first note, and the triggers that were missing

The engine was fine. **Almost nothing ever called it.**

| Trigger | Before | Now |
|---|---|---|
| App start / sign-in | ✅ worked — this is why note 1 crossed | unchanged |
| **On save** | ❌ It listened for the *summaries flow* to move, which only happens inside `insertOrUpdateNote`. Nothing called the engine directly. | `NoteRepository` calls `remoteSync.requestSyncSoon()` on every save **and** every delete. Debounced 1.2 s, so typing is one push. |
| **Android: back to the foreground** | ❌ did not exist. iOS had it via `scenePhase`. | `MainActivity.onResume()` nudges the engine. |
| **Android: network came back** | ❌ did not exist in-process. Only WorkManager's 15-minute run noticed. | `PustakmApplication` registers a `ConnectivityManager.NetworkCallback` — the Android twin of iOS's `NWPathMonitor`. Needs the new `ACCESS_NETWORK_STATE` permission. |
| **Manual** | ❌ no way to force a sync, and no way to see why one failed | Pull to refresh on **Notes** and **Note editor**, on **both** platforms. |
| Periodic (15 min) | ✅ | unchanged |
| Offline → queued locally | ✅ SQLite was always the source of truth | unchanged; it now actually flushes on reconnect |

### Pull to refresh

- **Android** — `PullToRefreshBox` on the notes grid (and on the *empty* list, which is exactly when
  you reach for it) and inside the editor. `NotesViewModel.refresh()` / `NoteEditorViewModel.refresh(id)`
  run a real cycle and put the failure reason in the snackbar.
- **iOS** — `.refreshable` on both screens, awaiting `SyncController.syncNowAsync()` so the spinner
  stops when the work is actually done. Failures show in an alert.

**This is also the diagnostic.** If sync still does not cross devices, pull down on the notes list:
the screen now tells you *why* — "Your session has expired", "No internet", "The request was
rejected" — instead of failing silently. That one line is what to send me.

### What to test

1. Note created on A appears on B within a couple of seconds of the last keystroke. Both directions.
2. Edit on A → B. Delete on A → gone on B.
3. Airplane mode on A, create two notes, airplane mode off, **do not touch the app** → both land on B.
4. Pull down on the notes list and inside a note, on both platforms.
5. Kill and reopen the Android app → it syncs on resume (it never used to).
