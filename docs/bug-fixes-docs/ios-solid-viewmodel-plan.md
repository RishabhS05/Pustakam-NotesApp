# iOS SOLID ViewModel Adoption Plan (mirror Android) — Signup first

> **Status:** Plan only. **No code changed.** Reviewed against `master`.
> **Goal:** Give iOS the same clean architecture Android already has — a `BaseViewModel` with a `makeAWish` engine that consumes the shared **Flow** use-cases and dispatches to `onLoading/onSuccess/onFailure`, native Swift `UIState` + `TaskCode`, ViewModels depending on **use-cases via Koin** (never repositories).
> **Order:** Steps 1–4 are one-time shared machinery. Steps 5–6 repeat per screen. We migrate **one screen at a time**; the old repo-based `BaseViewModel`/`apiHandler` stays until the last screen is migrated, then gets deleted.

## SOLID mapping (what each piece is responsible for)

- **SRP** — `UseCaseObserver` (Kotlin) only collects a Flow and forwards emissions; `BaseViewModel` (Swift) only dispatches + owns lifecycle; each screen VM owns one screen's state; `…UIState` only holds state; the `View` only renders.
- **OCP** — `BaseViewModel.makeAWish` is closed for modification; new screens extend it without touching it.
- **LSP** — every screen VM is substitutable as a `BaseViewModel`.
- **ISP** — `TaskCode` is a small protocol with per-feature enums; a VM implements only the handlers it needs.
- **DIP** — VMs depend on use-case abstractions resolved from Koin, not on concrete repositories.

## Layering (target)

```
SignupView (SwiftUI)
   └─ observes RegisterViewModel.uiState  (@Published SignupUIState)
        └─ RegisterViewModel : BaseViewModel   (Swift)
             └─ makeAWish(.signup) { signUseCase.invoke(user:) }   // shared Flow use-case
                  └─ UseCaseObserver (Kotlin/iosMain) collects Flow → onLoading/onSuccess/onError
```

---

## Step 0 — expose the use-case (Koin)

**File:** `shared/src/iosMain/kotlin/com/app/pustakam/koinDI/KoinHelper.kt`
**Change:** add a getter for the signup use-case (repos stay for now, removed at the end):

```kotlin
fun getSignUseCase() = get<SignUseCase>()
```

---

## Step 1 — Kotlin Flow→callback bridge (one-time, reused by every screen)

**New file:** `shared/src/iosMain/kotlin/com/app/pustakam/domain/UseCaseObserver.kt`
**Responsibility (SRP):** collect a `Flow<Result<T, Error>>` and forward each emission to Swift closures. Own a cancellable scope so the Swift VM can tear it down.

```kotlin
class UseCaseObserver {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun <T> observe(
        flow: Flow<Result<T, Error>>,
        onLoading: () -> Unit,
        onSuccess: (T) -> Unit,
        onError: (Error) -> Unit
    ): Job = scope.launch {
        flow.collect { r ->
            when (r) {
                is Result.Loading -> onLoading()
                is Result.Success -> onSuccess(r.data)
                is Result.Error   -> onError(r.error)
            }
        }
    }
    fun cancelAll() { scope.coroutineContext.cancelChildren() }
}
```

Why Kotlin: Swift cannot call `Flow.collect` (suspend, not bridged). This is the single, generic bridge — no per-use-case wrappers, and it fixes the un-cancellable scopes in the old `NoteRepositoryHelper`.

---

## Step 2 — native TaskCode (Swift, presentation-only)

**New file:** `iosApp/iosApp/view/screens/base/TaskCode.swift`
Mirror of Android's `TaskCode`/`AUTH` — stays native (presentation concern).

```swift
protocol TaskCode {}
enum AuthTask: TaskCode { case login, signup }
// later: enum NoteTask: TaskCode { case getNotes, insert, delete, read }, etc.
```

---

## Step 3 — native UI state (Swift)

**New file:** `iosApp/iosApp/view/screens/base/BaseUIState.swift`

```swift
protocol BaseUIState { var isLoading: Bool { get } var error: String? { get } var successMessage: String? { get } }
```

**New file:** `iosApp/iosApp/view/screens/signup/SignupUIState.swift` (mirror Android `SignupUIState`):

```swift
struct SignupUIState: BaseUIState {
    var isLoading: Bool = false
    var error: String? = nil
    var successMessage: String? = nil
    var isRegistered: Bool = false
    var imageUrl: String? = nil
}
```

---

## Step 4 — new iOS `BaseViewModel` (the makeAWish engine)

**New file:** `iosApp/iosApp/view/screens/base/BaseViewModel.swift` — **new class, do NOT edit the existing repo-based one yet** (rename the old one to `LegacyBaseHandler` or leave it; it keeps un-migrated screens compiling). This new base:

- is an `ObservableObject`,
- owns a `UseCaseObserver` and cancels it in `deinit`,
- exposes `makeAWish`, and the overridable contract `onLoading/onSuccess/onFailure/clearError`,
- provides the default `onFailure` that logs out on `UNAUTHORIZED` (mirrors Android).

Shape:

```swift
class BaseViewModel: ObservableObject {
    private let observer = UseCaseObserver()

    // subclasses override these
    func onLoading(_ code: TaskCode) {}
    func onSuccess(_ code: TaskCode, _ data: Any?) {}
    func onFailure(_ code: TaskCode, _ error: NetworkError?) {
        if error == NetworkError.unauthorized { Task { await logoutUserForcefully() } }
    }
    func clearError() {}
    func logoutUserForcefully() async {}

    // Android makeAWish equivalent: takes a use-case that returns a Flow
    func makeAWish(_ code: TaskCode, showLoader: Bool = true,
                   call: @escaping () async throws -> AnyObject?) {
        Task { @MainActor in
            guard let flow = try? await call() as? Kotlinx_coroutines_coreFlow else {
                self.onFailure(code, NetworkError.unknown); return
            }
            _ = observer.observe(flow,
                onLoading: { if showLoader { self.onLoading(code) } },
                onSuccess: { data in self.onSuccess(code, data) },
                onError:   { err in self.onFailure(code, err as? NetworkError) })
        }
    }

    deinit { observer.cancelAll() }
}
```

(Interop note: the use-case's `invoke` is `suspend` and returns a `Flow`, so Swift `await`s it to get the `Flow`, then hands it to `observe`. `Kotlinx_coroutines_coreFlow` is the generated Swift type for `Flow`. The generic `T` erases to `Any?` across the bridge — the concrete VM casts it.)

---

## Step 5 — `RegisterViewModel` (Swift, replaces `SignUpHandler`)

**New file:** `iosApp/iosApp/view/screens/signup/RegisterViewModel.swift`

- `: BaseViewModel`
- `@Published private(set) var uiState = SignupUIState()`
- holds `private let signUseCase = KoinHelper().getSignUseCase()`
- `registerUser(name:email:phone:password:confirm:)` → validate via `FieldValidationKt.checkRegisterFieldsValidity`; on `NONE`, `makeAWish(AuthTask.signup) { try await signUseCase.invoke(user: req) }`; else set `uiState.error`.
- override `onLoading` → `uiState.isLoading = true`
- override `onSuccess(.signup, data)` → cast `data as? BaseResponse<User>`; `uiState.isRegistered = resp.isSuccessful; uiState.successMessage = resp.message; uiState.isLoading = false`
- override `onFailure` → `uiState.error = (error?.getError()); uiState.isLoading = false` (call `super.onFailure` first to keep the UNAUTHORIZED→logout behavior)
- override `clearError` → clear error/loading

This is the exact mirror of Android `RegisterViewModel`.

---

## Step 6 — `SignupView` (SwiftUI) uses the VM

**File:** `iosApp/iosApp/view/screens/signup/SignupView.swift`

- Replace `private let signupHandle = SignUpHandler()` with `@StateObject private var vm = RegisterViewModel()`.
- `signUpHandle()` → `vm.registerUser(...)` (no local `Task`/`apiHandler`).
- Drive UI from `vm.uiState`:
  - `if vm.uiState.isLoading { LoadingUI() … }`
  - error alert bound to `vm.uiState.error != nil` (map to the existing `ErrorField`, or add an `.alert` on a derived binding), with an action that calls `vm.clearError()`.
  - navigation: `.onChange(of: vm.uiState.isRegistered) { if $0 { dismiss() } }` (replaces the current unconditional `dismiss()` bug — it currently dismisses even on failure).
- Delete `SignUpHandler` from this file.

---

## Step 7 — verify

- Build iOS framework + app.
- Run signup: invalid fields → inline error, no navigation; valid → loader shows, success dismisses; server error → alert, stays on screen.
- Android untouched — confirm it still builds/runs.

---

## After Signup (per-screen repeat)

For Login, Notes, NoteEditor, Tags — reuse Steps 1–4 (already built). Each screen only needs:

1. Koin getter for its use-case(s) (Step 0).
2. A `TaskCode` case (Step 2) + a `…UIState` struct (Step 3).
3. A `…ViewModel : BaseViewModel` (Step 5) + view wiring (Step 6).

Observer-style flows (`notesState`, `tagState`, `selectedNoteMediaContent`) will add one method on `UseCaseObserver` that keeps the returned `Job` alive (non-terminal) and cancels in `deinit` — handled when we reach the Notes screen.

## Final cleanup (after all screens migrated)

- Delete the old repo-based `BaseViewModel`/`SignUpHandler`/`LoginHandler`, `apiHandler` in `BaseNetworkHandler.swift` (or keep `BaseResult` only if still used), and `NoteRepositoryHelper.kt`.
- Remove `getBaseRepository()/getNoteRepository()/getNoteContentRepository()` from `KoinHelper` so repos are no longer exposed to the UI.
- Grep iOS for `Repositary` / `get…Repository` → expect zero hits.

> Nothing above is implemented. Confirm the file layout/names and we'll execute Step 0 → Step 4 (the machinery), then wire Signup (Steps 5–6).
