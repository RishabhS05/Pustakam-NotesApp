# A5 — Shared Use-Case Layer Migration Plan

> **Status:** Plan only. **No code changed.** Reviewed against `master`.
> **Goal:** Move the domain use-cases into `shared/commonMain`. After this, the UI layer (Android ViewModels + iOS Swift ViewModels/handlers) talks **only to use-cases** — repositories are no longer referenced by the UI on either platform. KMM stays: native SwiftUI/Compose UI, shared Kotlin domain.
> **Why now:** the use-cases are currently thin, but new features will add real domain logic; centralizing before that avoids two divergent copies later.

## Target architecture

```
Android Compose VM ─┐                        ┌─ NoteRepository
                    ├─► shared UseCase ─────►│
iOS Swift VM ───────┘   (suspend Result)     └─ BaseRepository / NoteContentRepository
```

- **Command/query use-cases** return `suspend ... Result<BaseResponse<T>, Error>` — bridges cleanly to Swift `async/await` and to Android's `getBaseApiCall { }` Loading wrapper.
- **Observer use-cases** wrap the repo `StateFlow`s and expose **both** `val stream: StateFlow<T>` (Android collects natively) **and** `fun watch(onEach): Job` (iOS callback bridge — Flow doesn't auto-bridge to Swift). `watch` returns the `Job` so Swift can cancel it (fixes the current un-cancellable `NoteRepositoryHelper` scopes).
- Repos remain Koin singletons but the UI never injects a repo type again.

---

## Phase 0 — New shared files to CREATE

All under `shared/src/commonMain/kotlin/com/app/pustakam/domain/usecase/`.

| File | Contents |
|------|----------|
| `base/FlowWatcher.kt` | `fun <T> Flow<T>.watch(scope: CoroutineScope, onEach: (T) -> Unit): Job` — one cancellable collector helper reused by every Observe use-case. |
| `auth/LoginUseCase.kt` | `suspend operator fun invoke(login: Login): Result<BaseResponse<User>, Error> = repo.loginUser(login)` (inject `BaseRepository`). |
| `auth/RegisterUseCase.kt` | `invoke(user: RegisterReq) = repo.registerUser(user)`. |
| `auth/LogoutUseCase.kt` | `suspend operator fun invoke() = repo.userLogout()`. |
| `auth/ObserveAuthStateUseCase.kt` | `val stream = repo._userAuthState` (Flow<UserPreference>) + `fun watch(cb): Job`. |
| `note/GetNotesUseCase.kt` | `invoke(page: Int) = repo.getAllNotes(page)` (inject `NoteRepository`). |
| `note/ObserveNotesUseCase.kt` | `val stream = repo.notesState` + `watch(cb)`. |
| `note/ReadNoteUseCase.kt` | `invoke(id: String?) = repo.getANote(id)`. |
| `note/CreateOrUpdateNoteUseCase.kt` | `invoke(note: Note) = repo.insertOrUpdateNote(note)`. |
| `note/DeleteNoteUseCase.kt` | `invoke(id: String?) = repo.deleteNote(id ?: "")`. |
| `note/DeleteNoteContentUseCase.kt` | `invoke(id: String?) = repo.deleteNoteContentFromDb(id ?: "")`. |
| `tag/GetTagsUseCase.kt` | `invoke() = repo.getTagsFromDB()`. |
| `tag/ObserveTagsUseCase.kt` | `val stream = repo.tagState` + `watch(cb)`. |
| `tag/CreateTagUseCase.kt` | `invoke(tag: Tag) = repo.createTagOnDB(tag)`. |
| `tag/UpdateTagUseCase.kt` | `invoke(tag: Tag) = repo.updateTagOnDB(tag)`. |
| `tag/DeleteTagUseCase.kt` | `invoke(tagId: String?) = repo.deleteTagOnDB(tagId ?: "")`. |
| `notecontent/NoteContentUseCase.kt` | Facade over `NoteContentRepository`: `addAll(note)`, `update(content)`, `clear()`, `indexOf(id)`, `val stream = repo.selectedNoteMediaContent` + `watch(cb)`. |
| `di/useCasesModule.kt` | Koin `module { factory { LoginUseCase() } … }` for every use-case above. |

Notes:
- Each use-case is a `KoinComponent` that `by inject`s the repo it needs — so the constructors stay empty and Swift/Android can `get<…>()` them.
- `factory` (not `single`) for command use-cases; observer use-cases can be `single` if you want one shared collector, but `factory` is fine since they wrap a repo singleton's flow.

---

## Phase 1 — Shared files to MODIFY

| File | Change |
|------|--------|
| `koinDI/Koin.kt` | Add `useCasesModule()` to the `modules(...)` list. |
| `iosMain/.../koinDI/KoinHelper.kt` | **Remove** `getBaseRepository()`, `getNoteRepository()`, `getNoteContentRepository()`. **Add** getters for the use-cases iOS needs (e.g. `fun loginUseCase() = get<LoginUseCase>()`, etc.), or a single `inline fun <reified T> useCase(): T = get()`. |
| `iosMain/.../domain/NoteRepositoryHelper.kt` | **Delete** (or gut). Its three helpers (`noteListStateHelper`, `getTagsHelper`, `noteContentMediaList`) are replaced by `ObserveNotesUseCase.watch`, `ObserveTagsUseCase.watch`, `NoteContentUseCase.watch`. This also removes the current un-cancellable `CoroutineScope(Dispatchers.Main)` leak. |

Repos (`BaseRepository`, `NoteRepository`, `NoteContentRepository`) themselves: **no change** — still registered, now consumed only by use-cases.

---

## Phase 2 — Android files to MODIFY

Android already follows UI → UseCase, so this is mostly repointing to the shared use-cases and deleting the local copies.

**Delete (local use-cases, now in shared):**

- `androidApp/.../screen/notes/NoteUseCase.kt` (all note + tag use-cases)
- `androidApp/.../screen/login/LoginUseCase.kt`
- `androidApp/.../screen/signup/SignupUseCase.kt`
- `androidApp/.../screen/base/BaseUseCase.kt` (`BaseUseCase` / `NoteBaseUseCase`) — see note below.

**Keep the Loading wrapper:** move `getBaseApiCall { … }` out of the deleted `BaseUseCase` into Android's `screen/base/BaseViewModel.kt` (it's a UI-loading concern). VMs then do `makeAWish(code) { getBaseApiCall { sharedUseCase(x) } }`.

| ViewModel | Current | After |
|-----------|---------|-------|
| `AppViewModel.kt` | `get<BaseRepository>()._userAuthState` | inject `ObserveAuthStateUseCase`; `authState = it.stream`. **No repo reference.** |
| `login/LoginViewModel.kt` | `LoginUseCase()` (local) + `logoutUser()` | `get<LoginUseCase>()` (shared) wrapped in `getBaseApiCall`; logout via `LogoutUseCase`. |
| `signup/SignupViewModel.kt` | `SignUseCase()` | `get<RegisterUseCase>()`. |
| `notes/list/NotesViewModel.kt` | `GetNotesUseCase()`; `.notes.collect`; `logoutUser()` | `get<GetNotesUseCase>()` + `get<ObserveNotesUseCase>().stream.collect`; `LogoutUseCase`. |
| `noteEditor/NoteEditorViewModel.kt` | `ReadNoteUseCase`, `DeleteNoteUseCase`, `DeleteNoteContentUseCase`, `CreateORUpdateNoteUseCase` + `get<NoteContentRepository>()` (lines 43, 87, 238, 291) | shared equivalents + `get<NoteContentUseCase>()` for `addAll/update/clear`. **Removes the direct `NoteContentRepository` reference.** |
| `features/tags/TagViewModel.kt` | `GetTagCase`, `CreateTagUseCase`, `UpdateTagUseCase` | shared `GetTagsUseCase`, `CreateTagUseCase`, `UpdateTagUseCase` (rename `GetTagCase → GetTagsUseCase`). |
| `settings/SettingsViewModel.kt` | (no repo/use-case) | no change. |

`makeAWish` in `BaseViewModel.kt` is unchanged (still takes `suspend () -> Flow<Result<…>>`); only the *source* of that Flow changes to `getBaseApiCall { sharedUseCase() }`.

---

## Phase 3 — iOS files to MODIFY

iOS currently calls repos directly. Switch each call to the matching shared use-case.

| File | Current repo call | After (use-case) |
|------|-------------------|------------------|
| `view/screens/base/BaseViewModel.swift` | holds `baseRepositary`, `noteRepositary`, `noteContentRepository` from `KoinHelper` | hold the **use-cases** the subclasses need (or expose a `useCase()` accessor). Remove the three repo properties. |
| `network/BaseNetworkHandler.swift` (`IBaseHandler`) | protocol declares the 3 repo properties | replace with use-case properties/accessor; `apiHandler` stays as-is. |
| `view/screens/login/LoginView.swift` | `baseRepositary.loginUser(login:)` | `loginUseCase.invoke(login:)`. |
| `view/screens/signup/SignupView.swift` | `baseRepositary.registerUser(user:)` | `registerUseCase.invoke(user:)`. |
| `view/screens/notes/noteEditor/NoteEditorViewModel.swift` | `noteRepositary.getANote/insertOrUpdateNote/deleteNote`, `noteContentRepository.addAllNoteContent/updateNoteContent` | `readNoteUseCase`, `createOrUpdateNoteUseCase`, `deleteNoteUseCase`, `noteContentUseCase.addAll/update`. |
| `view/screens/notes/noteList/NotesViewModel.swift` | `noteRepositary.getAllNotes/createTagOnDB`; `NoteRepositoryHelper().noteListStateHelper/getTagsHelper` | `getNotesUseCase`, `createTagUseCase`; `observeNotesUseCase.watch{}`, `observeTagsUseCase.watch{}` (keep the returned `Job` to cancel in `deinit`). |
| `media/MediaManager.swift` | `NoteRepositoryHelper().noteContentMediaList{}` | `noteContentUseCase.watch{}` (store `Job`, cancel on teardown). |
| `ContentView.swift` (`UserPreferenceWrapper`) | already uses `UserPreferenceViewModel` | optionally route through `ObserveAuthStateUseCase.watch{}` for consistency (optional). |

---

## Suggested execution order (vertical slices, each independently testable)

1. **Scaffolding:** create `FlowWatcher.kt`, `useCasesModule.kt`, wire into `Koin.kt`. Build only (nothing consumes them yet).
2. **Auth slice:** `LoginUseCase` + `RegisterUseCase` + `LogoutUseCase`. Repoint `LoginViewModel`/`SignupViewModel` (Android) and `LoginView`/`SignupView` (iOS). Test login/register/logout on both. Delete Android `LoginUseCase.kt`/`SignupUseCase.kt`.
3. **Notes slice:** notes use-cases + `ObserveNotesUseCase`. Repoint `NotesViewModel` + `NoteEditorViewModel` (both platforms). Test list + create/read/delete.
4. **Tags slice:** tag use-cases + `ObserveTagsUseCase`. Repoint `TagViewModel` (Android) + `NotesViewModel` (iOS).
5. **Note-content slice:** `NoteContentUseCase`. Repoint `NoteEditorViewModel` (both) + `MediaManager` (iOS). Delete direct `NoteContentRepository` usage.
6. **Cleanup:** delete Android `NoteUseCase.kt`/`BaseUseCase.kt`, delete `NoteRepositoryHelper.kt`, strip repo getters from iOS `KoinHelper`, remove repo props from iOS `BaseViewModel`/`IBaseHandler`. Grep for any lingering `get<…Repository>()` / `Repositary` in the UI to confirm repos are fully hidden.

Do **not** do all six at once — land each slice as its own commit so a break is easy to localize.

---

## Risks & gotchas

- **Flow → Swift:** observer use-cases MUST provide the `watch(cb): Job` bridge; a bare `StateFlow` isn't consumable from Swift. Store and cancel the returned `Job` in the Swift VM's `deinit` to avoid the leak that exists today.
- **Loading state:** keep `getBaseApiCall` (Android only) so `makeAWish`/loaders keep working; iOS keeps `apiHandler`. Don't push Loading into the shared use-cases.
- **`logoutUser()`** currently lives on the Android use-case base; after the move it becomes `LogoutUseCase`. Make sure `BaseViewModel.onFailure`'s `UNAUTHORIZED → logoutUserForcefully()` path calls it.
- **DI scope:** `factory` vs `single` per use-case — command use-cases `factory`; observer use-cases either works since the underlying repo/flow is a singleton.
- **Naming:** rename `GetTagCase` → `GetTagsUseCase`, `SignUseCase` → `RegisterUseCase`, `CreateORUpdateNoteUseCase` → `CreateOrUpdateNoteUseCase` while moving (fixes E2/E3 casing at the same time).
- **`ReadUserUseCase`/`UpdateUserUseCase`/`DeleteUserUseCase`** exist on Android but are unused by any VM — port them to shared only if you actually need them; otherwise drop.

> Nothing above is implemented. Confirm the plan (or adjust the file layout / naming), and we'll execute slice 1 first.
