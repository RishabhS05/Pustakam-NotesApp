# Module Decoupling — Findings & Execution Plan

**Date:** 04-Aug-2026 · **Platforms:** F (shared Kotlin) · I (iOS) · A (Android) · LBD (local DB)
**Branch audited:** `NoteBookReader` (`9c85a1e`)
**Companion:** `.ai/architecture.md`, `.ai/dependency-graph.md`, `.ai/code-review-graph.md`

> **Purpose of this document.** Every finding below carries its **file and line**, so the plan can be
> executed without re-reading the codebase. §10 is a context appendix — the facts a future session
> needs, in one place.

---

## 1. Verdict — the diagnosis is right, but not where you'd think

**The module graph is fine.** It is acyclic, edges point downward, there are no sideways feature
edges, and `settings.gradle.kts` matches the folder tree. The 29-Jul restructure did its job.

**The coupling is at the type level.** Modules are wired to each other's *concrete classes* rather
than to abstractions, and `:shared` re-exports every implementation to everyone. So:

```
  WHAT THE BUILD FILES SAY              WHAT THE CODE ACTUALLY DOES
  ────────────────────────              ───────────────────────────
  :feature:notes ──► :core:data         NoteBaseUseCase ──► NoteRepository (concrete impl)
  :androidApp    ──► :shared            SettingsViewModel ──► BasePreferences (concrete, :core:database)
                                        PlayerViewModel ──► NoteContentRepository (concrete impl)
                                        AppViewModel ──► BaseRepository (concrete)
                                        BaseNetworkHandler.swift ──► NoteRepository (concrete, from Swift)
```

**This means the fix is NOT another module split.** Re-cutting modules would cost weeks and change
nothing, because the leaks are `get<ConcreteClass>()` calls, not Gradle edges. The fix is
**introduce the abstractions, then seal the exports.**

Interfaces already exist for much of this and are simply **not used as the injected type**:
`ILocalNotesRepository`, `IRemoteNoteRepository`, `IAppPreferences` are all written, implemented, and
bypassed.

---

## 2. The root cause, in one line

`feature/notes/…/domain/usecase/NoteBaseUseCase.kt:8`

```kotlin
protected val noteRepository = repository as NoteRepository   // ← unchecked downcast to an impl
```

The domain layer casts its way into the data layer's implementation class. Everything in §3 is
either a cause or a consequence of that line.

---

## 3. Findings

### F1 🔴 Use cases depend on the concrete repository, not on the interfaces that already exist

| | |
|---|---|
| **Where** | `feature/notes/…/domain/usecase/NoteBaseUseCase.kt:8,13-14` |
| **Evidence** | `protected val noteRepository = repository as NoteRepository`<br>`override fun setRepository(): NoteRepository { return get<NoteRepository>() }` |
| **Blast radius** | all **16** notes use cases (`NoteUseCase.kt` ×10, `NoteContentUseCase.kt` ×6) |

`ILocalNotesRepository` and `IRemoteNoteRepository` exist in `domain/repository/` and are already
implemented by `NoteRepository` — they are simply never injected. The cast is also unchecked: change
the Koin binding and it becomes a `ClassCastException` at runtime, not a compile error.

**Complexity: MEDIUM.** Mechanical but wide. **Risk: MEDIUM** — touches every notes use case.

---

### F2 🔴 The domain layer re-publishes the data layer's mutable state

| | |
|---|---|
| **Where** | `NoteBaseUseCase.kt:9,10,12`, `NoteContentUseCase.kt:13` |
| **Evidence** | `val notes = noteRepository.notesState`<br>`val tags = noteRepository.tagState`<br>`val noteSummaries = noteRepository.noteSummariesState`<br>`val selectedMediaContent get() = noteContentRepository.selectedNoteMediaContent` |

Use cases are pass-through holes onto `NoteRepository`'s `MutableStateFlow`s
(`NoteRepository.kt:37,38,42`; `NoteContentRepository.kt:17`). The presentation layer observes data-layer
state through a domain-layer alias, so the "layer boundary" is decorative.

**Complexity: MEDIUM.** Needs a domain-owned observable contract. **Risk: MEDIUM** — this is the
live refresh path for the notes list on both platforms; break it and lists stop updating.

---

### F3 🟠 ViewModels inject repositories directly, skipping the domain layer

| File | Line | Violation |
|---|---|---|
| `androidApp/…/hardware/audio/player/PlayerViewModel.kt` | 67, 83 | `get<NoteContentRepository>()`, then collects `selectedNoteMediaContent` |
| `androidApp/…/screen/noteEditor/NoteEditorViewModel.kt` | 49 | `get<NoteContentRepository>()` |
| `androidApp/…/screen/AppViewModel.kt` | 12 | `get<BaseRepository>()` |

Three ViewModels reach past the use cases into `data/repositoryImpl/`.

**Complexity: SMALL** — 3 files, and use cases for this already exist
(`SetSelectedNoteContentUseCase`, `UpdateSelectedMediaContentUseCase`, `GetSelectedMediaIndexUseCase`).
**Risk: LOW.**

---

### F4 🟠 ViewModels inject `:core:database`'s concrete preferences class

| File | Line |
|---|---|
| `screen/settings/SettingsViewModel.kt` | 48 — `get<BasePreferences>()` |
| `screen/bookReading/BookReaderViewModel.kt` | 34 — `by inject<BasePreferences>()` |
| `screen/notebookReader/NoteBookReaderViewModel.kt` | 61 — `by inject<BasePreferences>()` |

Plus `MainActivity.kt` and `navigation/AppNavGraph.kt` import the same package — **5 androidApp files
importing `core.database.localdb.preferences`.**

`IAppPreferences` exists (`IAppPrefrences.kt`) and is bound at `DatabaseModule.kt:37`
(`single<IAppPreferences> { get<BasePreferences>() }`), and is used at **exactly one** injection site.
The abstraction was built and then ignored.

Note: `IAppPreferences` currently exposes only **writes** (`setToken`, `setThemeMode`,
`setReadingMode`, `clear`…). The read-side flows the ViewModels need are on the concrete class, which
is *why* everyone injects the concrete class. The interface is incomplete, not merely unused.

**Complexity: SMALL.** **Risk: LOW.**

---

### F5 🟠 `:core:data`'s base contracts are hollow

`core/data/…/base/ILocalRepository.kt` — the entire file:

```kotlin
package com.app.pustakam.core.data.base

interface ILocalRepository
```

An empty marker interface. `IRemoteRepository` covers only auth/user. So
`BaseRepository : IRemoteRepository, ILocalRepository` declares nothing meaningful, and
`BaseRepository` itself injects three concrete classes (`BaseRepository.kt:23-25`):

```kotlin
protected val apiClient by inject<ApiCallClient>()      // concrete, :core:network
protected val notesDao  by inject<NotesDao>()           // concrete, :core:database
protected val userPrefs = get<BasePreferences>()        // concrete, :core:database
```

**Complexity: SMALL.** **Risk: LOW.**

---

### F6 🔴 `:shared` re-exports every implementation — nothing is encapsulated

`shared/build.gradle.kts` has 8 × `api()` and 8 × `export()`. `:androidApp` declares exactly one
dependency (`implementation(projects.shared)`) and thereby transitively sees `NotesDao`,
`ApiCallClient`, `NoteRepository`, `NoteContentRepository`, `BasePreferences`, and the SQLDelight
generated types.

**Nothing prevents a Compose screen from calling the DAO. Only convention does.** That is the
mechanism by which F3 and F4 happened.

> **Important constraint:** this cannot be fixed by flipping `api()` → `implementation()`.
> `export()` to Swift **only works on an `api()` dependency** (`.ai/dependency-graph.md` §4). Removing
> the `api()` breaks the iOS framework. The fix must be a **facade**, not a visibility change.

**Complexity: LARGE.** **Risk: HIGH** — touches the build and the iOS export surface.

---

### F7 🟠 iOS breaks its own bridge seam

| Where | Violation |
|---|---|
| `iosApp/…/network/BaseNetworkHandler.swift:24` | `var noteRepositary: NoteRepository { get set }` — a Swift protocol exposing the **concrete Kotlin repository** |
| `feature/notes/src/iosMain/…/NoteRepositoryHelper.kt:27-28` | `get<NoteRepository>()` + `get<NoteContentRepository>()` — **confirmed dead code**, no live references |
| `iosApp/…/file/FileImportService.swift`, `…/noteEditor/NoteEditorViewModel.swift` | reach into `:core:filesys` (`PathPolicy`, `MimeCatalog`, `FileNameGenerator`) directly |

The bridge design is correct and the 29-Jul audit called it "already a clean seam." Two legacy holes
remain. The `:core:filesys` access is arguably fine (it is a stateless policy module), but it should
be a **deliberate decision**, not an accident.

**Complexity: SMALL.** **Risk: LOW** — the dead file has no callers.

---

### F8 🟡 No navigation contract — the graph knows every feature

- **Android:** `screen/navigation/AppNavGraph.kt` imports `core.data` **and**
  `core.database.localdb.preferences` — navigation reads auth state from the database module to pick
  a start destination.
- **iOS:** `Router.Destination` carries a `Note?` payload and a `(CapturedMedia?) -> Void` closure, so
  navigation is coupled to both model types and the camera.

Adding a feature means editing the central graph on both platforms.

**Complexity: LARGE.** **Risk: HIGH** — navigation rewrites regress in ways tests don't catch.

---

### F9 🟡 No event channel between features

Cross-feature signals ("a note was saved → the list should refresh") travel by **sharing the
repository singleton's StateFlow**. That is precisely why F1–F3 exist: the only way to observe
another feature's change is to hold its repository.

Also: **22 androidApp files import `core.filesys`** directly.

**Complexity: MEDIUM.** **Risk: MEDIUM.**

---

## 4. Summary table

| # | Finding | Sev | Complexity | Risk | Files | Blocks |
|---|---|---|---|---|---|---|
| F1 | Use cases → concrete repo (downcast) | 🔴 | M | Med | 3 + 16 use cases | F2, F4 |
| F2 | Domain re-publishes data state | 🔴 | M | Med | 4 | — |
| F3 | VMs inject repositories | 🟠 | S | Low | 3 | — |
| F4 | VMs inject `BasePreferences` | 🟠 | S | Low | 5 | — |
| F5 | Hollow base interfaces | 🟠 | S | Low | 3 | F1 |
| F6 | `:shared` exports everything | 🔴 | L | High | build + all | — |
| F7 | iOS seam holes | 🟠 | S | Low | 3 | — |
| F8 | No navigation contract | 🟡 | L | High | ~8 | — |
| F9 | No event channel | 🟡 | M | Med | ~6 | — |

**Honest read: F1 + F5 + F3 + F4 + F7 are ~70% of the benefit for ~20% of the risk.** They are all
mechanical, all independently shippable, and none of them changes behaviour. F6, F8, F9 are real
architecture projects and should be decided separately, not bundled.

---

## 5. Target architecture

```
  ┌──────────────────────────────────────────────────────────────────┐
  │ PRESENTATION   Compose VM / SwiftUI VM                            │
  │   may see: use cases, domain models, IAppPreferences              │
  │   may NOT see: repositories, DAO, ApiCallClient, BasePreferences  │
  └──────────────────────────┬───────────────────────────────────────┘
                             │ use cases only
  ┌──────────────────────────▼───────────────────────────────────────┐
  │ DOMAIN   use cases + repository INTERFACES + domain-owned flows   │
  │   may NOT see: any *Impl class                                    │
  └──────────────────────────┬───────────────────────────────────────┘
                             │ interfaces only, bound by Koin
  ┌──────────────────────────▼───────────────────────────────────────┐
  │ DATA   NoteRepository : ILocalNotesRepository, IRemoteNoteRepo    │
  │        internal wherever the language allows                      │
  └──────────────────────────┬───────────────────────────────────────┘
  ┌──────────────────────────▼───────────────────────────────────────┐
  │ SOURCES   NotesDao · ApiCallClient · BasePreferences · FileSystem │
  └──────────────────────────────────────────────────────────────────┘
```

**The one rule to enforce:** *a module may name another module's **interfaces** and **models**, never
its **implementations**. Implementations are reachable only through Koin, by their interface type.*

---

## 6. Execution plan

Each phase is **independently shippable** and leaves the app working. Do them in order — later
phases assume earlier ones.

### Phase 0 — Guardrails · ~0.5 day · no behaviour change

1. Fill in `ILocalRepository` with the real local-side contract; extend `IRemoteRepository`.
2. Extend `IAppPreferences` with the **read** side (the flows ViewModels actually need) — this is the
   prerequisite that makes F4 fixable.
3. Delete the dead `feature/notes/src/iosMain/…/NoteRepositoryHelper.kt` — **ask first** (decision G1).
4. Add the §5 rule + the `get<ConcreteClass>()` red flag to `.ai/code-review-graph.md`.

**Verify:** `./gradlew build` + framework link. Nothing else should change.

---

### Phase 1 — Domain depends on interfaces · ~2–3 days · **highest value**

Fixes **F1, F5**.

1. `NoteBaseUseCase` holds `ILocalNotesRepository` + `IRemoteNoteRepository`, injected separately.
2. **Delete the `as NoteRepository` downcast** (`NoteBaseUseCase.kt:8`).
3. `NotesModule.kt` binds the interfaces:
   ```kotlin
   single { NoteRepository() }
   single<ILocalNotesRepository>  { get<NoteRepository>() }
   single<IRemoteNoteRepository>  { get<NoteRepository>() }
   single<BaseRepository>         { get<NoteRepository>() }   // keep — BaseUseCase still needs it
   ```
4. Any method a use case calls that is **not** on an interface → add it to the interface, or it was
   never domain API.

**Watch out:** `NoteRepository` has public methods not on either interface (`insertNotes`,
`insertTag`, `createNewEmptyNote`). Each is a decision: promote to the interface, or make internal.

**Verify:** all 16 use cases compile with no cast; app behaviour identical; framework links.

---

### Phase 2 — Domain owns its observable state · ~2 days

Fixes **F2**.

1. Add the read-side flows to `ILocalNotesRepository` (`notesState`, `noteSummariesState`,
   `tagState`) so the domain names them as contract, not as an impl field.
2. Use cases expose **their own** narrowed flows rather than aliasing the repository's.

**Watch out:** this is the live-refresh path for both platforms' note lists and for
`NotesBridge`'s reactive section (`NotesBridge.kt:169+`). Test list refresh after save/delete on
**both** platforms before merging.

---

### Phase 3 — Seal the presentation layer · ~1–2 days

Fixes **F3, F4, F7**.

1. `PlayerViewModel:67`, `NoteEditorViewModel:49` → use the three existing note-content use cases.
2. `AppViewModel:12` → an auth use case instead of `BaseRepository`.
3. `SettingsViewModel:48`, `BookReaderViewModel:34`, `NoteBookReaderViewModel:61` → `IAppPreferences`
   (needs Phase 0 step 2).
4. `MainActivity` + `AppNavGraph` → stop importing the preferences package.
5. iOS: `BaseNetworkHandler.swift:24` → drop `NoteRepository` from the protocol; go through the bridge.

**Verify after this phase:**
```bash
grep -rn "get<Note.*Repository>\|inject<BasePreferences>\|get<BasePreferences>" androidApp/src   # → 0
grep -rln "core.database.localdb" androidApp/src                                                 # → 0
```
Those two commands become the regression test for the whole effort.

---

### Phase 4 — Facade / export surface · ~3–5 days · **DECIDE BEFORE STARTING**

Fixes **F6**. This is the only phase that makes the boundary *impossible* to cross rather than merely
discouraged.

Two options — pick one:

**4a. Facade object per feature (cheaper).** `:feature:notes` exposes one `NotesFacade` with the
public API; repositories become `internal`. `:shared` keeps exporting the module, but the module's own
surface is small. ~2–3 days, low risk, does not touch the build.

**4b. Split `:feature:notes` into `:feature:notes:api` + `:feature:notes:impl` (stricter).**
`:shared` exports only `:api`. Genuinely enforced by the compiler. ~4–5 days, touches
`settings.gradle.kts`, `:shared` `api()`/`export()`, and Koin wiring — **and every mistake here fails
silently in Xcode, not in Gradle.**

> Recommendation: **4a first.** It gets most of the benefit at a fraction of the risk, and it makes 4b
> straightforward later if you still want it. `internal` in Kotlin is module-scoped, so 4a genuinely
> hides implementations from other Gradle modules.

---

### Phase 5 — Navigation contract · ~3–5 days · optional

Fixes **F8**. Android: a route registry each feature contributes to, so `AppNavGraph` stops importing
`core.data`/preferences. iOS: `Router.Destination` carries **ids**, not `Note?` and not closures.

> Do this only when a third feature module is actually coming. With two features it is cost without
> benefit.

---

### Phase 6 — Event channel · ~2–3 days · optional

Fixes **F9**. A small typed `DomainEvent` bus in `:core:common` (`NoteSaved`, `NoteDeleted`,
`ContentImported`) so features stop sharing repository singletons to hear about each other.

> **Only worth it after Phases 1–3.** If the domain owns its flows, most of the need evaporates — and
> an event bus added *before* fixing F1/F2 becomes a second coupling mechanism alongside the first.

---

## 7. Suggested sequencing

| Sprint | Phases | Outcome |
|---|---|---|
| 1 | 0 + 1 | Domain no longer names any implementation. The downcast is gone. |
| 2 | 2 + 3 | ViewModels see only use cases. The two grep checks return 0. |
| 3 | 4a | Implementations are `internal` — the boundary is compiler-enforced. |
| later | 4b / 5 / 6 | Only when a third feature or a real need arrives. |

**Stopping after Sprint 2 is a legitimate outcome.** The tight coupling you described is gone at that
point; Phases 4–6 are hardening.

---

## 8. Guardrails — what NOT to change

These are settled (`.ai/project-decisions.md`) and this work must not touch them:

- **A1** — no shared UI, no Compose Multiplatform, no ViewModels in shared. iOS stays hand-written Swift.
- **D1** — bridges remain the *only* iOS seam. Decoupling must **narrow** that seam, never widen it.
- **B2** — `ContentType` stays in `:core:common`; moving it is a cycle.
- **E2** — `:core:filesys` keeps its two-edge diet; don't add dependencies to it.
- **G1** — nothing is deleted without asking. Keep old entry points as delegates.
- **G5** — a refactor proves equivalence; it doesn't assert it.
- **B6** — do not mix a Kotlin upgrade into this.

---

## 9. Verification per phase

```bash
cd Pustakam/Pustakm
./gradlew build
./gradlew :core:filesys:allTests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # THE iOS check — a build pass is not enough
./gradlew :androidApp:assembleDebug
```
Then Xcode → Clean Build Folder → Build.

**Behavioural smoke test after every phase** (both platforms): create a note → add text + image +
audio → leave the editor → confirm the list refreshes → reopen → delete a block → search → open a PDF
→ toggle reading mode → change theme → log out.

The list-refresh step is the one that catches Phase 2 regressions, and it fails **silently**.

---

## 10. Context appendix — so the code needn't be re-read

**Module graph:** acyclic and correct. Not the problem. See `.ai/dependency-graph.md`.

**Interfaces that already exist and are bypassed:**

| Interface | File | Used as injection type? |
|---|---|---|
| `ILocalNotesRepository` | `feature/notes/…/domain/repository/` | ❌ never |
| `IRemoteNoteRepository` | same | ❌ never |
| `IAppPreferences` | `core/database/…/preferences/IAppPrefrences.kt` | ⚠️ 1 site; **write-only, no read side** |
| `ILocalRepository` | `core/data/…/base/` | ⚠️ **empty interface** |
| `IRemoteRepository` | `core/data/…/base/` | ⚠️ auth/user only |

**Every concrete-injection site in the codebase** (the full list — this is the work):

```
androidApp/…/hardware/audio/player/PlayerViewModel.kt:67     get<NoteContentRepository>()
androidApp/…/screen/noteEditor/NoteEditorViewModel.kt:49     get<NoteContentRepository>()
androidApp/…/screen/settings/SettingsViewModel.kt:48         get<BasePreferences>()
androidApp/…/screen/bookReading/BookReaderViewModel.kt:34    inject<BasePreferences>()
androidApp/…/screen/notebookReader/NoteBookReaderViewModel.kt:61  inject<BasePreferences>()
androidApp/…/screen/AppViewModel.kt:12                       get<BaseRepository>()
core/data/…/base/BaseRepository.kt:23,24,25                  inject<ApiCallClient>, inject<NotesDao>, get<BasePreferences>
core/data/…/usecases/BaseUseCase.kt:25                       get<BaseRepository>()        ← acceptable (interface-ish)
feature/notes/…/usecase/NoteBaseUseCase.kt:8,14              as NoteRepository / get<NoteRepository>()   ← ROOT CAUSE
feature/auth/…/bridge/AuthBridge.kt:69                       get<BasePreferences>()
feature/auth/…/UserPreferenceViewModel.kt:13                 get<BasePreferences>()
feature/notes/…/bridge/ReaderPrefsBridge.kt:30               get<BasePreferences>()
feature/notes/…/NoteRepositoryHelper.kt:27,28                get<NoteRepository>(), get<NoteContentRepository>()  ← DEAD FILE
iosApp/…/network/BaseNetworkHandler.swift:24                 var noteRepositary: NoteRepository
```

**Repository state exposed as public flows:**
`NoteRepository.kt:37` `notesState` · `:38` `tagState` · `:42` `noteSummariesState` ·
`NoteContentRepository.kt:17` `selectedNoteMediaContent`

**Sizing:** 130 Android `.kt` · 14 ViewModels · 107 Swift · 16 notes use cases · 6 auth use cases ·
5 androidApp files importing DB preferences · 22 importing `core.filesys` · 0 importing `NotesDao` or
`core.network` (those two boundaries are already clean).

**Dead code confirmed:** `NoteRepositoryHelper.kt` — replaced by `NotesBridge`'s reactive section
(`NotesBridge.kt:169+`); the only remaining mentions are comments in `NotesBridgeAdapter.swift:16` and
`MediaManager.swift:25`.

---

## 11. Open questions for the owner

1. **Phase 4a or 4b** — facade object, or an `:api`/`:impl` module split? (Recommendation: 4a.)
2. **Phases 5 & 6** — worth doing now, or defer until a third feature module exists?
3. **`:core:filesys` from the UI** (22 Android files, 2 Swift) — deliberate and fine, or should it go
   behind use cases too?
4. **Delete `NoteRepositoryHelper.kt`?** Dead, but G1 says ask.
5. **Stop after Sprint 2?** That already removes the coupling you described.
