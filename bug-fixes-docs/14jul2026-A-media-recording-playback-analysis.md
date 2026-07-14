# 14-Jul-2026 · Android · Media Recording & Playback — Flow Trace, Change Log, Root-Cause Analysis

Purpose: trace exactly how media recording and playback work today (MVI), list every change made
in the recent sessions with file + line + effect, and pinpoint the two open bugs with proposed
fixes. **No code is changed by this document — it is for us to agree on the plan first.**

Open issues to solve:
- **I-1**: A recorded video is not added to the note list after Stop → Back.
- **I-2**: After playing one media, selecting another media does not switch (was "plays same file";
  after later edits became "nothing plays / white screen").

---

## 1. MVI architecture as implemented here

The codebase follows an MVI-ish pattern per feature:

- **Intent / Action**: sealed interfaces of events.
  - Media capture/edit: `MediaProcessingEvent` — `ImageDataViewModel.kt:22-44`.
  - Media playback (UI → VM): `MediaPlayingUIEvent` — `PlayerViewModel.kt:24-37`.
  - Player service events (VM → player): `MediaPlayingEvent` — `MediaServiceListener.kt:19-31`.
  - Player domain state (player → VM): `PlayerState` — `PlayerViewModel.kt:39-47`.
- **Model / State** (immutable, exposed as `StateFlow`):
  - `NoteContentUiState` (note + `contents`) held in `NoteEditorViewModel` (`_noteContentUiState`).
  - `AudioUiState` / `PlayerUiState` in `PlayMediaViewModel` (`_playerUiState`) — `PlayerViewModel.kt:49-65`.
  - `MediaFileStateHandler` in `ImageDataViewModel` (`_mediaFileState`) — `ImageDataViewModel.kt:46-53`.
- **View**: Compose screens collect state with `collectAsStateWithLifecycle()` and emit intents via
  `viewModel.onXxx(...)`.

Adherence notes (where the code deviates from clean MVI — relevant to the bugs):

1. `ImageDataViewModel` exposes a **second** side-channel of state, `_paths`
   (`ImageDataViewModel.kt:59-62`), that the `NoteEditor` View reads and then feeds back into
   `NoteEditorViewModel.getMediaData(...)`. This is a **View-mediated hand-off between two
   ViewModels**, not a pure intent → single-VM → state loop. This is the fragile seam behind **I-1**.
2. Playback keeps **two parallel ordered lists** that must stay index-aligned:
   `NoteContentRepository._selectedNoteMediaContent` (`NoteContentRepository.kt:13-17`) and the
   ExoPlayer's own playlist built from it (`MediaServiceListener.addMediaItemList`, `:48-53`).
   Selection converts id → index via `getIndexOfMedia` (`:18-20`) and seeks the player by that index
   (`:69-84`). When those two lists drift, selection breaks. This is the fragile seam behind **I-2**.

---

## 2. Flow trace — VIDEO RECORDING (id → note list)

1. **NoteEditor**, user taps camera FAB → `preparePermissionDialog(IMAGE)`
   (`NotesEditorView.kt:247-249`). After grant → `navigateTo(CameraData(noteId))`
   (`NotesEditorView.kt:138-142`).
2. Nav graph builds `CameraStreamingScreen` with the **graph-shared** `ImageDataViewModel`
   (`HomeNavGraph.kt:83-90`, shared via `sharedViewModel`, `NavBackstackEntry+ext.kt:12-18`).
   So the same `ImageDataViewModel` instance is used by Camera **and** NoteEditor.
3. Tap Record → `recordingVideo(...)` starts capture (`CameraPreview.kt:105-112` → `CameraInAction.kt:25-48`),
   `startOrStopRecording(true)` (`CameraPreview.kt:112`).
4. Tap Stop → `recordingVideo` sees `recording != null` → `clearRecording()` stops it
   (`CameraInAction.kt:26-29`, `ImageDataViewModel.kt:147-151`).
5. Camera fires `VideoRecordEvent.Finalize` (async, main executor) → on success
   `imageDataViewModel.saveRecordedVideo(outputFile)` (`CameraInAction.kt:42`) →
   `_paths.value += (path, VIDEO)` (`ImageDataViewModel.kt:157-159`).
6. Back press → `onBackPress()` → `upPress()` returns to NoteEditor (`CameraPreview.kt:70-72`).
7. **NoteEditor** resumes. `capturedPaths = imageDataViewModel.paths.collectAsStateWithLifecycle()`
   (`NotesEditorView.kt:191`) → becomes `[(path, VIDEO)]`.
8. `LaunchedEffect(capturedPaths)` runs → `getMediaData(capturedPaths)` then `clearPaths()`
   (`NotesEditorView.kt:192-197`).
9. `getMediaData` builds `MediaContent`, appends to `contents`, updates state
   (`NoteEditorViewModel.kt:310-345`). LazyColumn renders `state.value.contents`
   (`NotesEditorView.kt:254-256`).

Image capture is the same except step 5-6 route through `Route.ImagePreview` →
`saveImage` (`ImageDataViewModel.kt:138-143`) → `popBackInclusive`. **Image works; video does not** —
so the divergence is in *when/whether* step 8's `getMediaData` sees a non-null note and a live list.

---

## 3. Flow trace — MEDIA PLAYBACK (MVI, audio/video)

1. Card composes; `PlayMediaViewModel` obtained via `viewModel()` (activity/nav scoped, shared by all
   cards) — e.g. `AudioPlayerView.kt:60`, `VideoCard.kt:74`.
2. `PlayMediaViewModel` init #1 collects `NoteContentRepository.selectedNoteMediaContent`
   (`PlayerViewModel.kt:73-91`): builds the ExoPlayer playlist (`addMediaItemList`, `MediaServiceListener.kt:48-53`)
   **and** the `mediaStates` map — both from the **same** emitted list, so they start aligned.
3. `_selectedNoteMediaContent` is filled by:
   - `addAllNoteContent(note)` on note READ — sorted by position (`NoteContentRepository.kt:30-34`,
     called from `NoteEditorViewModel.onSuccess READ`, `:86`).
   - `updateNoteContent(mediaContent)` for playing media edits (`NoteEditorViewModel.updateContent`, `:249-250`).
4. Tap play (audio): `onPlay` → `SelectedMediaChange(id)` (`AudioPlayerView.kt:71-74`) →
   `onPlayingIntent` (`PlayerViewModel.kt:153-178`) → `selectionAudioId(id)` (`:180-188`):
   `indexOf = getIndexOfMedia(id)` then `onPlayerEvents(SelectedAudioChange(id), indexOf)`.
5. `MediaServiceListener.SelectedAudioChange` (`:69-84`): if `selectedAudioIndex ==
   currentMediaItemIndex` → `playOrPause`; else `seekToDefaultPosition(selectedAudioIndex)` + play.
6. Player callbacks (`onPlaybackStateChanged`/`onIsPlayingChanged`, `:96-129`) push `PlayerState`
   back; VM init #2 (`:93-128`) maps them into `mediaStates` and drives the card UI.

**The invariant that must hold**: the index from `getIndexOfMedia` (based on
`_selectedNoteMediaContent`) must equal the track's index inside the ExoPlayer playlist. If the two
lists differ in content or order, `seekToDefaultPosition` targets the wrong/nonexistent track →
"plays the same file" or (with a strict guard) "nothing plays".

---

## 4. Change log — everything I touched, with effect

Legend: ✅ keep (safe/correct) · ⚠️ suspect (may cause a bug) · ↩️ already reverted.

| # | File:line (approx) | Change | Effect / risk |
|---|---|---|---|
| C1 | `FileOps.kt:60-165` | Added `saveMediaToGallery`, `writeMediaToUri`, `mimeTypeFor`, `suggestedFileName` | ✅ Save-to-device feature; no playback impact |
| C2 | `AndroidManifest.xml:26-31` | Added legacy `WRITE_EXTERNAL_STORAGE` (maxSdk 28) | ✅ Save-to-device only |
| C3 | `NotesEditorView.kt:447-505` | `MediaSaveOverlay` (save icon + SAF picker, IO) on image/video cards | ✅ UI overlay; wraps card in a `Box` |
| C4 | `NotesEditorView.kt:407-431` | AUDIO branch: SAF picker + `onSave` wired to `AudioPlayerUIState` | ✅ Save-to-device only |
| C5 | `AudioPlayerView.kt:56-80` | Forward `onSave` (was empty lambda) | ✅ Fixes dead save button |
| C6 | `NotesEditorView.kt:186-197` | Moved `getMediaData()/clearPaths()` from composition body into `LaunchedEffect(capturedPaths)` | ⚠️ **Prime suspect for I-1** — timing of the two-VM hand-off changed |
| C7 | `NoteEditorViewModel.kt:310-345` | Rewrote `getMediaData` to be "pure" (build items, `addAll`, single copy) | ⚠️ Reads `_noteContentUiState.value` once, mutates that captured `contents` list — could target a stale list under a concurrent state swap |
| C8 | `String+ext.kt:6-24` | `toBitmap()` now downsamples + | ✅ Image decode perf; images only |
| C9 | `ImageDataViewModel.kt:109-119, 138-143` | Decode + bitmap save moved to `Dispatchers.IO` | ✅ Perf; image path only |
| C10 | `PlayerViewModel.kt` (Playing/Ready/timeline/UpdateProgress/seekTo) | Replaced `mediaStates[id]!!` with null-safe skips | ✅ Prevents recorded-video NPE crash; does not change selection |
| C11 | `NoteEditorViewModel.getMediaData` | (earlier) registered captured media into `NoteContentRepository` | ↩️ **Reverted** — this caused list drift → I-2 "plays same file" |
| C12 | `MediaServiceListener.SelectedAudioChange` | (earlier) resolve index by mediaId + guard | ↩️ **Reverted** to original — its guard could no-op → "nothing plays" |

Current state of the two fragile seams:
- Playback selection (`MediaServiceListener.kt:69-84`) and repository (`NoteContentRepository.kt`) are
  **back to the original committed code** (C11, C12 reverted). Only C10 null-safety remains.
- Recording hand-off still uses the `LaunchedEffect` form (C6) and the rewritten `getMediaData` (C7).

---

## 5. Root-cause analysis & proposed fixes

### I-1 · Recorded video not added to the note list

Most likely cause: the **two-VM hand-off timing** changed by C6/C7. In the original code
`getMediaData()` ran in the composition body every recomposition, so it retried until it observed a
non-null note and a live list. As a `LaunchedEffect(capturedPaths)` it runs once per list change; if
that single run reads `_noteContentUiState.value` while the note is momentarily null, or mutates a
`contents` list instance that is then replaced, the video is dropped **and** `clearPaths()` still
wipes the path — so it never re-appears. Image capture survives because its path is written on a
different screen (ImagePreview) and the return path recomposes NoteEditor with a stable note.

Proposed fix (to agree): keep the crash-safe LaunchedEffect but make the hand-off robust:
- Guard `clearPaths()` so it only runs **after** `getMediaData` actually consumed the items
  (i.e., only clear the exact paths that were added; if note was null, do **not** clear — retry next
  emission). `NoteEditorViewModel.getMediaData` should return whether it consumed, or accept the
  list and clear internally.
- In `getMediaData` (`NoteEditorViewModel.kt:310-345`) do the append inside a single
  `_noteContentUiState.update {}` using `it` (the freshest state), not a pre-captured
  `_noteContentUiState.value`, so it can never target a stale list.

### I-2 · Selecting another media doesn't switch

Cause: index drift between `_selectedNoteMediaContent` and the ExoPlayer playlist
(`MediaServiceListener.kt:48-53` vs `:69-84`, via `getIndexOfMedia` `:18-20`). My C11 registration
made this worse; it's reverted, so behaviour is back to the original. The original design is still
fragile because it relies on two lists staying perfectly index-aligned.

Proposed fix (to agree): make selection index-independent — resolve the target track from the
**ExoPlayer's own playlist by `mediaId`** inside `SelectedAudioChange` (`MediaServiceListener.kt:69-84`),
with a guard that does nothing when the id isn't present (never seek to `-1`). This is what I tried in
C12 but reverted because it was bundled with the C11 drift; applied **alone** (with C11 staying
reverted) it makes selection correct regardless of the parallel list.

---

## 6. Proposed plan (needs your OK before I touch code)

1. **I-1**: fix the hand-off in `getMediaData` + conditional `clearPaths` (Section 5, I-1). Verify:
   record video → Back → video appears; capture 3 images rapidly → all appear, no crash.
2. **I-2**: mediaId-based selection in `MediaServiceListener.SelectedAudioChange` only (Section 5, I-2),
   with C11 staying reverted. Verify: play A, select B → B plays; select A → A plays.
3. Leave C10 null-safety in place (prevents the recorded-video NPE).

Open question for you: do you want me to (a) apply fixes 1 & 2 above, or (b) first fully revert C6/C7
back to the original composition-body `getMediaData` (restores pre-change video behaviour, but
re-introduces the multi-image crash) and then reapply only the multi-image fix in a safer way?
