# Multi-Module KMM Restructure — Execution Plan

**Date:** 29-Jul-2026
**Platforms:** iOS (I) · Android (A) · Local DB (LBD)
**Reference doc:** `docs/system-design/app-flow.md`
**Constraint:** zero feature regression · UI unchanged on both platforms · `import shared` keeps working

---

## 1. Current State (audited, not assumed)

| Item | Value |
|---|---|
| Gradle modules | 3 — root, `:androidApp`, `:shared` |
| Kotlin / AGP | 2.1.20 / 8.10.0 |
| Gradle features already ON | `TYPESAFE_PROJECT_ACCESSORS`, `org.gradle.caching`, `org.gradle.configuration-cache` |
| `shared/commonMain` | 57 `.kt` |
| `shared/androidMain` | 8 `.kt` (all `actual`) |
| `shared/iosMain` | 21 `.kt` (7 = bridges, 8 = `actual`) |
| `androidApp` | 105 `.kt`, 13 ViewModels, **43 files import from `shared`** |
| `iosApp` | 86 `.swift`, 3 Bridge Adapters |
| iOS framework | `baseName = "shared"`, `isStatic = true` |
| DI | single `initKoin()` in commonMain — 7 Koin modules, 22 use-case factories |

**What the code actually looks like (matters for the split):**

- `data/models/**` are `@Serializable` classes doing double duty as DTO **and** domain model. There is no mapper layer.
- `NoteRepository` / `NoteContentRepository` are **implementations living inside the `domain/` package**.
- `NoteRepository` is a Koin `single` holding `MutableStateFlow` — it is the app's in-memory source of truth, not a stateless repo.
- `BaseRepository` injects `ApiCallClient` + `NotesDao` + `BasePreferences` — it sits on top of network **and** database at once.
- iOS reaches Kotlin only through `NotesBridge` / `NoteContentBridge` / `AuthBridge` / `ReaderPrefsBridge`. Swift never touches a repository. This is already a clean seam — the restructure must not disturb it.

---

## 2. Changes / Corrections to `app-flow.md`

These are the deltas between the doc and what the code can support today.

### 2.1 Folder tree ≠ Gradle module tree — flagged

The doc's tree shows `notes/presentation`, `notes/domain`, `notes/data`, `notes/api` as siblings. If each becomes its own Gradle module you get 4 modules for one feature × 20 features = 80 modules. **Recommendation: keep the doc's folder structure verbatim, but as packages inside one Gradle module per feature.** Split into separate Gradle modules only when a real compile-time boundary is needed. Nothing in the doc is lost — the directories are identical.

### 2.2 `presentation/` in shared — defer, don't do it in this restructure

The doc's architecture diagram puts a shared MVI ViewModel under both Compose and SwiftUI. That is a good destination, but it is a **rewrite of 13 Android ViewModels and 3 Swift ViewModels**, not a file move. Mixing it into the restructure makes "no regression" unverifiable — if the app breaks you won't know whether the module graph or the VM rewrite did it.

**Recommendation:** this plan creates the empty `presentation/` package in `:feature:notes` and stops there. VM migration is a separate follow-up, done one screen at a time behind the existing bridge. Section 8 sketches it.

### 2.3 `hardware/` and `media/` in shared — contract only

The doc lists `shared/hardware/{camera,microphone,...}` and `shared/media/{player,recorder,...}`. Today **all** of that is native: `androidApp/hardware/**` (CameraX, ExoPlayer, MediaSessionService) and `iosApp/hardware/**` (AVFoundation). Moving them into shared means writing `expect`/`actual` wrappers over two mature native implementations — that is a feature project, not a restructure, and it is where regressions would come from.

**Recommendation:** drop `hardware/` and `media/` from the first pass. If you want the seam now, add only `interface AudioRecorder` / `interface MediaPlayer` in `:core:common` and let the existing native classes implement them. Zero behaviour change.

### 2.4 `sync/`, `ai/`, `analytics/` — no code exists

`data/sync/SyncStatus.kt` is one enum. `ai/` and `analytics/` are empty. Creating three Gradle modules for one enum costs configuration time and buys nothing. **Recommendation:** `SyncStatus` → `:core:model`. Create `:core:sync` when the sync engine is actually written.

### 2.5 Repository implementations must leave `domain/`

`NoteRepository` (impl) currently lives in `domain/repositories/noteRepository/`. In a single module that is only a smell; **across modules it becomes a hard circular dependency** — `domain` would need `data` and `data` needs `domain`. This move is mandatory, not optional:

```
domain/repositories/noteRepository/ILocalNotesRepository.kt   → domain/repository/   (interface, stays)
domain/repositories/noteRepository/IRemoteNoteRepository.kt   → domain/repository/   (interface, stays)
domain/repositories/noteRepository/NoteRepository.kt          → data/repository/     (impl, MOVES)
domain/repositories/noteRepository/NoteContentRepository.kt   → data/repository/     (impl, MOVES)
```

### 2.6 Two `initKoin()` functions exist — cleanup

`commonMain/koinDI/Koin.kt` declares `initKoin(appDeclaration: KoinAppDeclaration = {})` and `iosMain/koinDI/Koin.kt` declares `initKoin()` — same package, same name. iOS calls `KoinKt.doInitKoin(appDeclaration:)` (the commonMain one), so the iosMain version is **dead code** and only registers `getDataSourceFromPlatForm()` + `getDatabaseModule()`. It compiles today because the signatures differ, but it is an ObjC-name-clash waiting to happen when the umbrella is introduced. Recommend deleting `shared/src/iosMain/.../koinDI/Koin.kt` in Phase 6 — **your call, no file will be deleted without approval.**

### 2.7 Minor build cleanups (found while auditing)

- `shared/build.gradle.kts` adds `ktor-client-darwin` in **both** `nativeMain` and `iosMain`. `iosMain` alone is enough.
- `Greeting.kt` + `Platform.kt` are KMM-template leftovers. `Platform` has `expect`/`actual`; `Greeting` is unreferenced. Flagging only — no deletion without approval.
- `commonMain` uses `api(...)` for ktor/datetime/datastore/koin specifically so the iOS framework re-exports them. **This is the single thing most likely to break the multi-module build** — see §5.3.

---

## 3. Target Module Graph (8 modules + umbrella)

```
                          ┌──────────────┐
   androidApp ───────────►│              │
                          │   :shared    │  ← umbrella, name UNCHANGED
   iosApp (import shared)►│  (iOS fwk)   │     api() + export() only, no code
                          └──────┬───────┘
                                 │
        ┌────────────────┬───────┴────────┬─────────────────┐
        ▼                ▼                ▼                 ▼
 :feature:notes    :feature:auth   :feature:export     (future features)
        │                │                │
        └────────────────┴───────┬────────┘
                                 ▼
                            :core:data          BaseRepository, BaseUseCase,
                                 │              ILocalRepository, IRemoteRepository
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
       :core:network      :core:database       :core:model
              │                  │                  │
              └──────────────────┴─────────┬────────┘
                                           ▼
                                     :core:common
```

**Rule: dependencies point down only.** No sideways `:feature:notes` → `:feature:auth`. If two features need the same thing, it moves down into `:core:*`.

### File-by-file destination map

| Current path (under `shared/src/`) | Target module | Package inside module |
|---|---|---|
| `commonMain/util/**` (11 files) | `:core:common` | `core.common.util` |
| `commonMain/extensions/**` (2) | `:core:common` | `core.common.extensions` |
| `commonMain/koinDI/CoroutinesDispatcher.kt` + 2 actuals | `:core:common` | `core.common.coroutine` |
| `androidMain|iosMain/util/{LocalFilePathResolver,UniqueIdGenerator}.*.kt` | `:core:common` | `core.common.util` |
| `iosMain/bridge/{Closeable,BridgeError,FlowBridge}.kt` | `:core:common` (iosMain) | `core.common.bridge` |
| `commonMain/data/models/**` (10) | `:core:model` | `core.model` |
| `commonMain/data/sync/SyncStatus.kt` | `:core:model` | `core.model.sync` |
| `commonMain/data/network/**` (4) + 2 actuals | `:core:network` | `core.network` |
| `commonMain/data/localdb/database/**` (5) + 4 actuals | `:core:database` | `core.database` |
| `commonMain/sqldelight/**` (`.sq`, `.sqm`, snapshots) | `:core:database` | *(package unchanged)* |
| `commonMain/data/localdb/preferences/**` (4) + 2 actuals | `:core:database` | `core.database.preferences` |
| `commonMain/domain/repositories/base/**` (3) | `:core:data` | `core.data` |
| `commonMain/domain/repositories/usecases/BaseUseCase.kt` | `:core:data` | `core.data` |
| `domain/repositories/noteRepository/I*.kt` (2) | `:feature:notes` | `feature.notes.domain.repository` |
| `domain/repositories/noteRepository/*Repository.kt` (2, **impls**) | `:feature:notes` | `feature.notes.data.repository` |
| `usecases/NoteUseCase.kt` + `NoteContentUseCase.kt` (14 classes) | `:feature:notes` | `feature.notes.domain.usecase` |
| `iosMain/bridge/{NotesBridge,NoteContentBridge,ReaderPrefsBridge}.kt` | `:feature:notes` (iosMain) | `feature.notes.bridge` |
| `iosMain/domain/NoteRepositoryHelper.kt` | `:feature:notes` (iosMain) | `feature.notes.bridge` |
| `usecases/{LoginUseCase,SignupUseCase}.kt` (5 classes) | `:feature:auth` | `feature.auth.domain.usecase` |
| `iosMain/bridge/AuthBridge.kt`, `UserPreferenceViewModel.kt` | `:feature:auth` (iosMain) | `feature.auth.bridge` |
| `commonMain/export/**` (3) | `:feature:export` | `feature.export` |
| `commonMain/koinDI/Koin.kt` | `:shared` | `com.app.pustakam.koinDI` *(unchanged — iOS calls `KoinKt`)* |
| `iosMain/koinDI/KoinHelper.kt` | `:shared` (iosMain) | unchanged |
| `Greeting.kt`, `Platform.kt`, `crashlytics/` | `:core:common` | `core.common` |

> `:core:data` exists because `BaseRepository` needs network **and** database **and** is extended by both notes and auth. It cannot live in either one without creating a cycle.

---

## 4. Execution Phases

**Non-negotiable rule: every phase ends with a green build on both platforms and a manual smoke test.** If a phase can't get there, revert that phase only — the earlier ones stay.

Each phase = one commit. Code touched during the move carries the project's one-liner comment convention (`// 🔧 29-Jul-2026 <what changed>`).

### Phase 0 — Baseline & safety net · ~0.5 day

1. Branch: `refactor/multi-module`.
2. Record a working baseline: Android APK installs + Notes list, editor, audio record/play, camera, search, export-PDF, login all pass. iOS same. Screenshot each.
3. `git status` currently shows a large pending doc rename — **commit or stash that first** so the restructure diff is readable.
4. Add `gradle/module-conventions.gradle.kts` (or `build-logic/`) holding the repeated KMP + Android-library block. Without it you copy 25 lines of boilerplate into 8 files.
5. Confirm `./gradlew :androidApp:assembleDebug` and the Xcode build are green **before** touching anything.

**Verify:** builds green, baseline recorded.

### Phase 1 — `:core:common` + `:core:model` · ~1 day

Two leaf modules, nothing depends on them yet in the wrong direction. Lowest risk, do them first to prove the convention plugin works.

1. `settings.gradle.kts` → `include(":core:common", ":core:model")`.
2. Move files per the map. **Use the IDE's Move refactor**, not `mv` — it rewrites imports across all 148 Kotlin files automatically.
3. `:core:model` must `api(libs.kotlinx.serialization)` + `api(libs.kotlinx.datetime)` — models are in public signatures everywhere.
4. `shared/build.gradle.kts` → `api(projects.core.common)`, `api(projects.core.model)`. **`api`, not `implementation`** — see §5.3.

**Verify:** both apps build; Notes list renders; dates format correctly (exercises `DateTimeUtils`).

### Phase 2 — `:core:network` + `:core:database` · ~1.5 days

The fiddly one. SQLDelight config moves with the `.sq` files.

1. Move the whole `sqldelight { }` block from `shared/build.gradle.kts` into `core/database/build.gradle.kts` **unchanged** — `packageName`, `schemaOutputDirectory`, `verifyMigrations` all stay identical so generated code lands in the same package and existing `.sqm` migrations keep matching.
2. Move `commonMain/sqldelight/**` including `databases/` snapshots. Losing the snapshots breaks `verifyMigrations`.
3. `:core:database` must `api()` the generated `NotesDatabase` + `NotesDao` (used by `:core:data`) and `api(libs.datastore)` + `api(libs.datastore.preferences)`.
4. `linkerOpts.add("-lsqlite3")` stays on the **umbrella framework** block, not here.

**Verify:** `./gradlew :core:database:generateCommonMainNotesDatabaseSchema` succeeds. **Critical smoke test: install over the existing app (do NOT uninstall) and confirm old notes are still there.** If the DB name or package drifted, this is where you find out.

### Phase 3 — `:core:data` · ~0.5 day

Move `BaseRepository`, `ILocalRepository`, `IRemoteRepository`, `BaseUseCase`. Depends on `:core:network` + `:core:database` + `:core:model`. Small and mechanical.

**Verify:** login still works (`BaseRepository._userAuthState` drives auth).

### Phase 4 — `:feature:auth` + `:feature:notes` · ~1.5 days

Do `:feature:auth` first — 5 use cases, one bridge, much smaller blast radius. Then `:feature:notes`.

1. Create the doc's package tree inside each module: `domain/{model,repository,usecase,validation}`, `data/{repository,datasource,local,remote,mapper}`, `presentation/` *(empty for now)*, `api/`.
2. Apply §2.5 — repository impls into `data/repository/`, interfaces stay in `domain/repository/`.
3. iosMain bridges move into their owning feature module.
4. Split `NoteUseCase.kt` (11 classes in one file) into one file per use case while you're moving it — the doc's `usecase/` folder implies this and it makes the next reviewer's life easier.

**Verify:** full iOS regression — notes list, editor save (the `writeScope` path in `NotesBridge`), tags, search, reader progress. Android same.

### Phase 5 — `:feature:export` · ~0.5 day

`DocxExporter`, `NoteExportBuilder`, `OoxmlPackaging`. Consumed by 7 files in `androidApp` and by `NoteExporter.swift`.

**Verify:** export a note to PDF/DOCX on both platforms and open the file.

### Phase 6 — `:shared` umbrella + iOS framework export · ~1 day · **highest risk**

`:shared` keeps its name and its directory, loses all its source, and becomes a manifest. Full detail in §5.

**Verify:** Xcode build green with **zero** changes to any of the 86 Swift files. That is the pass condition.

### Phase 7 — Koin DI split · ~1 day

Today one `initKoin()` knows about every class in the app — which means `:shared` transitively needs everything, defeating the module boundaries.

Each module exposes its own module function; the umbrella composes them:

```kotlin
// :core:database   — 🔧 29-Jul-2026 module-local DI, was inline in Koin.kt
fun databaseModule(): Module = module { /* NotesDatabase, adapters, driver */ }

// :feature:notes   — 🔧 29-Jul-2026 module-local DI
fun notesModule(): Module = module {
    single { NoteRepository() }
    single { NoteContentRepository() }
    factory { CreateORUpdateNoteUseCase() }
    // ... 13 more
}

// :shared/commonMain/koinDI/Koin.kt — composition root only, name UNCHANGED
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        commonModule(), networkModule(), databaseModule(), preferencesModule(),
        coreDataModule(), authModule(), notesModule(), exportModule(),
        getDataSourceFromPlatForm(), getDatabaseModule(),
    )
}
```

Keep the function name `initKoin` and its signature — Swift calls `KoinKt.doInitKoin(appDeclaration:)` and Android's Application calls `initKoin { androidContext(...) }`. Changing either breaks both apps.

**Verify:** cold start both apps. Koin resolution failures surface immediately at startup, which is the good case.

### Phase 8 — Full regression + cleanup · ~1 day

Full pass on the Phase 0 checklist, both platforms, on a **release** build too (`isMinifyEnabled = false` today, but check). Then re-enable `org.gradle.configuration-cache` verification and confirm no module broke it.

---

## 5. Multi-Module iOS — Swift + Kotlin

This is the part with no Android equivalent, so it gets its own section.

### 5.1 The constraint

Kotlin/Native compiles **one Gradle module into one framework**. Eight modules would mean eight frameworks — and with `isStatic = true` that risks duplicate Kotlin-runtime symbols at link time. The standard fix is an **umbrella module**: one framework, assembled from many modules.

### 5.2 The umbrella — `shared/build.gradle.kts` after the restructure

```kotlin
// 🔧 29-Jul-2026 :shared is now an umbrella — no source, only re-exports
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget { compilations.all { kotlinOptions { jvmTarget = "17" } } }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "shared"          // ← unchanged: all 86 Swift files keep `import shared`
            isStatic = true
            linkerOpts.add("-lsqlite3")

            // export() = these modules' public API appears in shared.h for Swift.
            // Omit one and its types vanish from Swift with no compile error here —
            // the failure shows up in Xcode as "cannot find type 'Note' in scope".
            export(projects.core.common)
            export(projects.core.model)
            export(projects.core.database)
            export(projects.core.network)
            export(projects.core.data)
            export(projects.feature.notes)
            export(projects.feature.auth)
            export(projects.feature.export)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api(), NOT implementation() — export() requires an api dependency
            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.database)
            api(projects.core.network)
            api(projects.core.data)
            api(projects.feature.notes)
            api(projects.feature.auth)
            api(projects.feature.export)
            api(libs.koin)               // Koin types appear in KoinHelper's signature
        }
    }
}

android {
    namespace = "com.app.pustakam"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

### 5.3 The rule that will bite you

> **`export()` only works on an `api()` dependency. `implementation()` silently produces an empty header.**

And it's transitive: if `:feature:notes` uses `implementation(projects.core.model)`, then `Note` is invisible to Swift even though `:shared` exports `:core:model` — because `NotesBridge.readNote(...)` returns a type its own module doesn't publicly expose.

**Rule of thumb:** any type that appears in a bridge's public signature must be `api()` all the way down the chain. When in doubt, use `api()` between your own modules; the cost is a slightly larger recompile scope, not correctness.

### 5.4 Feature module Kotlin side — `:feature:notes/build.gradle.kts`

```kotlin
// 🔧 29-Jul-2026 feature module: domain + data + iOS bridge for Notes
kotlin {
    androidTarget { /* ... */ }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { it /* no framework block! */ }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)     // Note/Notes/Tag are in NotesBridge's signature → api
            api(projects.core.data)      // BaseRepository is a supertype of NoteRepository → api
            implementation(projects.core.database)   // NotesDao is internal detail → implementation
            implementation(libs.kotlinx.coroutines.core)
            api(libs.koin)               // KoinComponent is a supertype of NotesBridge → api
        }
    }
}
```

Note the **absent `binaries.framework { }` block** — only the umbrella declares a framework.

### 5.5 The bridge — unchanged code, new module

`NotesBridge.kt` moves from `shared/src/iosMain/.../bridge/` to `feature/notes/src/iosMain/.../bridge/`. **The body does not change** — only the `package` line and imports:

```kotlin
// 🔧 29-Jul-2026 moved from :shared → :feature:notes, body unchanged
package com.app.pustakam.feature.notes.bridge

import com.app.pustakam.core.model.notes.Note        // was: data.models.response.notes.Note
import com.app.pustakam.core.common.bridge.Closeable // was: bridge.Closeable
import com.app.pustakam.feature.notes.domain.usecase.GetNotesUseCase

class NotesBridge : KoinComponent {
    private val getNotesUseCase: GetNotesUseCase by inject()

    fun getNotes(
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (Notes?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { getNotesUseCase(page) }, onLoading, onSuccess, onError)
}
```

### 5.6 Swift side — **nothing changes**

This is the whole point of keeping the umbrella named `shared`:

```swift
import Foundation
import shared                                   // ← unchanged

final class NotesBridgeAdapter {
    private let bridge = NotesBridge()          // ← unchanged, now from :feature:notes
    private var closeables: [Closeable] = []    // ← unchanged, now from :core:common

    func getNotes(page: Int, onState: @escaping (UiState<Notes>) -> Void) {
        closeables.append(bridge.getNotes(      // ← unchanged
            page: Int32(page),
            onLoading: { onState(.loading) },
            onSuccess: { onState(.success($0)) },
            onError:   { onState(.failure($0)) }
        ))
    }
    deinit { closeables.forEach { $0.close() }; bridge.dispose() }
}
```

Kotlin package names do **not** appear in the generated ObjC header — only class names. `NotesBridge` is `NotesBridge` in Swift whether it lives in `:shared` or `:feature:notes`. Xcode's `embedAndSign` build phase also needs no edit, because it targets `:shared` and `:shared` still exists under the same name.

### 5.7 The one Swift-visible risk: ObjC name collisions

ObjC has no namespaces. Two Kotlin classes named `Mapper` in different modules become `SharedMapper` and `SharedMapper_` in the header — ugly and order-dependent. Guard against it with a per-module prefix where a clash is plausible:

```kotlin
// :feature:notes  — 🔧 29-Jul-2026 avoid ObjC name clash across feature modules
@ObjCName("NotesNoteMapper")
class NoteMapper { /* ... */ }
```

Only needed for genuinely duplicated names. Nothing in the current 21 iosMain files collides.

---

## 6. Estimate & Complexity

| Phase | Work | Days | Complexity | Risk if it goes wrong |
|---|---|---:|---|---|
| 0 | Baseline, convention plugin | 0.5 | Low | — |
| 1 | `:core:common` + `:core:model` | 1.0 | Low | Import churn across 148 files (IDE handles it) |
| 2 | `:core:network` + `:core:database` | 1.5 | **High** | **SQLDelight schema/migration drift → user data loss** |
| 3 | `:core:data` | 0.5 | Low | Auth state breaks |
| 4 | `:feature:auth` + `:feature:notes` | 1.5 | Medium | Koin resolution, repo-impl move |
| 5 | `:feature:export` | 0.5 | Low | Export silently broken |
| 6 | `:shared` umbrella + iOS export | 1.0 | **High** | **Swift can't see Kotlin types → 86-file cascade** |
| 7 | Koin DI split | 1.0 | Medium | Runtime crash at cold start (loud, so findable) |
| 8 | Full regression | 1.0 | Low | — |
| | **Total** | **8.5 focused days** | **Medium-High overall** | |

**Calendar:** ~2 weeks for one developer alongside other work. ~8–9 days if this is the only task.
**Add 1–2 days buffer** — Phase 2 and Phase 6 are the ones that historically overrun.

### Where the risk actually is

1. **SQLDelight (Phase 2)** — `packageName`, `schemaOutputDirectory` and the `databases/` snapshots must move together and unchanged. `verifyMigrations = true` is your friend here: it fails the build on drift rather than shipping a broken migration. Test by upgrading over an installed build, never a fresh install.
2. **`api` vs `implementation` (Phase 6)** — the failure mode is a confusing Xcode error, not a Gradle one. §5.3 is the mitigation.
3. **Gradle configuration cache** — already ON. It is stricter in multi-module builds. If a phase breaks it, that phase has a build-script bug, not a cache problem.
4. **Koin (Phase 7)** — moving repository impls between modules changes nothing at runtime as long as every `single`/`factory` is still registered. Cold-start crashes make omissions obvious.

### What is explicitly NOT in this estimate

Shared ViewModels (§2.2), `hardware/`+`media/` in shared (§2.3), sync engine, and the ~20 empty feature modules from the doc. Each is its own project.

---

## 7. How to Test

Run this list at the end of **every phase**, both platforms. Anything red = stop and fix before the next phase.

```
Android:  ./gradlew clean :androidApp:assembleDebug
iOS:      Xcode → Product → Clean Build Folder → Build
DB:       ./gradlew :core:database:generateCommonMainNotesDatabaseSchema
```

**Smoke checklist (both platforms):**

1. Cold start → no Koin crash
2. Login → Notes list loads
3. Create note → type text → back → **reopen: text persisted** (covers the `writeScope` fix)
4. Add image / audio recording / video → plays back
5. Add tag → filter by tag
6. Search a phrase → results appear
7. Open book reader → scroll → back → **reopen: progress restored**
8. Export a note → open the file
9. Delete a note → list updates
10. **Install over the previous build (do not uninstall) → old notes still present**

Item 10 is the one that catches a broken SQLDelight move. Do not skip it.

---

## 8. Follow-up: Shared Presentation (separate project)

When you're ready for §2.2, the safe order is one screen at a time behind the bridge that already exists:

1. Add `NotesListViewModel` (MVI: `State` / `Intent` / `Reducer`) in `:feature:notes/presentation/home/`.
2. Expose it to iOS via the existing `NotesBridge` pattern — Swift's `NotesViewModel` becomes a thin `@Observable` wrapper that forwards intents and renders state.
3. Android's `NotesViewModel` delegates to the shared one instead of calling use cases directly.
4. Ship it. Verify. Only then move to the editor.

Both UIs stay exactly as they are — they render shared state instead of local state. That is the doc's architecture diagram, reached without a rewrite.
