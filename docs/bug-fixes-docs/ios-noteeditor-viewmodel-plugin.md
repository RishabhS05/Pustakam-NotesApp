# iOS Clean Native Flow — NoteEditor Refactor (Editor slice)

Series: bridge plan → NotesBridge implementation → NotesList plugin (rev 2) → **this doc**.
**Documentation only — no project files touched yet.**
Pattern applied "accordingly" from the list slice: init-injected adapter, single `@Published` state struct, bridge-only data access, UI structure unchanged (mechanical binding edits + two genuine bug fixes that require View lines).

---

## 1. Current Problems (verified against the real files)

### NoteEditorViewModel.swift

| # | Problem | Effect |
|---|---------|--------|
| E1 | Declared `class NoteEditorViewModel: ObservableObject` but has `override init() { super.init() }` and uses `apiHandler` / `noteRepositary` / `noteContentRepository` (which only exist on `BaseViewModel`/`IBaseHandler`). **As written this cannot compile** without inheriting `BaseViewModel` — declaration and body disagree | File is in a broken/ambiguous state; refactor resolves it |
| E2 | `var note: Note?` is **not `@Published`** and set from a `Task` | `OverlayEditorButtons(showDelete: vm.note != nil)` never updates when the async empty note arrives; UI reads stale nil |
| E3 | New-note flow is async (`setNote(nil)` → Task → `self.note = ...`) with **no readiness guard**; `addNewText()`, `getCapturedData()` force-unwrap `note!.id` | Tap "+text"/camera before the empty note arrives → **crash** |
| E4 | Dual source of truth: `noteContents` (published) AND `note.contents` maintained by hand in `updateContent`/`addContent`/`getCapturedData` | Drift bugs; three copies of the same append logic |
| E5 | `createorUpdateNoteCall()` is fire-and-forget `Task` with no error surface and `note!` unwrap | Silent save failures; crash window per E3 |
| E6 | Repo-direct calls (`noteRepositary.getANote/insertOrUpdateNote/deleteNote`, `noteContentRepository.*`) | The SOLID violation this series removes |

### NoteEditorView.swift

| # | Problem | Effect |
|---|---------|--------|
| V1 | `@ObservedObject private var noteEditorViewModel = NoteEditorViewModel()` — inline-initialized **ObservedObject** | VM is **recreated on every view re-render**; contents/state reset mid-editing. Must be `@StateObject` |
| V2 | `@State var title` is edited by `NoteTextEditor` but **never written back to `note.title`** before save | **Title edits are silently lost** on save |
| V3 | `.onDisappear { saveNote() }` runs unconditionally — including after a successful delete (`callDelete` → `dismiss()` → onDisappear) | **Deleted note is re-inserted into the DB on exit** (zombie note) |
| V4 | Trash toolbar button action is commented out (`// onDelete()`) | Delete unreachable from toolbar despite full alert machinery existing |
| V5 | View drives delete via `Task` + `isLoading` + `as! NetworkError` cast | Same casting fragility the list slice removed |

Known shared-code issues that surface here but belong to other pieces (not fixed in this doc): `createText` ignores its `text` param and media `positionedAt: 0` for all media (Piece 5); `insertOrUpdateNoteFromDb` returning before contents are written (Piece 2/D).

---

## 2. Target flow

```
NoteEditorView (structure unchanged)
  └─ NoteEditorViewModel : ObservableObject
       │   @Published var state: NoteEditorUIState     (single surface, incl. title)
       ├─ adapter: NotesBridgeAdapter                  (init-injected — readNote / upsert / delete)
       └─ contentBridge: NoteContentBridge             (init-injected — selected-media state)
             └─ NoteContent use cases (commonMain)     ◀ use-cases-only rule, same as NotesBridge
                   └─ NoteContentRepository (Kotlin)
```

## 3. NoteContent — use-case-backed bridge (REVISED: no repository in the bridge)

> **Rev 3 change.** The first version of this section injected `NoteContentRepository` directly into `NoteContentBridge` — a violation of the use-cases-only rule (and it's what currently sits in `iosMain/bridge/NoteContentBridge.kt`). This revision routes everything through use cases, same as `NotesBridge`. Complete change list for the NoteContent code:

| # | File | Action |
|---|------|--------|
| 3.1 | `commonMain/.../usecases/NoteContentUseCase.kt` | **NEW** — content use cases + base |
| 3.2 | `commonMain/.../koinDI/Koin.kt` | **EDIT** — register 3 factories in the existing `useCases` module |
| 3.3 | `iosMain/.../bridge/NoteContentBridge.kt` | **REWRITE** — inject use cases, drop repository import |
| — | `NoteContentRepository.kt` | **UNCHANGED** (its own bugs — `clear()` no-op etc. — stay in refactor Piece 5) |
| — | Swift (`NotesBridgeAdapter` / editor VM) | **UNCHANGED** — bridge's public API is identical |

### 3.1 NEW — `commonMain/.../domain/repositories/usecases/NoteContentUseCase.kt`

`NoteContentRepository` is not a `BaseRepository`, so it gets its own small base (mirror of `NoteBaseUseCase`'s pattern, including the state accessor like `notes`/`tags`). These are synchronous state operations — no `Flow<Result<...>>` wrapping needed, so no `getBaseApiCall`:

```kotlin
package com.app.pustakam.domain.repositories.usecases

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class NoteContentBaseUseCase : KoinComponent {
    protected val noteContentRepository: NoteContentRepository by inject()
    /** Selected-note media stream — mirror of NoteBaseUseCase.kt.notes / .tags */
    val selectedMediaContent get() = noteContentRepository.selectedNoteMediaContent
}

/** Load a note's media contents into the selected-note state (editor open). */
class SetSelectedNoteContentUseCase : NoteContentBaseUseCase() {
    operator fun invoke(note: Note) = noteContentRepository.addAllNoteContent(note)
}

/** Add/replace one media item in the selected-note state (capture, recording). */
class UpdateSelectedMediaContentUseCase : NoteContentBaseUseCase() {
    operator fun invoke(content: NoteContentModel.MediaContent) =
        noteContentRepository.updateNoteContent(content)
}

/** Index lookup used by media players (Android PlayerViewModel parity). */
class GetSelectedMediaIndexUseCase : NoteContentBaseUseCase() {
    operator fun invoke(id: String): Int = noteContentRepository.getIndexOfMedia(id)
}
```

(Android's `NoteEditorViewModel`/`PlayerViewModel` also call this repository directly today — they can migrate to these same use cases later; additive, nothing breaks now.)

### 3.2 EDIT — `commonMain/.../koinDI/Koin.kt` (existing `useCases` module, 3 lines)

```kotlin
val useCases: Module = module {
    // ... existing factories ...
    factory<SetSelectedNoteContentUseCase> { SetSelectedNoteContentUseCase() }
    factory<UpdateSelectedMediaContentUseCase> { UpdateSelectedMediaContentUseCase() }
    factory<GetSelectedMediaIndexUseCase> { GetSelectedMediaIndexUseCase() }
}
```

### 3.3 REWRITE — `iosMain/.../bridge/NoteContentBridge.kt` (full file)

Public API unchanged (`setSelectedNote` / `updateMediaContent` / `observeSelectedMedia` / `dispose`) → zero Swift-side impact. The only structural change: **no `NoteContentRepository` import anywhere in `iosMain`**.

```kotlin
package com.app.pustakam.bridge

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.usecases.SetSelectedNoteContentUseCase
import com.app.pustakam.domain.repositories.usecases.UpdateSelectedMediaContentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Selected-note media state for iOS — use-case-backed (no repository access). */
class NoteContentBridge : KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val setSelectedNoteUseCase: SetSelectedNoteContentUseCase by inject()
    private val updateMediaUseCase: UpdateSelectedMediaContentUseCase by inject()

    fun setSelectedNote(note: Note) = setSelectedNoteUseCase(note)

    fun updateMediaContent(content: NoteContentModel.MediaContent) =
        updateMediaUseCase(content)

    /** Live media list of the selected note (players/visualizers). */
    fun observeSelectedMedia(onChange: (List<NoteContentModel.MediaContent>) -> Unit): Closeable =
        setSelectedNoteUseCase.selectedMediaContent.watch(scope) { onChange(it) }

    fun dispose() = scope.cancel()
}
```

### 3.4 Review notes on the bridge code currently applied in `iosMain/bridge/` (found while auditing)

Your applied files deviate from the docs in three spots worth fixing while you're in there:

1. **`BridgeError.kt` — error codes collapsed:** `is NetworkError -> BridgeError("SERVER_ERROR", ...)` hardcodes every network error's code as `SERVER_ERROR`. `NOT_FOUND` (empty tags, missing note — behaviors the VMs rely on) becomes indistinguishable. Doc version: `code = name` (the enum entry name).
2. **Typo:** `toBrigeError` → `toBridgeError` (internal, compiles, but it'll spread through call sites).
3. **Package/folder mismatch:** `NotesBridge.kt` declares `package com.app.pustakam.bridge.notes` but sits in `bridge/` (not `bridge/notes/`). Kotlin tolerates it; the IDE will warn. Either move the file into `bridge/notes/` or change the package to `com.app.pustakam.bridge`.

## 4. Swift — `NotesBridgeAdapter.swift` additions (editor slice)

```swift
// MARK: - Editor slice

func readNote(noteId: String?, onState: @escaping (UiState<Note>) -> Void) {
    closeables.append(bridge.readNote(
        noteId: noteId,
        onLoading: { onState(.loading) },
        onSuccess: { onState(.success($0)) },
        onError:   { onState(.failure($0)) }
    ))
}

func createOrUpdateNote(note: Note, onState: @escaping (UiState<Note>) -> Void) {
    closeables.append(bridge.createOrUpdateNote(
        note: note,
        onLoading: { onState(.loading) },
        onSuccess: { onState(.success($0)) },
        onError:   { onState(.failure($0)) }
    ))
}

func deleteNote(noteId: String?, onState: @escaping (UiState<KotlinBoolean>) -> Void) {
    closeables.append(bridge.deleteNote(
        noteId: noteId,
        onLoading: { onState(.loading) },
        onSuccess: { onState(.success($0)) },
        onError:   { onState(.failure($0)) }
    ))
}
```

## 5. REWRITE — `NoteEditorViewModel.swift` (full file)

```swift
import shared
import Combine
import SwiftUI

/// Single UI-state surface for the editor (mirror of the list slice pattern).
struct NoteEditorUIState {
    var note: Note? = nil                       // fixes E2: now inside @Published state
    var title: String = ""                      // fixes V2: title owned by VM, saved reliably
    var noteContents: [NoteContentModel] = []   // SINGLE source of truth (fixes E4)
    var isLoading = false
    var isDeleted = false                       // fixes V3: save() refuses after delete
    var errorMessage: String? = nil

    /// E3 guard: content actions allowed only once the note exists.
    var isNoteReady: Bool { note != nil }
}

class NoteEditorViewModel: ObservableObject {

    @Published var state = NoteEditorUIState()

    private let adapter: NotesBridgeAdapter
    private let contentBridge: NoteContentBridge

    /// DI per series convention: defaults keep call sites/tests simple. (fixes E1, E6)
    init(note: Note? = nil,
         adapter: NotesBridgeAdapter = NotesBridgeAdapter(),
         contentBridge: NoteContentBridge = NoteContentBridge()) {
        self.adapter = adapter
        self.contentBridge = contentBridge
        load(note: note)
    }

    deinit { contentBridge.dispose() }          // adapter cleans itself up

    // MARK: - Load

    private func load(note: Note?) {
        if let note {
            apply(note: note)
        } else {
            // New note: bridge returns a fresh empty Note (pre-generated id/timestamps).
            adapter.readNote(noteId: nil) { [weak self] result in
                guard let self else { return }
                switch result {
                case .loading:            self.state.isLoading = true
                case .success(let note):  self.state.isLoading = false
                                          if let note { self.apply(note: note) }
                case .failure(let error): self.state.isLoading = false
                                          self.state.errorMessage = error.message
                case .idle: break
                }
            }
        }
    }

    private func apply(note: Note) {
        state.note = note
        state.title = note.title ?? ""
        state.noteContents = note.contents as? [NoteContentModel] ?? []
        contentBridge.setSelectedNote(note: note)
    }

    // MARK: - Content editing (single list; note.contents materialized only at save)

    func addContent(content: NoteContentModel) {
        guard state.isNoteReady else { return }                 // fixes E3 crash window
        state.noteContents.append(content)
        if content.isPlayingMedia(), let media = content as? NoteContentModel.MediaContent {
            contentBridge.updateMediaContent(content: media)    // safe cast (was as!)
        }
    }

    func updateContent(content: NoteContentModel) {
        if let index = state.noteContents.firstIndex(where: { $0.id == content.id }) {
            state.noteContents[index] = content
        } else {
            addContent(content: content)
        }
        // NOTE: no more parallel note.contents bookkeeping (E4) — save() materializes.
    }

    func addNewText() {
        guard let noteId = state.note?.id else { return }       // fixes E3 (was note!.id)
        let text = NoteContentObjectHelper.shared.createText(
            noteId: noteId,
            positionedAt: Int64(state.noteContents.count),
            text: "")
        addContent(content: text)
    }

    func getCapturedData(media: CapturedMedia?) {
        guard let noteId = state.note?.id else { return }       // fixes E3 (was note!.id)
        let timeStamp = DateTimeUtilsKt.getCurrentTimestamp()

        switch media {
        case .image(let image):
            let type = ContentType.image
            guard let data = image.pngData() else { return }
            let path = saveImageFile(data: data,
                                     in: "\(type.name)/\(noteId)",
                                     to: "\(timeStamp)\(type.getExt())")
            saveMedia(type: type, localPath: path, noteId: noteId, timestamp: "\(timeStamp)")

        case .video(let path):
            let type = ContentType.video
            guard let saved = copyFile(to: "\(type.name)/\(noteId)",
                                       fileName: "\(timeStamp)\(type.getExt())",
                                       from: path) else { return }
            saveMedia(type: type, localPath: saved.path, noteId: noteId, timestamp: "\(timeStamp)")

        case .audio(let path):
            let type = ContentType.audio
            guard let saved = copyFile(to: "\(type.name)/\(noteId)",
                                       fileName: "\(timeStamp)\(type.getExt())",
                                       from: path) else { return }
            saveMedia(type: type, localPath: saved.path, noteId: noteId, timestamp: "\(timeStamp)")

        case .none:
            break
        }
    }

    /// ONE construction/append path for captured media (was triplicated + note.contents).
    /// Private method (promoted from nested func): reusable by future flows, e.g. file import.
    /// noteId/timestamp are explicit params now — callers must keep them consistent
    /// with the saved file's folder/name (the nested version guaranteed this by capture).
    private func saveMedia(type: ContentType, localPath: String,
                           noteId: String, timestamp: String) {
        let media = NoteContentObjectHelper.shared.createMedia(
            contentType: type,
            noteId: noteId,
            positionedAt: 0,            // behavior kept as today (position fix = Piece 5)
            localPath: localPath,
            url: "",
            duration: 0,
            timestamp: timestamp)
        addContent(content: media)
    }

    // MARK: - Save / Delete

    /// Replaces createorUpdateNoteCall(). Guarded, materializes state → Note, surfaces errors.
    func saveNote() {
        guard !state.isDeleted else { return }                  // fixes V3 (zombie note)
        guard let note = state.note else { return }             // fixes E5 (was note!)

        note.title = state.title                                // fixes V2 (title saved)
        note.contents = state.noteContents                      // E4: materialize once

        adapter.createOrUpdateNote(note: note) { [weak self] result in
            if case .failure(let error) = result {
                self?.state.errorMessage = error.message
                print("saveNote failed [\(error.code)] \(error.message)")
            }
        }
    }

    /// Replaces deleteNoteCall(); View no longer runs Task/casts. (fixes V5)
    func deleteNote(onDeleted: @escaping () -> Void) {
        guard let noteId = state.note?.id else { return }
        adapter.deleteNote(noteId: noteId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .loading:
                self.state.isLoading = true
            case .success:
                self.state.isLoading = false
                self.state.isDeleted = true                     // blocks onDisappear save
                onDeleted()                                     // View dismisses
            case .failure(let error):
                self.state.isLoading = false
                self.state.errorMessage = error.message
            case .idle: break
            }
        }
    }

    func shareNote() {}     // stub kept (nothing deleted)
}
```

> ⚠️ `NoteContentObjectHelper` is a Kotlin `object` — from Swift it's `NoteContentObjectHelper.shared.createText(...)`. The current code writes `NoteContentObjectHelper()` which shouldn't compile against a Kotlin object export — consistent with finding E1 that this file is in a non-compiling state. Verify against the generated header on first build.

## 6. `NoteEditorView.swift` — edits (mechanical + 3 real fixes)

1. **V1 fix (required, one line):**
   `@ObservedObject private var noteEditorViewModel = NoteEditorViewModel()`
   → `@StateObject private var noteEditorViewModel: NoteEditorViewModel`
   and in `init(note:)`: `_noteEditorViewModel = StateObject(wrappedValue: NoteEditorViewModel(note: note))` — replaces the `setNote(note:)` call; `_title` seeding is no longer needed (title lives in VM state).
2. **Binding-path edits** (same rule as list slice — properties gain `.state`):
   `$title` → `$noteEditorViewModel.state.title` · `noteEditorViewModel.noteContents` → `.state.noteContents` · `noteEditorViewModel.note != nil` → `.state.note != nil` (E2 now actually updates) · `isLoading` (local `@State`) → `.state.isLoading` (one spinner, VM-driven).
3. **V3/V5 fix — `callDelete` collapses to:**

```swift
private func callDelete(noteId: String?) {
    guard noteId.isNotNilOrEmpty() else { return }
    noteEditorViewModel.deleteNote { dismiss() }
}
```

   (delete errors surface via `state.errorMessage` → existing alert plumbing; no Task, no `as! NetworkError`).
4. **V4 (needs your sign-off — it enables a currently dead button):** trash button action `// onDelete()` → `setAlert(message: "Delete this note?", title: "Delete", alertType: .DELETE)` — the confirm alert already exists and calls `callDelete`.
5. `.onDisappear { saveNote() }` stays **unchanged** — the zombie-note bug is fixed inside `saveNote()` via `isDeleted`.

Everything else (layout, `renderWidget`, alerts, permissions, recorder) untouched.

## 7. Nothing deleted

`NoteContentRepository` Kotlin code, `BaseViewModel`, `apiHandler`, stubs (`shareNote`, `removeContent`, `addContentData` — the latter two simply don't reappear in the rewrite; flag if you want them kept as stubs), `NoteRepositoryHelper` — all left in place; removals happen in the cleanup piece with your approval.

## 8. Verification scenarios (editor slice)

1. New note → editor opens → empty note arrives async → FAB/delete button state updates (E2); typing title + save persists it (V2).
2. **Race:** open new note and instantly tap +text/camera → no crash, action ignored until ready (E3). If UX prefers, buttons can disable on `!state.isNoteReady` — optional View tweak.
3. Edit existing → change title only → back (onDisappear save) → title persisted (V2).
4. Add text/image/video/audio → save → reopen → contents present, positions consistent (single append path, E4).
5. **Delete → confirm → dismiss → note stays deleted** (V3 zombie fixed); list screen updates via `observeNotes` from the list slice.
6. Save failure (forced) → `errorMessage` set → alert shows; no silent loss (E5).
7. Re-render storm (rotate, sheet open/close) → VM survives, contents intact (V1 `@StateObject`).
8. Media playback widgets still receive updates via `NoteContentBridge.observeSelectedMedia` → `NoteRepositoryHelper.noteContentMediaList` becomes unused for this screen (deprecation candidate, later).

## 9. Series status

| Slice | Status |
|-------|--------|
| NotesBridge (Kotlin) | documented, not applied |
| Notes list VM plugin | documented (rev 2), not applied |
| **NoteEditor VM plugin** | **this doc — awaiting review** |
| Login/Signup, media flows | pending |
