# getNotes — Full End-to-End Implementation (iOS, Offline)

Companion to `ios-notes-usecase-bridge-plan.md`. This is the **complete, copy-ready code** for one vertical slice: fetching + observing the notes list from SwiftUI down to SQLDelight, through use cases only, with **no bridging library**.

**Code status: documentation only — none of this is applied to the project yet.**

```
NotesView (SwiftUI)                          UNCHANGED
  └─ NotesViewModel (Swift, Combine)         MODIFIED (internals only, same @Published)
       └─ NotesBridgeAdapter (Swift)         NEW
            └─ NotesBridge (Kotlin iosMain)  NEW
                 └─ GetNotesUseCase          EXISTING commonMain — untouched
                      └─ NoteRepository      EXISTING — untouched
                           └─ NotesDao → SQLDelight (offline)
```

---

## LAYER 1 — Kotlin `iosMain` (new package `com.app.pustakam.bridge`)

### 1.1 `bridge/Closeable.kt`

```kotlin
package com.app.pustakam.bridge

import kotlinx.coroutines.Job

/** Cancellation handle Swift can hold. Call close() in deinit/onDisappear. */
class Closeable(private val job: Job) {
    fun close() = job.cancel()
}
```

### 1.2 `bridge/BridgeError.kt`

```kotlin
package com.app.pustakam.bridge

import com.app.pustakam.util.Error
import com.app.pustakam.util.ErrorMessage
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.ValidationError

/** The single error shape Swift ever sees. */
data class BridgeError(
    val code: String,      // "NOT_FOUND", "SERVER_ERROR", "VALIDATION_EMAIL", "UNKNOWN"...
    val message: String    // human-readable, from getError()/message
)

internal fun Error.toBridgeError(): BridgeError = when (this) {
    is NetworkError    -> BridgeError(code = name, message = getError())
    is ValidationError -> BridgeError(code = "VALIDATION_$name", message = getError())
    is ErrorMessage    -> BridgeError(code = "UNKNOWN", message = message)
    else               -> BridgeError(code = "UNKNOWN", message = toString())
}
```

### 1.3 `bridge/FlowBridge.kt`

```kotlin
package com.app.pustakam.bridge

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Observe any Flow (used for StateFlow observation e.g. notes list). */
internal fun <T> Flow<T>.watch(
    scope: CoroutineScope,
    onEach: (T) -> Unit
): Closeable {
    val job = scope.launch {
        collectLatest { onEach(it) }
    }
    return Closeable(job)
}

/**
 * Run a use case and fan Result out to typed callbacks.
 * `producer` is suspend because use-case invoke() is suspend.
 * This is the ONLY place in the codebase that unwraps Result for iOS.
 */
internal fun <T> subscribeTo(
    scope: CoroutineScope,
    producer: suspend () -> Flow<Result<BaseResponse<T>, Error>>,
    onLoading: () -> Unit,
    onSuccess: (T?) -> Unit,       // unwrapped BaseResponse.data
    onError: (BridgeError) -> Unit
): Closeable {
    val job = scope.launch {
        producer().collectLatest { result ->
            when (result) {
                is Result.Loading -> onLoading()
                is Result.Success -> onSuccess(result.data.data)
                is Result.Error   -> onError(result.error.toBridgeError())
            }
        }
    }
    return Closeable(job)
}
```

### 1.4 `bridge/notes/NotesBridge.kt` (getNotes slice only — other CRUD methods follow the same pattern)

```kotlin
package com.app.pustakam.bridge.notes

import com.app.pustakam.bridge.BridgeError
import com.app.pustakam.bridge.Closeable
import com.app.pustakam.bridge.subscribeTo
import com.app.pustakam.bridge.watch
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.domain.repositories.usecases.GetNotesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The ONLY entry point iOS uses for Notes.
 * One instance per Swift ViewModel. Owns its scope → dispose() kills everything.
 */
class NotesBridge {

    // Main-dispatcher scope: every callback lands on the main thread.
    // Heavy work still runs on IO — getBaseApiCall() does flowOn(Dispatchers.IO).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // lazy → Koin resolution happens at first call, not at construction.
    private val getNotesUseCase by lazy { GetNotesUseCase() }

    /** One-shot: load notes for a page (offline → DAO under the hood). */
    fun getNotes(
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (Notes?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(
        scope = scope,
        producer = { getNotesUseCase(page) },   // suspend invoke → Flow<Result<...>>
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError
    )

    /** Reactive: live notes list (repo pushes updates on insert/update/delete). */
    fun observeNotes(onChange: (Notes) -> Unit): Closeable =
        getNotesUseCase.notes.watch(scope) { onChange(it) }

    /** Swift calls from deinit — cancels observers AND in-flight calls. */
    fun dispose() = scope.cancel()
}
```

> Exported ObjC signatures Swift will see (for reference):
> `getNotes(page: Int32, onLoading: @escaping () -> Void, onSuccess: @escaping (Notes?) -> Void, onError: @escaping (BridgeError) -> Void) -> Closeable`
> `observeNotes(onChange: @escaping (Notes) -> Void) -> Closeable`

---

## LAYER 2 — Kotlin `commonMain` (existing, shown for the reader — NO changes for getNotes)

```kotlin
// usecases/NoteUseCase.kt — EXISTING
class GetNotesUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(page: Int) =
        getBaseApiCall { noteRepository.getAllNotes(page) }
}

// usecases/BaseUseCase.kt — EXISTING (`notes` StateFlow already exposed)
abstract class NoteBaseUseCase : BaseUseCase() {
    protected val noteRepository = repository as NoteRepository
    val notes = noteRepository.notesState          // ← observeNotes uses this
    override fun setRepository(): NoteRepository = get<NoteRepository>()
}

// getBaseApiCall — EXISTING: emits Loading, then result, on Dispatchers.IO
fun <T> getBaseApiCall(apiCall: suspend () -> Result<T, Error>): Flow<Result<T, Error>> =
    flow {
        emit(Result.Loading)
        emit(apiCall())
    }.flowOn(Dispatchers.IO)

// NoteRepository.getAllNotes — EXISTING (offline path; API call commented until Online phase)
suspend fun getAllNotes(page: Int = 0): Result<BaseResponse<Notes>, Error> =
    getNotesFromDb(page).onSuccess { notes ->
        if (notes.data?.notes?.count()!! > 0) insertNotes(notes = notes.data)  // → notesState emits
    }
```

---

## LAYER 3 — Swift `iosApp/bridge/`  ⚠️ add both files to the Xcode target

### 3.1 `bridge/UiState.swift`

```swift
import Foundation
import shared

/// Single UI-state surface for any bridge call.
enum UiState<T> {
    case idle
    case loading
    case success(T?)
    case failure(BridgeError)
}
```

### 3.2 `bridge/notes/NotesBridgeAdapter.swift`

```swift
import Foundation
import shared

/// Owns the Kotlin bridge + every Closeable it hands out.
/// ViewModels never touch Closeable or dispose() — deinit here handles it.
final class NotesBridgeAdapter {

    private let bridge = NotesBridge()
    private var closeables: [Closeable] = []

    deinit {
        closeables.forEach { $0.close() }
        bridge.dispose()
    }

    /// One-shot fetch, surfaced as UiState via a single callback.
    func getNotes(page: Int, onState: @escaping (UiState<Notes>) -> Void) {
        let closeable = bridge.getNotes(
            page: Int32(page),
            onLoading: { onState(.loading) },
            onSuccess: { notes in onState(.success(notes)) },
            onError:   { error in onState(.failure(error)) }
        )
        closeables.append(closeable)
    }

    /// Live notes list — fires on every insert/update/delete in the repo.
    func observeNotes(onChange: @escaping (Notes) -> Void) {
        let closeable = bridge.observeNotes(onChange: onChange)
        closeables.append(closeable)
    }
}
```

---

## LAYER 4 — Swift ViewModel (internals swapped; same `@Published` names ⇒ **View untouched**)

### `view/screens/notes/noteList/NotesViewModel.swift` (getNotes slice)

```swift
import shared
import Combine
import SwiftUI

class NotesViewModel: ObservableObject {

    // @Published names identical to today — NotesView binds without any change
    @Published var page: Int = 1
    @Published var notes = [Note]()
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    private let adapter = NotesBridgeAdapter()

    init() {
        // replaces NoteRepositoryHelper().noteListStateHelper (which leaked)
        adapter.observeNotes { [weak self] state in
            // already on main thread — no DispatchQueue.main.async needed
            self?.notes = state.notes as? [Note] ?? []
        }
        getNotesCall()
    }

    // replaces Task + apiHandler + ResultSuccess<T> casting
    func getNotesCall() {
        adapter.getNotes(page: page) { [weak self] state in
            guard let self else { return }
            switch state {
            case .loading:
                self.isLoading = true
            case .success:
                self.isLoading = false
                self.page += 1
                // list itself arrives via observeNotes — single source of truth
            case .failure(let error):
                self.isLoading = false
                self.errorMessage = error.message   // e.g. "No Record found"
            case .idle:
                break
            }
        }
    }
    // NO deinit needed — adapter cleans up automatically
}
```

### LAYER 5 — `NotesView.swift` — **ZERO changes** (shown to prove it)

```swift
// binds exactly as today:
// @StateObject var viewModel = NotesViewModel()
// List(viewModel.notes, ...) / viewModel.isLoading / viewModel.getNotesCall()
```

---

## Runtime sequence (offline)

```
NotesView appears
  → NotesViewModel.init
      → adapter.observeNotes ──────────► NotesBridge.observeNotes
                                            └ notesState.watch(scope)   [Closeable #1]
      → getNotesCall()
          → adapter.getNotes(page:1) ──► NotesBridge.getNotes           [Closeable #2]
                → GetNotesUseCase(1)
                    emit Result.Loading ──────────► onLoading → isLoading = true   (main)
                    [IO thread] NoteRepository.getAllNotes(1)
                        → NotesDao.selectAllNotesFromDb → SQLDelight
                        → insertNotes(...) → _notes.update → notesState emits
                    emit Result.Success(BaseResponse(Notes)) ─► onSuccess → isLoading=false (main)
      notesState emission ────────────► observeNotes callback → notes = [...] → SwiftUI re-renders

NotesViewModel deallocates
  → adapter.deinit → Closeable #1 & #2 .close() → bridge.dispose() → zero leaks
```

## Checks covered by this slice (from plan §5)

| # | Scenario | Where handled |
|---|----------|---------------|
| 1 | Empty DB | `Result.Success` with empty `Notes` → `observeNotes` emits empty list |
| 8 | Reopen screen ×5, one observer | adapter `deinit` closes closeables |
| 9 | Navigate away mid-request | `Closeable #2.close()` cancels the coroutine |
| 10 | Callbacks on main thread | bridge scope = `Dispatchers.Main` |
| 13 | DB exception | `ErrorMessage` → `BridgeError("UNKNOWN", msg)` — no silent cast failure |
| 15 | Loading emitted first | `getBaseApiCall` emits `Result.Loading` |

## Same pattern for the rest of CRUD

Every other method is a clone of `getNotes` with a different use case and payload type — `readNote(noteId:)` → `ReadNoteUseCase` → `Note?`; `createOrUpdateNote(note:)` → `CreateORUpdateNoteUseCase` → `Note?`; `deleteNote(noteId:)` → `DeleteNoteUseCase` → `KotlinBoolean?`; tags likewise. No new concepts.
