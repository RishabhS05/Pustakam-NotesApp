# iOS ⇄ Kotlin Bridge Plan — Notes CRUD via Use Cases (No 3rd-party Library)

**Scope:** Offline-only Notes + Tags CRUD. Architecture files only. No feature-file changes. Online sync comes after this works.
**Rule:** Both platforms consume **only use cases** (SOLID — depend on abstractions, not `NoteRepository`). Bridge is hand-written Kotlin in `iosMain` — no SKIE, no KMP-NativeCoroutines.

---

## 1. Current Problems (why iOS is broken by design today)

| # | Problem | Where |
|---|---------|-------|
| 1 | Swift ViewModels call repositories directly (`KoinHelper().getNoteRepository()`), skipping use cases. Android uses `GetNotesUseCase` etc. — the two platforms have different entry points into domain. | `iosApp/view/screens/base/BaseViewModel.swift`, `NotesViewModel.swift`, `NoteEditorViewModel.swift` |
| 2 | Use cases return `Flow<Result<T, Error>>`. Kotlin `Flow` has no usable Swift API and ObjC erases generics — so use cases are *uncallable* from Swift today, which is why repos got exposed. | `BaseUseCase.getBaseApiCall()` |
| 3 | Swift unwraps results with `as? ResultSuccess<T>` / `as? ResultError<NetworkError>` casts. Generic erasure makes this fragile; `ErrorMessage` errors (non-enum) would fail the cast silently. | `network/BaseNetworkHandler.swift` |
| 4 | Observers (`NoteRepositoryHelper`, `ObservableStateFlow`, `UserPreferenceViewModel`) launch `CoroutineScope(Dispatchers.Main)` jobs that are **never cancelled** → duplicated callbacks each time a VM is recreated + memory leaks. | `iosMain/domain/NoteRepositoryHelper.kt`, `iosMain/util/ObservableStateFlow.kt` |
| 5 | `NoteRepositoryHelper` / `KoinHelper` are `object` singletons resolving repos at class-init — crash risk if touched before `initKoin()`. | `iosMain` |
| 6 | No unified Loading/Success/Error surface in Swift — `Result.Loading` is dropped by `apiHandler`. | Swift side |

---

## 2. Target Architecture

```
SwiftUI View
   └── Swift ViewModel (ObservableObject)          [iosApp — thin, UI state only]
         └── NotesBridge (Kotlin, iosMain)          [NEW — the only API Swift sees]
               ├── CreateORUpdateNoteUseCase        [commonMain — unchanged]
               ├── ReadNoteUseCase / GetNotesUseCase
               ├── DeleteNoteUseCase / DeleteNoteContentUseCase
               ├── Tag use cases (GetTagCase, CreateTagUseCase, UpdateTagUseCase, DeleteTagUseCase)
               └── notes/tags StateFlow observation → callback + Closeable
                     └── NoteRepository → NotesDao → SQLDelight   [unchanged]
```

Android is already correct (VM → UseCase) — **zero Android changes**.

Bridge contract (applies to every method):

- Swift never sees `Flow`, `Result`, or casts. Only typed callbacks: `onLoading`, `onSuccess(data)`, `onError(code, message)`.
- Every subscription returns a `Closeable`; Swift calls `close()` in `deinit`/`onDisappear`.
- All callbacks delivered on **main thread**; work runs on `Dispatchers.IO` inside use cases (already done by `flowOn`).

---

## 3. New Files (the only additions)

### 3.0 Final File Placement (agreed)

**Kotlin — `shared/src/iosMain/kotlin/com/app/pustakam/`** (auto-compiled by Gradle, no registration):

```
com/app/pustakam/
├── bridge/                      ← NEW top-level package (interface-adapter layer, not domain)
│   ├── Closeable.kt             generic — reused by every future bridge
│   ├── FlowBridge.kt            internal plumbing, never exported to Swift
│   ├── BridgeError.kt           error mapping
│   └── notes/
│       └── NotesBridge.kt       feature-scoped; future: bridge/auth/, bridge/notecontent/
├── domain/NoteRepositoryHelper.kt   stays for now; deprecated, deleted later with approval
├── koinDI/ …                        untouched
└── util/ …                          untouched
```

**commonMain:** only `usecases/BaseUseCase.kt` edited (2-line `tags` accessor). Nothing added.

**Swift — `iosApp/iosApp/`** (mirrors Kotlin layout):

```
iosApp/iosApp/
├── bridge/                      ← NEW folder
│   ├── UiState.swift            generic state enum
│   └── notes/
│       └── NotesBridgeAdapter.swift
├── network/BaseNetworkHandler.swift   stays (login/signup still use it)
└── view/screens/notes/…               VMs edited in a later step only
```

⚠️ Swift files must be added to the Xcode target (project navigator) — unlike Kotlin, they are not picked up automatically.


### 3.1 `shared/src/iosMain/kotlin/com/app/pustakam/bridge/Closeable.kt`

```kotlin
class Closeable(private val job: Job) {
    fun close() = job.cancel()
}
```

### 3.2 `shared/src/iosMain/kotlin/com/app/pustakam/bridge/FlowBridge.kt`

Generic internal plumbing (not exposed to Swift directly):

```kotlin
internal fun <T> Flow<T>.watch(scope: CoroutineScope, onEach: (T) -> Unit): Closeable {
    val job = scope.launch { collectLatest { onEach(it) } }
    return Closeable(job)
}

/** Collects Flow<Result<BaseResponse<T>, Error>> from a use case and
 *  fans out to typed callbacks. One place that understands Result. */
internal fun <T> Flow<Result<BaseResponse<T>, Error>>.subscribe(
    scope: CoroutineScope,
    onLoading: () -> Unit,
    onSuccess: (T?) -> Unit,          // unwraps BaseResponse.data
    onError: (BridgeError) -> Unit
): Closeable
```

### 3.3 `shared/src/iosMain/kotlin/com/app/pustakam/bridge/BridgeError.kt`

Single error shape for Swift (kills the `as? ResultError<NetworkError>` casts):

```kotlin
data class BridgeError(
    val code: String,        // e.g. "NOT_FOUND", "SERVER_ERROR", "UNKNOWN"
    val message: String
)
// + internal fun Error.toBridgeError(): BridgeError
//   (maps NetworkError enum entries and ErrorMessage.error text)
```

### 3.4 `shared/src/iosMain/kotlin/com/app/pustakam/bridge/notes/NotesBridge.kt`

The **only** class iOS Notes screens instantiate. One instance per Swift ViewModel; owns its scope.

```kotlin
class NotesBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // use cases resolved lazily → safe even if created before Koin (fails at call, not init)
    private val getNotesUseCase by lazy { GetNotesUseCase() }
    private val readNoteUseCase by lazy { ReadNoteUseCase() }
    private val upsertNoteUseCase by lazy { CreateORUpdateNoteUseCase() }
    private val deleteNoteUseCase by lazy { DeleteNoteUseCase() }
    private val deleteNoteContentUseCase by lazy { DeleteNoteContentUseCase() }
    private val getTagsUseCase by lazy { GetTagCase() }
    private val createTagUseCase by lazy { CreateTagUseCase() }
    private val updateTagUseCase by lazy { UpdateTagUseCase() }
    private val deleteTagUseCase by lazy { DeleteTagUseCase() }

    // ---- one-shot CRUD (each returns Closeable so in-flight work is cancellable) ----
    fun getNotes(page: Int, onLoading: () -> Unit,
                 onSuccess: (Notes?) -> Unit, onError: (BridgeError) -> Unit): Closeable
    fun readNote(noteId: String?, ...)          // null id → repo returns new empty Note
    fun createOrUpdateNote(note: Note, ...)
    fun deleteNote(noteId: String?, ...)
    fun deleteNoteContent(contentId: String?, ...)

    fun getTags(...) ; fun createTag(tag: Tag, ...) ; fun updateTag(tag: Tag, ...) ; fun deleteTag(tagId: String?, ...)

    // ---- reactive state (replaces NoteRepositoryHelper) ----
    fun observeNotes(onChange: (Notes) -> Unit): Closeable   // use case's `notes` StateFlow
    fun observeTags(onChange: (List<Tag>) -> Unit): Closeable

    /** Cancel everything this bridge started. Swift calls from deinit. */
    fun dispose() = scope.cancel()
}
```

Notes:

- Reactive state comes via `NoteBaseUseCase.notes` (already exposed on use cases) — **not** via `KoinHelper.getNoteRepository()`. `tagState` needs a small accessor on the use case (see §4.1).
- Per-instance scope fixes leak #4: `dispose()` cancels every observer and in-flight call.

### 3.5 Swift architecture files — `iosApp/iosApp/bridge/`

| File | Purpose |
|------|---------|
| `NotesBridgeAdapter.swift` | Thin `ObservableObject`-friendly wrapper: holds `NotesBridge`, stores returned `Closeable`s in an array, `deinit { closeables.forEach { $0.close() }; bridge.dispose() }`. Exposes Swift-y async-style or closure API to VMs. |
| `UIState.swift` | `enum UiState<T> { case idle, loading, success(T), failure(BridgeError) }` — replaces `BaseResult` + `apiHandler` casting for Notes screens. |

---

## 4. Modified Files (edits only — nothing deleted without your sign-off)

### 4.1 Kotlin (`shared`)

| File | Change |
|------|--------|
| `commonMain/.../usecases/BaseUseCase.kt` | Add `val tags = noteRepository.tagState` to `NoteBaseUseCase` (mirror of existing `notes`) so tags observation also flows through use cases. **No other change.** |
| `iosMain/.../koinDI/KoinHelper.kt` | Mark `getNoteRepository()` / `getNoteContentRepository()` `@Deprecated("Use NotesBridge")` — kept until Swift VMs migrate (delete later, with your permission). |
| `iosMain/.../domain/NoteRepositoryHelper.kt` | Same — `@Deprecated`, superseded by `observeNotes`/`observeTags`. Not deleted yet. |

### 4.2 Swift (`iosApp`) — feature files touched *only* to swap the data source, no UI/behavior change

| File | Change |
|------|--------|
| `view/screens/base/BaseViewModel.swift` | Stop exposing `noteRepositary`/`noteContentRepository` for Notes screens; Notes VMs get a `NotesBridgeAdapter` instead. |
| `view/screens/notes/noteList/NotesViewModel.swift` | Replace `NoteRepositoryHelper().noteListStateHelper{}` → `adapter.observeNotes`; `noteRepositary.getAllNotes` → `adapter.getNotes(page:)`; `createTagOnDB` → `adapter.createTag`. Store closeables; drop `as!` casts. |
| `view/screens/notes/noteEditor/NoteEditorViewModel.swift` | Same swap for read/upsert/delete note + delete content. |
| `network/BaseNetworkHandler.swift` | Untouched for now (login/signup still use it). Notes flow stops using `apiHandler`. |

`iOSApp.swift` already calls `initKoin()` before any VM — verify order stays: Koin → first `NotesBridge` use.

---

## 5. Scenario Matrix (offline — all must pass before Online phase)

**CRUD**

1. Notes list, empty DB → `observeNotes` emits empty; UI shows empty state; no crash.
2. Notes list with data + pagination (`page` param; note: `NotesDao.selectAllNotesFromDb` currently computes `offset` but queries all — behavior kept as-is, documented).
3. Create note: `readNote(nil)` → empty `Note` (id pre-generated), save → `createOrUpdateNote` → list auto-updates via `observeNotes` (repo pushes into `_notes`).
4. Edit existing note → save → list reflects change without manual refresh.
5. Read note by id: found → data; missing id in DB → `NOT_FOUND` error surfaces as `BridgeError`, not a cast failure.
6. Delete note → removed from DB and from observed list. Delete note content → content row gone, note stays.
7. Tags: create (hex color from Swift `Color.tohexColor()`), update, delete, get; empty tag list returns `NOT_FOUND` today — bridge maps it to `onSuccess([])`? **No** — keep repo behavior, surface as error; UI decides. (Documented, not changed.)

**Lifecycle / threading**

8. Open Notes → back → reopen ×5: exactly one active observer (old ones closed in `deinit`) — fixes today's duplicate-callback bug.
9. Kill view mid-request (navigate away while `getNotes` in flight) → `Closeable.close()` cancels; no callback into deallocated VM (`[weak self]` in adapter).
10. All callbacks land on main thread — assert via `Thread.isMainThread` in debug.
11. Rapid save taps (double-tap save) → two upserts serialize on same note id; last write wins; no duplicate list rows (repo replaces by id).

**Error / edge**

12. `deleteNote(nil)` / empty id → `BridgeError(NOT_FOUND)`, no crash (today `noteId ?? ""` deletes nothing and returns SERVER_ERROR — surfaced cleanly).
13. DB exception (e.g. constraint) → `ErrorMessage` path → `BridgeError(code = "UNKNOWN", message = stacktrace)` — the cast bug #3 today would show nothing.
14. Bridge created before `initKoin()` → lazy resolution defers crash to first call with clear message (verify in test).
15. Loading state: every one-shot call emits `onLoading` before result (comes from `Result.Loading` in `getBaseApiCall`).

**Parity check**

16. Same action on Android and iOS (create/edit/delete note, tag CRUD) produces identical DB rows — both go through the identical use case → repository path.

---

## 6. Explicitly Out of Scope (next phases)

- Online sync (`upsertNewNoteApi`, `deleteNoteApi`, … currently commented in repo) — bridge API won't change when enabled; only repo internals do. That's the payoff of exposing use cases only.
- `NoteContentRepository` media flows (`noteContentMediaList`) — migrate after Notes CRUD proves the pattern.
- Login/Signup use cases over the bridge.
- Deleting deprecated `NoteRepositoryHelper` / `KoinHelper` repo getters — separate approval.
- (Recommendation only) `-Xmemory-model=experimental` in `shared/build.gradle.kts` is obsolete since Kotlin 1.7.20's new memory model became default; removable later.

## 7. Suggested Order of Work

1. `Closeable.kt` + `FlowBridge.kt` + `BridgeError.kt` (pure additions, nothing depends on them).
2. `tags` accessor in `NoteBaseUseCase` (2-line commonMain change; Android unaffected).
3. `NotesBridge.kt` — build framework, confirm ObjC header exports clean signatures (`Note`, `Notes`, `Tag` already export fine — Swift uses them today).
4. Swift `NotesBridgeAdapter` + `UIState`.
5. Swap `NotesViewModel.swift`, then `NoteEditorViewModel.swift`.
6. Run scenario matrix §5; then deprecation annotations.
