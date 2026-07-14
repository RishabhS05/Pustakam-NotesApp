# 14jul2026-I — Fix: deleting a content item deleted the whole note (+ delete UI for video/audio)

Platform: **I (iOS)** · Type: **Bugfix + Feature** · Screens: Note Editor

## The bug

Long-press on an image card (CardImageEditor) showed the actions bar; tapping delete raised the
confirmation alert — but confirming deleted the **entire note**, not the pressed image.

Two wiring faults in `NoteEditorView.swift`:

1. The image's `actionDelete` raised alert type `.DELETE` — the same case as the toolbar's
   note-level trash button — whose Confirm runs `callDelete()` → `deleteNote()`.
2. The `.DELETE_CONTENT` alert case existed, but its Confirm did nothing, and nothing in the view
   remembered *which* item was pressed. The Kotlin bridge method `NotesBridge.deleteNoteContent`
   existed but had no UI path calling it (known gap, listed in
   `12jul2026-FIALBD-progress-report-fixes-and-pending.md`, pending item 3).

Also: video had no delete UI at all, and audio's trash button called an `onDelete` that callers
could never pass in (custom `init` didn't accept it — the tap silently did nothing).

## The fix (flow)

```
Long-press card → actions bar → trash
  → askDeleteContent(contentId, kind)        // remembers the pressed item's id
  → DELETE_CONTENT alert → Confirm
  → NoteEditorViewModel.deleteContent(contentId:)
       1. media? → deleteFile(localPath)      // disk
       2. adapter.deleteNoteContent(...)      // DB (write call, survives screen death)
       3. state.noteContents.removeAll(id)    // UI; save() rebuilds from this list,
                                              // so the item can't resurrect on next save
```

Note-level delete (toolbar trash → `.DELETE` alert → `callDelete()`) is unchanged.
Mirrors Android `NoteEditorViewModel.removeContent()` (file + DB + state).

## Files changed

| File | Change |
|---|---|
| `iosApp/iosApp/bridge/notes/NotesBridgeAdapter.swift` | NEW `deleteNoteContent(contentId:onState:)` — wraps existing Kotlin bridge call; write call, not retained (same pattern as `deleteNote`) |
| `iosApp/iosApp/file/FileOps.swift` | NEW `deleteFile(filePath:)` — removes the media file from disk; safe no-op if missing |
| `iosApp/iosApp/view/screens/notes/noteEditor/NoteEditorViewModel.swift` | NEW `deleteContent(contentId:)` — file + DB + state removal (see flow above) |
| `iosApp/iosApp/view/screens/notes/noteEditor/NoteEditorView.swift` | NEW `@State deleteContentId`; NEW `askDeleteContent(contentId:kind:)`; image now uses `.DELETE_CONTENT` (was `.DELETE` — the bug); video/audio wired to same flow; `.DELETE_CONTENT` Confirm now performs the delete; `resetAlert()` clears the selection |
| `iosApp/iosApp/view/widgets/video/VideoCardView.swift` | NEW long-press actions bar (delete/edit, 2.5s auto-hide, bottom-fade gradient — identical pattern to CardImageEditor) + `actionDelete` init param |
| `iosApp/iosApp/view/widgets/audio/AudioPlayingView.swift` | `init` now accepts `onDelete`/`onEdit` (trash button already existed; it just could never receive a callback) |

**Untouched on purpose:** CardImageEditor UI (user-added actions-bar design kept as-is — only its
call-site wiring changed). Text content has no per-item delete UI (Android doesn't either).

## How to use

- Image / Video: long-press the card → actions bar appears → trash → Confirm.
- Audio: trash button on the player row → Confirm.
- New widget params (defaults keep old call sites compiling):
  - `VideoCardPlayer(content: media, actionDelete: { ... })`
  - `AudioPlayView(mediaContent: media, onDelete: { ... }, onEdit: { ... })`

## Verification notes

- Only call sites of the three widgets are in `NoteEditorView.swift` — no other screen affected.
- `isMediaFile()` / `localPath` confirmed exported from shared `Note.kt` (lines 84, 123).
- `save()` materializes from `state.noteContents`, so a deleted item cannot reappear via
  `onDisappear` auto-save.

## Follow-ups (not done, ask first)

- Per-item delete for text blocks (neither platform has it).
- `actionEdit` on image/video actions bar is still a stub on both widgets.
