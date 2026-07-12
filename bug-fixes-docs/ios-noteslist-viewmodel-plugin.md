# iOS Clean Native Flow — Plugging NotesBridge into NotesViewModel (Notes List slice)

Series: `ios-notes-usecase-bridge-plan.md` → `ios-notesbridge-full-implementation.md` → **this doc (rev 2)**.
**Documentation only — no project files touched yet.**

Rev 2 changes (per review):
- **DI:** adapter arrives via `init` injection (default arg). Composition-root `AppDI` in `iOSApp.swift` is specified for later — added when a second screen migrates ("if required root use it in root, if required only injection use it accordingly").
- **State:** the eight `@Published` vars are clubbed into a single `NotesUIState` struct behind one `@Published` — functionally identical under `ObservableObject` (any published change invalidates the whole view either way), and mirrors Android's existing `NotesUIState`.
- Honest consequence: `NotesView.swift` now needs **mechanical binding-path edits** (`notesHandler.x` → `notesHandler.state.x`). No structural/logic change, but no longer "zero UI edits".

---

## 0. Prerequisites (unchanged — none exist in code yet)

1. `bridge/Closeable.kt`, `BridgeError.kt`, `FlowBridge.kt`, `notes/NotesBridge.kt` in `iosMain` (ios-notesbridge-full-implementation.md §2–3).
2. `tags` accessor (2 lines) in `NoteBaseUseCase` (same doc §1).
3. Rebuilt `shared` framework.

---

## 1. The clean flow

```
BEFORE:
NotesView ─▶ NotesViewModel : BaseViewModel : IBaseHandler
               ├─ NoteRepositoryHelper (leaking observers)
               └─ Task + apiHandler + noteRepositary.* (repo direct + casts)

AFTER:
NotesView ─▶ NotesViewModel : ObservableObject          (no BaseViewModel/IBaseHandler)
               │   @Published var state: NotesUIState    (single state surface)
               └─ adapter: NotesBridgeAdapter            (INJECTED via init)
                     └─ NotesBridge ─▶ use cases ─▶ NoteRepository ─▶ DAO
```

### DI decision applied

- **Now (one consumer):** `init(adapter: NotesBridgeAdapter = NotesBridgeAdapter())`. Callers don't change (`NotesViewModel()` still compiles); tests/previews inject a fake.
- **Later (composition root), when NoteEditor/Login migrate:** central `AppDI` in `iOSApp.swift`, screens built in the existing `navigationDestination` switch:

```swift
// iOSApp.swift — added ONLY when a 2nd screen migrates
@Observable final class AppDI {
    func notesViewModel() -> NotesViewModel {
        NotesViewModel(adapter: NotesBridgeAdapter())
    }
}
// case .Notes: NotesView(notesHandler: di.notesViewModel())
```

- **Deliberately NOT used:** putting the *adapter* in `.environment()`. Environment objects get app lifetime; the adapter must die with its screen (its `deinit` cancels observers). App-lifetime adapter = the observer leak returns. Environment stays for app-lifetime objects only (`Router`, `ThemeManager` — as the codebase already does).

---

## 2. NEW — `iosApp/iosApp/bridge/UiState.swift` (unchanged from rev 1)

```swift
import Foundation
import shared

enum UiState<T> {
    case idle
    case loading
    case success(T?)
    case failure(BridgeError)
}
```

## 3. NEW — `iosApp/iosApp/bridge/notes/NotesBridgeAdapter.swift` (unchanged from rev 1)

```swift
import Foundation
import shared

final class NotesBridgeAdapter {

    private let bridge = NotesBridge()
    private var closeables: [Closeable] = []

    deinit {
        closeables.forEach { $0.close() }
        bridge.dispose()
    }

    func observeNotes(onChange: @escaping (Notes) -> Void) {
        closeables.append(bridge.observeNotes(onChange: onChange))
    }

    func observeTags(onChange: @escaping ([Tag]) -> Void) {
        closeables.append(bridge.observeTags(onChange: onChange))
    }

    func getNotes(page: Int, onState: @escaping (UiState<Notes>) -> Void) {
        closeables.append(bridge.getNotes(
            page: Int32(page),
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        ))
    }

    func createTag(tag: Tag, onState: @escaping (UiState<Tag>) -> Void) {
        closeables.append(bridge.createTag(
            tag: tag,
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        ))
    }
}
```

## 4. REWRITE — `NotesViewModel.swift` (full file, rev 2: injected adapter + single state)

```swift
import shared
import Combine
import SwiftUI

/// Single UI-state surface — mirror of Android's NotesUIState data class.
/// Value type: mutating any field fires the one @Published publisher.
struct NotesUIState {
    var notes: [Note] = []
    var tags: [Tag] = []
    var isLoading = false
    var page = 1
    var showSheet = false
    var showColorPalette = false
    var tagName = ""
    var color = Color.red
    var errorMessage: String? = nil
}

class NotesViewModel: ObservableObject {

    /// ONE publisher instead of eight. Functionally identical re-render behavior
    /// under ObservableObject (objectWillChange invalidates the whole view either way).
    @Published var state = NotesUIState()

    private let adapter: NotesBridgeAdapter

    /// DI: default arg keeps existing `NotesViewModel()` call sites compiling;
    /// tests/previews pass a fake. AppDI composition root takes over later.
    init(adapter: NotesBridgeAdapter = NotesBridgeAdapter()) {
        self.adapter = adapter

        // Live notes list (replaces NoteRepositoryHelper.noteListStateHelper).
        // Bridge callbacks arrive on the main thread — direct assignment is safe.
        adapter.observeNotes { [weak self] notesState in
            self?.state.notes = notesState.notes as? [Note] ?? []
        }

        // Live tags (replaces getTagsHelper). The old clear()-all-notes quirk
        // on tag emissions is intentionally dropped (rev 1 §6.2).
        adapter.observeTags { [weak self] tags in
            self?.state.tags = tags
        }
    }

    // MARK: - API triggers

    /// Called from NotesView.onAppear — same name, new internals.
    func getNotesCall() {
        adapter.getNotes(page: state.page) { [weak self] result in
            guard let self else { return }
            switch result {
            case .loading:
                self.state.isLoading = true
            case .success:
                self.state.isLoading = false
                // list content arrives via observeNotes — single source of truth
            case .failure(let error):
                self.state.isLoading = false
                self.state.errorMessage = error.message
                print("getNotes failed [\(error.code)] \(error.message)")
            case .idle:
                break
            }
        }
    }

    func createTag() {
        guard !state.tagName.isEmpty else { return }
        let tag = Tag(label: state.tagName, color: state.color.tohexColor())
        adapter.createTag(tag: tag) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success:
                self.state.tagName = ""            // tag list refreshes via observeTags
            case .failure(let error):
                self.state.errorMessage = error.message
                print("createTag failed [\(error.code)] \(error.message)")
            case .loading, .idle:
                break
            }
        }
    }
    // No deinit — adapter.deinit closes observers + in-flight calls.
}
```

## 5. `NotesView.swift` — mechanical binding-path edits ONLY

Rule: every **property** access `notesHandler.x` → `notesHandler.state.x`. **Method** calls (`getNotesCall()`, `createTag()`) unchanged. Verified against the actual file — complete list:

| Line (approx) | Before | After |
|---|---|---|
| staggeredGrid | `items: notesHandler.notes` | `items: notesHandler.state.notes` |
| tagsUi | `ForEach(notesHandler.tags)` | `ForEach(notesHandler.state.tags)` |
| tagsUi (+Tag) | `notesHandler.showSheet = true` | `notesHandler.state.showSheet = true` |
| createTag view | `.foregroundColor(notesHandler.color)` | `notesHandler.state.color` |
| createTag view | `notesHandler.showColorPalette = true` | `notesHandler.state.showColorPalette = true` |
| createTag view | `text: $notesHandler.tagName` | `$notesHandler.state.tagName` |
| createTag view | `if notesHandler.showColorPalette` | `notesHandler.state.showColorPalette` |
| Done button | `notesHandler.showSheet = false` / `showColorPalette = false` | `state.` versions |
| colorSelector | `$notesHandler.color` | `$notesHandler.state.color` |
| body | `if notesHandler.notes.isEmpty` | `notesHandler.state.notes.isEmpty` |
| body | `if notesHandler.isLoading` | `notesHandler.state.isLoading` |
| sheet | `isPresented: $notesHandler.showSheet` | `$notesHandler.state.showSheet` |
| onDisappear | `notesHandler.isLoading = false` | `notesHandler.state.isLoading = false` |
| onAppear | `notesHandler.getNotesCall()` | **unchanged** (method) |
| Done button | `notesHandler.createTag()` | **unchanged** (method) |

~13 one-token edits; screen structure, layout, and logic untouched. Bindings through a published struct (`$notesHandler.state.tagName`) are fully supported by SwiftUI.

### Functional-equivalence check (why the single struct is safe here)

- `ObservableObject` has no per-property tracking: eight `@Published`s or one — **any** change triggers the same whole-view `objectWillChange`. Render behavior identical.
- TextField typing (`state.tagName`) republishes the struct per keystroke — exactly what `@Published var tagName` already did.
- No `didSet` observers, no Combine pipelines subscribe to individual properties in the current code → nothing breaks by moving fields into a struct. (`NotesViewUIState` enum kept, still unused; nothing deleted.)

---

## 6. Behavior notes (carried from rev 1)

1. `page` stays 1 — matches today; DAO ignores offset anyway (Piece-1 refactor).
2. Dropped `clear()`-on-tags-emission quirk — only intentional behavior change.
3. `errorMessage` additive; View may ignore it.
4. Editor screen untouched — old and new paths share the same `NoteRepository` singleton.

## 7. Apply order

1. Kotlin bridge files + `tags` accessor → build shared framework.
2. Xcode: create `bridge/` and `bridge/notes/` groups, add the 2 Swift files, check **Target Membership: iosApp** (project is `objectVersion 54` — not auto-synced).
3. Replace `NotesViewModel.swift` with §4.
4. Apply the §5 table to `NotesView.swift` (mechanical).
5. Run §8.

## 8. Verification scenarios

1. Fresh install → empty UI, no stuck spinner.
2. Existing notes → list on appear; loading toggles once.
3. Create note in editor (old path) → back → appears without re-entry (shared repo emission).
4. Create tag → chip appears immediately, `tagName` resets, sheet closed.
5. Notes ↔ Home ×5 → one observer per VM (no duplicate emissions in logs).
6. Leave screen mid-load → no crash (`[weak self]` + Closeable cancel).
7. Forced error → `state.errorMessage` set, list intact.
8. Preview/test: `NotesViewModel(adapter: FakeAdapter())` — DI seam works.
9. Android parity: same ops, same repository emissions.
