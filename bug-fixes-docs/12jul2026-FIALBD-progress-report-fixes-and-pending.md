# Progress Report — Fixes Applied & Pending Work
**Date:** 12-Jul-2026 · **Platforms:** F/I/A/LBD (shared KMM, iOS, Android, local DB) · **Phase:** P0 (offline, Notes CRUD stabilization)

---

## 1. Status Summary

The iOS clean-architecture migration (use-cases-only via hand-written bridge, no third-party lib) is **functionally complete for Notes CRUD + Auth**: create/edit/delete notes with text content now works end-to-end on iOS with real-time list updates. ~35 distinct bugs were found and fixed across all layers during the migration. Remaining work is listed in §4.

```
DONE      Bridge layer (Notes/NoteContent/Auth) · model refactor · repo/DAO fixes ·
          iOS list+editor+login+signup migration · real-time pipeline · text editor sync
PENDING   media flows migration · tag edit/delete UI · DB migrations (.sqm) ·
          pagination · legacy-path cleanup · scenario matrix run · online phase
```

---

## 2. FIXED — by layer (each carries a 🔧 review comment in code)

### 2.1 Shared model (`Note.kt`, factory, serialization)

| ID | Fix |
|----|-----|
| M1/M2 | Immutable fields; id-only `equals` + broken `hashCode` removed → StateFlow emissions reliable |
| M3 | `contents` non-null; `withTitle/withContents/withTitleAndContents` Swift-friendly helpers — **every edit auto-stamps note `updatedAt`** |
| C1/S1 | `MediaContent`: real metadata (mimeType, sizeBytes, width/height, thumbnailPath); title default "" (was "Audio"); **media titles now actually persisted** (DB column added — was silently reading the NOTE's title from the join); PDF + GIF round-trip |
| C2 | `TextContent.text` → `val`; `withText()` stamps **content** `updatedAt` |
| C4 | `position: Double` (fractional ordering; REAL column); iOS media no longer hardcodes position 0 |
| C6 | id/timestamps are required params — generated ONLY in `NoteContentObjectHelper` (id logic untouched: timestamp+UUID); deserialization can never regenerate ids |
| C8 | `Location` had duplicate `@SerialName("LINK")` (runtime crash) → LOCATION; `type` property → `contentType` on wire; `classDiscriminator="type"` + `coerceInputValues` configured |
| C3 | attempted Long timestamps → **reverted by decision**: timestamps stay String |
| — | `createText` ignored its `text` param (fixed); rich-text `metadata` was NEVER persisted (`val = null` bug) and dropped by one mapper (both fixed) |

### 2.2 Repository / DAO (`NoteRepository`, `NotesDao`, `BaseRepository`)

| ID | Fix |
|----|-----|
| F3 | `notesState`/`tagState` → plain `asStateFlow()` (killed never-cancelled `stateIn` scopes); copy-first upsert + id-based delete (emission + CME safety); tags create/update/delete all push into `_tags` (live tag UI); 3 missing-`return` dead-code bugs (`deleteNoteByIdFromDb`, `deleteNoteContentFromDb`, `deleteTagOnDB` — which also ignored the DAO result) |
| F4 | `CoroutineScope(...).launch` **inside** `database.transaction` (2 sites) → synchronous writes; contents are now saved atomically before the function returns |
| AUTH | `userLogout()` collected a flow forever + discarded the copy (never logged out, caller hung) → `userPrefs.clear()`; `prefs` lateinit race crash → safe default |

### 2.3 iOS bridge (Kotlin `iosMain` + Swift adapters)

| ID | Fix |
|----|-----|
| F1 | `toBrigeError` typo; error `code` was hardcoded "SERVER_ERROR" for ALL network errors (NOT_FOUND indistinguishable) → `code = name`; `NotesBridge` package/folder mismatch |
| F5 | `NoteContentBridge` was injecting the repository directly → now use-case-backed (`NoteContentUseCase.kt` + 3 Koin factories); **zero repository imports left in iosMain** |
| NEW | `AuthBridge` + `AuthBridgeAdapter` (login/signup/logout/observeAuthState) |
| REALTIME | **writes survive screen death**: app-lifetime `writeScope` for all mutations (dispose() was cancelling in-flight saves started from `onDisappear`) + Swift adapters no longer retain/close write Closeables (deinit was cancelling them from the other side) |
| — | `AuthBridgeAdapter.swift` registered into the Xcode target (pbxproj edited directly) |

### 2.4 iOS screens

| Screen | Fixes |
|--------|-------|
| Notes list | rev-2 VM (single `NotesUIState`, injected adapter); **hasLoaded guard** — re-fetch on every onAppear raced the editor's save and wiped it from state (Android parity); **tags initial load** — nothing ever called getTags → chips empty after relaunch |
| Editor | migrated View to new VM API (**source of the 19 compile errors**); `@StateObject` (VM was recreated on re-render, wiping edits); title now saved (was `@State` never written back); zombie-note guard (delete → onDisappear-save re-inserted it); crash guards for the async new-note window; single source of truth for contents |
| **Text editor widget** | **the "invisible data" bug**: `NoteTextFieldWrapper` never invoked `onTextChange` AND `attText` never synced back to the `$text` binding → titles + text saved as ""; initial value also never seeded → even saved text rendered blank. Both directions fixed with equality-guarded syncs |
| Login/Signup | migrated to AuthBridge; `as! NetworkError` crash casts removed (crashed on any ErrorMessage failure); signup `dismiss()` ran before the API responded (errors never seen) → dismiss only on success; handlers `@State` (plain `let` in a View struct was recreated per render, cancelling in-flight auth) |

### 2.5 Android (ripples + latent bugs found on the way)

`updateContent` added a duplicate instead of replacing; `removeContent` removed from a **discarded copy** (contents never actually removed from the note); `updateNoteObject()` built the title copy and threw it away (**titles were never saved on Android either**); M3/C4 call-site updates; text edits via `withText` (stamps content updatedAt).

---

## 3. Verified working after fixes (manual)

Create note → type title/text → back → appears in list **instantly** · reopen → contents visible · relaunch → data persists · tag create updates chips live · login/signup error paths don't crash.

---

## 4. PENDING — bugs & work items (priority order)

### P0 — finish stabilization
1. **Run the full scenario matrix** (bridge plan §5 + editor doc §8) on simulator + device; Android regression pass (editor flows touched by M3/C4).
2. **Editor trash button is still dead** (`// onDelete()` commented) — alert machinery exists; needs your sign-off to wire (V4). - wired with the delete function
3. **Delete-content flow on iOS** — `deleteNoteContent` exists in bridge/adapter but no UI path calls it (old `removeContent()` stub was empty).
4. **Media flows still on legacy path**: `AudioPlayerViewModel` (BaseViewModel), `MediaManager` (NoteRepositoryHelper) — migrate to `NoteContentBridge.observeSelectedMedia`; then `BaseNetworkHandler/IBaseHandler`, `NoteRepositoryHelper`, `KoinHelper` repo getters can be deprecated (deletion needs your approval).
5. **Tag update/delete UI missing on iOS** (adapter methods ready).

### P1 — robustness
6. **`.sqm` migrations** — schema changes currently require app reinstall (fine in dev, blocker before release).
7. **`insertNotes` replace semantics** — wholesale state replacement is safe now (single load) but must become merge-by-`updatedAt` for the online phase.
8. **Pagination** — DAO computes offset but never applies it; list loads everything (fine offline, fix with 6/7).
9. `getNotesFromDb` has no try/catch — a future schema issue surfaces as silent empty list instead of an error.
10. Stale `iosMain/koinDI/Koin.kt` `initKoin()` (not called, missing modules) — trap for future devs; deprecate/remove with approval.
11. `getUrl()` per-platform base URL for device testing (device needs Mac's LAN IP; Android emulator 10.0.2.2).

### P2+ — next feature phases (per `future-scope/app-vision-feature-roadmap.md`)
Backend model fixes (broken `LocationContent`, `content`→`contents` drift, validation) before Online phase · sync engine via `SyncStatus` · model gaps G1–G6 (canvas frame, DRAWING type, soft delete, sync fields, multi-tag, Notebook entity) · then roadmap P1 features (drawing, export, share).

---

## 5. Build notes
- Kotlin changed since last framework build → **Clean Build Folder** in Xcode; no schema change since the last reinstall → no reinstall needed now, but any future `.sq` change = reinstall (until item 6).
- Android untested since M3/C4 ripples — build + smoke test needed.
