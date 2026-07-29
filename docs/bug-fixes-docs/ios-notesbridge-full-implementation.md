# NotesBridge — Full Implementation (Koin-injected Use Cases)

Companion to `ios-notes-usecase-bridge-plan.md` (architecture) and `ios-getnotes-end-to-end-code.md` (getNotes vertical slice).
This doc contains the **complete `NotesBridge` with all CRUD + Tag + observation functions**, with use cases resolved via **Koin `inject()`** instead of direct construction.

**Code status: documentation only — nothing applied to the project yet.**

---

## 0. Why `inject()` works already (verified against your code)

- `commonMain/koinDI/Koin.kt` **already registers all Notes/Tag use cases** as `factory`:
  `CreateORUpdateNoteUseCase`, `DeleteNoteUseCase`, `ReadNoteUseCase`, `GetNotesUseCase`, `DeleteNoteContentUseCase`, `GetTagCase`, `CreateTagUseCase`, `UpdateTagUseCase`, `DeleteTagUseCase`.
- `iOSApp.swift` already boots the right container: `KoinKt.doInitKoin(appDeclaration: {_ in})` → the commonMain `initKoin`, which includes the `useCases` module. **No DI changes needed.**
- `factory` + `by inject()` semantics: each `NotesBridge` instance resolves its own use-case instances, **lazily at first use** (not at construction). So creating a bridge before Koin starts cannot crash; a clear Koin error appears only if a function is called pre-init.

> ⚠️ Side finding (review later, not part of this change): `iosMain/koinDI/Koin.kt` contains an older `initKoin()` that references `repositoriesModules` and does **not** include the `useCases` module. iOS doesn't call it (`iOSApp.swift` uses `doInitKoin`). Candidate for deprecation — with your approval, separately.

### Injection style: `inject()` vs `KoinComponent.get()`

```kotlin
class NotesBridge : KoinComponent {
    private val getNotesUseCase: GetNotesUseCase by inject()   // ← lazy, resolved once per bridge
}
```

`by inject()` = lazy `get()`. Because the Koin definitions are `factory`, every bridge gets fresh use-case objects; the use cases themselves share the same `single { NoteRepository() }` underneath — so state (`notesState`, `tagState`) is one source of truth across all bridges and Android-style parity is preserved.

---

## 1. Prerequisite: 2-line commonMain edit (plan §4.1)

`NoteBaseUseCase` already exposes `notes`; tags observation needs the mirror accessor:

```kotlin
// commonMain/.../usecase/BaseUseCase.kt  — NoteBaseUseCase
abstract class NoteBaseUseCase : BaseUseCase() {
    protected val noteRepository = repository as NoteRepository
    val notes = noteRepository.notesState
    val tags  = noteRepository.tagState        // ← ADD THIS LINE (only change in commonMain)
    override fun setRepository(): NoteRepository = get<NoteRepository>()
}
```

Android unaffected (additive).

---

## 2. Supporting bridge files (unchanged from the getNotes doc, shown for completeness)

### 2.1 `iosMain/.../bridge/Closeable.kt`

```kotlin
package com.app.pustakam.bridge

import kotlinx.coroutines.Job

/** Cancellation handle Swift can hold. Call close() in deinit/onDisappear. */
class Closeable(private val job: Job) {
    fun close() = job.cancel()
}
```

### 2.2 `iosMain/.../bridge/BridgeError.kt`

```kotlin
package com.app.pustakam.bridge

import com.app.pustakam.util.Error
import com.app.pustakam.util.ErrorMessage
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.ValidationError

/** The single error shape Swift ever sees. */
data class BridgeError(
    val code: String,      // "NOT_FOUND", "SERVER_ERROR", "VALIDATION_EMAIL", "UNKNOWN"...
    val message: String
)

internal fun Error.toBridgeError(): BridgeError = when (this) {
    is NetworkError    -> BridgeError(code = name, message = getError())
    is ValidationError -> BridgeError(code = "VALIDATION_$name", message = getError())
    is ErrorMessage    -> BridgeError(code = "UNKNOWN", message = message)
    else               -> BridgeError(code = "UNKNOWN", message = toString())
}
```

### 2.3 `iosMain/.../bridge/FlowBridge.kt`

```kotlin
package com.app.pustakam.bridge

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Observe any Flow/StateFlow → callback. Used by observeNotes/observeTags. */
internal fun <T> Flow<T>.watch(
    scope: CoroutineScope,
    onEach: (T) -> Unit
): Closeable {
    val job = scope.launch { collectLatest { onEach(it) } }
    return Closeable(job)
}

/**
 * Run a use case (suspend producer of Flow<Result<BaseResponse<T>, Error>>)
 * and fan Result out to typed callbacks. The ONLY place that unwraps Result for iOS.
 */
internal fun <T> subscribeTo(
    scope: CoroutineScope,
    producer: suspend () -> Flow<Result<BaseResponse<T>, Error>>,
    onLoading: () -> Unit,
    onSuccess: (T?) -> Unit,          // unwrapped BaseResponse.data
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

---

## 3. `iosMain/.../bridge/notes/NotesBridge.kt` — FULL CODE

All 9 use-case functions + 2 observers + dispose, matching plan §3.4.

```kotlin
package com.app.pustakam.bridge.notes

import com.app.pustakam.bridge.BridgeError
import com.app.pustakam.bridge.Closeable
import com.app.pustakam.bridge.subscribeTo
import com.app.pustakam.bridge.watch
import com.app.pustakam.data.models.Tag
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.domain.repositories.usecases.CreateORUpdateNoteUseCase
import com.app.pustakam.domain.repositories.usecases.CreateTagUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteNoteContentUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteNoteUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteTagUseCase
import com.app.pustakam.domain.repositories.usecases.GetNotesUseCase
import com.app.pustakam.domain.repositories.usecases.GetTagCase
import com.app.pustakam.domain.repositories.usecases.ReadNoteUseCase
import com.app.pustakam.domain.repositories.usecases.UpdateTagUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The ONLY Notes entry point for iOS. One instance per Swift ViewModel.
 *
 * - Use cases arrive via Koin `inject()` (factory scope, lazy → no pre-initKoin crash).
 * - Own scope on Dispatchers.Main → every callback lands on the main thread.
 *   (Heavy work still runs on IO: getBaseApiCall() does flowOn(Dispatchers.IO).)
 * - Every function returns a Closeable; dispose() cancels everything at once.
 */
class NotesBridge : KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ---- injected use cases (resolved lazily, per-bridge instances) ----
    private val getNotesUseCase: GetNotesUseCase by inject()
    private val readNoteUseCase: ReadNoteUseCase by inject()
    private val upsertNoteUseCase: CreateORUpdateNoteUseCase by inject()
    private val deleteNoteUseCase: DeleteNoteUseCase by inject()
    private val deleteNoteContentUseCase: DeleteNoteContentUseCase by inject()
    private val getTagsUseCase: GetTagCase by inject()
    private val createTagUseCase: CreateTagUseCase by inject()
    private val updateTagUseCase: UpdateTagUseCase by inject()
    private val deleteTagUseCase: DeleteTagUseCase by inject()

    /* ============================ NOTES — one-shot CRUD ============================ */

    /** Page of notes from local DB. List content itself arrives via observeNotes. */
    fun getNotes(
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (Notes?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { getNotesUseCase(page) }, onLoading, onSuccess, onError)

    /**
     * Read one note. Pass null/absent id → repository returns a NEW empty Note
     * (pre-generated id + timestamps) — this is the "create note" entry too.
     */
    fun readNote(
        noteId: String?,
        onLoading: () -> Unit,
        onSuccess: (Note?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { readNoteUseCase(noteId) }, onLoading, onSuccess, onError)

    /** Insert or update (repo decides by id) + pushes into notesState → list auto-refreshes. */
    fun createOrUpdateNote(
        note: Note,
        onLoading: () -> Unit,
        onSuccess: (Note?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { upsertNoteUseCase(note) }, onLoading, onSuccess, onError)

    /** Delete by id. Success payload is Boolean → Swift sees KotlinBoolean?. */
    fun deleteNote(
        noteId: String?,
        onLoading: () -> Unit,
        onSuccess: (Boolean?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { deleteNoteUseCase(noteId) }, onLoading, onSuccess, onError)

    /** Delete a single content block of a note (note itself stays). */
    fun deleteNoteContent(
        contentId: String?,
        onLoading: () -> Unit,
        onSuccess: (Boolean?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { deleteNoteContentUseCase(contentId) }, onLoading, onSuccess, onError)

    /* ============================ TAGS — one-shot CRUD ============================ */

    /** All tags. NOTE: empty DB currently yields NOT_FOUND error (repo behavior kept as-is). */
    fun getTags(
        onLoading: () -> Unit,
        onSuccess: (List<Tag>?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { getTagsUseCase() }, onLoading, onSuccess, onError)

    fun createTag(
        tag: Tag,
        onLoading: () -> Unit,
        onSuccess: (Tag?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { createTagUseCase(tag) }, onLoading, onSuccess, onError)

    fun updateTag(
        tag: Tag,
        onLoading: () -> Unit,
        onSuccess: (Tag?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { updateTagUseCase(tag) }, onLoading, onSuccess, onError)

    fun deleteTag(
        tagId: String?,
        onLoading: () -> Unit,
        onSuccess: (Boolean?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { deleteTagUseCase(tagId) }, onLoading, onSuccess, onError)

    /* ===================== REACTIVE STATE (replaces NoteRepositoryHelper) ===================== */

    /** Live notes list — fires on every insert/update/delete anywhere in the app. */
    fun observeNotes(onChange: (Notes) -> Unit): Closeable =
        getNotesUseCase.notes.watch(scope) { onChange(it) }

    /** Live tag list — requires the 2-line `tags` accessor from §1. */
    fun observeTags(onChange: (List<Tag>) -> Unit): Closeable =
        getTagsUseCase.tags.watch(scope) { onChange(it) }

    /* ======================================================================================== */

    /** Cancel every observer + in-flight call started by this bridge. Swift: call from deinit. */
    fun dispose() = scope.cancel()
}
```

---

## 4. What Swift sees (exported ObjC → Swift signatures)

```swift
let bridge = NotesBridge()

bridge.getNotes(page: 1,
    onLoading: { },
    onSuccess: { (notes: Notes?) in },
    onError:   { (e: BridgeError) in print(e.code, e.message) })   // → Closeable

bridge.readNote(noteId: nil) { ... }            // nil id → fresh empty Note (create flow)
bridge.createOrUpdateNote(note: note) { ... }   // onSuccess: Note?
bridge.deleteNote(noteId: id) { ... }           // onSuccess: KotlinBoolean?  ← see note below
bridge.deleteNoteContent(contentId: id) { ... } // onSuccess: KotlinBoolean?

bridge.getTags { ... }                          // onSuccess: [Tag]?
bridge.createTag(tag: tag) { ... }
bridge.updateTag(tag: tag) { ... }
bridge.deleteTag(tagId: id) { ... }

let c1 = bridge.observeNotes { (state: Notes) in /* main thread */ }
let c2 = bridge.observeTags  { (tags: [Tag]) in }

bridge.dispose()   // from adapter deinit
```

Type-mapping notes for Swift:

| Kotlin | Swift | Why |
|--------|-------|-----|
| `Int` (page) | `Int32` | Kotlin Int exports as Int32 — call with `Int32(page)` |
| `Boolean?` payload | `KotlinBoolean?` | nullable primitives box to Kotlin* classes; use `result?.boolValue == true` |
| `List<Tag>?` | `[Tag]?` | ObjC lightweight generic — bridges cleanly |
| `Notes`, `Note`, `Tag` | same names | plain exported classes, already used by Swift today |

---

## 5. Design decisions recap

1. **`by inject()` over `by lazy { X() }`** — your DI container already owns use-case creation for Android paths; the bridge now goes through the same registry (single composition root, easier to swap `factory` → `single` later, testable via Koin test modules). Behavior is identical because `inject()` is lazy too.
2. **One bridge per ViewModel, `factory` use cases, `single` repository** — bridges are cheap and disposable; state lives in `NoteRepository` (`notesState` / `tagState`), so every screen sees the same data.
3. **Typed callbacks per method** — generics die at the ObjC border; concrete signatures keep `Notes`/`Note`/`Tag` intact (no `as?` casts in Swift).
4. **`Closeable` returned everywhere + `dispose()`** — real Kotlin Job cancellation (Swift `Task` cancellation cannot cancel Kotlin coroutines without a library).
5. **Main-thread callbacks by construction** — scope is `Dispatchers.Main`; SwiftUI `@Published` writes are safe with no `DispatchQueue.main.async`.
6. **Repo behaviors intentionally NOT changed** (offline phase): empty tags → `NOT_FOUND` error; `deleteNote("")` → `SERVER_ERROR`; pagination offset computed but not applied in DAO. Bridge surfaces them honestly; fixes are separate approvals.

## 6. Next steps (in order)

1. Apply §1 two-line edit + create the 4 Kotlin files (§2, §3).
2. Build the shared framework; eyeball `shared.h` for the exported `NotesBridge` signatures.
3. Swift side: `NotesBridgeAdapter.swift` + `UiState.swift` (see end-to-end doc), add to Xcode target.
4. Swap `NotesViewModel.swift` internals, then `NoteEditorViewModel.swift`.
5. Run plan §5 scenario matrix.
