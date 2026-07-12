# Pustakam — Coding-Style Modernization Guide

> **Status:** Analysis only. **No code has been changed.** Review before any work begins.
> **Date:** 2026-06-30
> **Scope:** `PustakmServer` (Node/Express) + `Pustakm` (KMM: `shared`, `androidApp` Compose, `iosApp` SwiftUI).
> **Relationship to the other doc:** `REFACTORING_AND_FIXES.md` covers *correctness/bugs*. This doc covers *style, idioms, and tooling currency* — i.e. "what would a reviewer flag as out-of-date for 2026, even if it works today." Where the two overlap, the bug register wins on priority.

## How to read this

Each row is one modernization change: current state, the modern equivalent, and an effort estimate.

**Effort:** S (hours) · M (1–2 days) · L (multi-day) · XL (structural)
**Priority:** 🔴 ship-blocker / security · 🟠 high-value · 🟡 nice-to-have · 🟢 cosmetic

---

## Top changes, in order

1. **Backend has no type safety, no linter, no validation, no tests** — adopt TypeScript + ESLint(flat)/Prettier + Zod + a test runner. This is the single biggest gap. *(BE-1, BE-3, BE-6, BE-9)*
2. **Backend has no security middleware** — add `helmet`, `cors`, `express-rate-limit`, and env-schema validation. *(BE-5)*
3. **Android Gradle uses deprecated `kotlinOptions`/`composeOptions`** — migrate to the `compilerOptions {}` DSL; drop the obsolete `kotlinCompilerExtensionVersion`. *(FE-1, FE-2)*
4. **iOS is pre-concurrency** — no `async/await`, `ObservableObject`+`@Published` everywhere, no `NavigationStack`. Move to Swift Concurrency, the Observation framework (`@Observable`), and typed navigation. *(IOS-1, IOS-2, IOS-3)*
5. **Logging is ad-hoc on every layer** (`console.log`, `println`, `log_d`, `LogLevel.ALL`) — replace with a real logger per platform and gate it by build type. *(BE-4, FE-5)*
6. **Add formatting/lint gates + CI** so style stays consistent automatically. *(X-1, X-2)*

---

## Backend — Node / Express

The server is clean ESM and already uses `import`/`export` and mostly `const` (only 2 stray `var`s). The gaps are about the modern Node service *baseline*, not syntax.

| ID   | Area / File | Current | Modern equivalent | Pri | Effort |
| ---- | ----------- | ------- | ----------------- | --- | ------ |
| BE-1 | Whole project | Plain JS, no `tsconfig.json`, hand-written `.js` classes (`models/Note.js`, `User.js`). | **Migrate to TypeScript** (or at minimum `// @ts-check` + JSDoc + `checkJs`). The `@types/*` packages are already in `devDependencies` but there's no compiler consuming them. Types on `req/res`, models, and DB documents catch the bug-register issues (e.g. wrong status mapping, `notesList` undefined var) at compile time. | 🟠 | L |
| BE-2 | `routes/routes.js`, `package.json` | Express **4.19**. | Express **5.x** is now the stable line. Its headline win here: **async errors are forwarded to error middleware automatically**, which lets you delete the `tryCatchWrapper`/`tryCatchWithoutAsync` helpers entirely (`helpers/ApiWrapper.js`). Also enables `router.route(...)` path-regex safety fixes. Plan the upgrade deliberately (a few breaking changes). | 🟠 | M |
| BE-3 | No `eslint`/`prettier` config present | Style is enforced by hand; inconsistent spacing (`this. content`, `type = type,`). | Add **ESLint flat config (`eslint.config.js`)** with `@eslint/js` + `typescript-eslint`, and **Prettier**. Add `lint`/`format` npm scripts. | 🟠 | S |
| BE-4 | Everywhere (11 `console.log`, incl. `console.log(\`token: ${token}\`)`) | Tokens and full note payloads are logged to stdout in plaintext. | Replace with a structured logger — **`pino`** (fast, JSON, log levels). Never log tokens/PII; gate `debug` behind `LOG_LEVEL`. Removing token logging is a security item, not cosmetic. | 🔴 | S |
| BE-5 | `server.js` | No `helmet`, no `cors`, no rate limiting, `app.listen(3000…)` hardcoded port. | Add **`helmet()`**, **`cors()`** with an allow-list, **`express-rate-limit`** on `/login` & `/register`, and read `PORT` from env. | 🔴 | S |
| BE-6 | All middleware handlers | Request bodies are trusted as-is (`new Note(req.body)`, destructured `{title,url,description}`); no schema validation. | Validate input with **Zod** (or Joi) at the route boundary; infer TS types from the schema so the model and the validator can't drift. | 🟠 | M |
| BE-7 | `db/notesdb.js`, `db/usersdb.js` | Raw MongoDB driver with stringly-typed field maps and manual `$set` objects. | Acceptable to keep the native driver, but wrap collections in **typed collection helpers** (`Collection<NoteDoc>`) once on TS. If you want schema + lifecycle hooks, **Mongoose 8** is the conventional choice. Don't keep both. | 🟡 | M |
| BE-8 | `auth/jwtAuth.js` | `jwt.sign(payload, key)` with **no `expiresIn`**, token split from header by hand, verify runs but flow isn't short-circuited (see bug register). | Add `expiresIn`, use `algorithms: ['HS256']` allow-list on verify, and express it as **real Express middleware** (`(req,res,next)`) placed in the route chain rather than called inside each handler. | 🔴 | S |
| BE-9 | `package.json` `scripts.test` | `"test": echo "no test specified" && exit 1`. | Add **`vitest`** (or `node:test`) + **`supertest`** for route tests. Even a handful of happy-path/auth tests pays for itself. | 🟠 | M |
| BE-10 | `package.json` | `nodemon` in `dependencies`; no `engines` field; no `.nvmrc`. | Move `nodemon` to `devDependencies`, pin **`"engines": { "node": ">=20" }`**, add `.nvmrc`. Modern Node can run TS directly or via `tsx` in dev — drop `nodemon` for `node --watch`/`tsx watch`. | 🟢 | S |
| BE-11 | `models/*.js` | ES classes whose constructors take a destructured object and set fields one-by-one; `LocationContent` calls `super(this._id, …)` (reads `this` before init — bug). | On TS these become **`interface` + factory functions** or Zod schemas; drops the buggy inheritance. | 🟡 | S |

---

## Frontend — Shared (Kotlin Multiplatform)

Kotlin **2.1.20**, AGP **8.10**, a proper **version catalog**, Ktor 2.3, Koin 4, SQLDelight 2 — the stack is current. The style gaps are idiom-level.

| ID   | Area / File | Current | Modern equivalent | Pri | Effort |
| ---- | ----------- | ------- | ----------------- | --- | ------ |
| FE-1 | `shared/build.gradle.kts`, `androidApp/build.gradle.kts` (3 `kotlinOptions {}` blocks) | `kotlinOptions { jvmTarget = "17" }` — **deprecated** in Kotlin 2.x. | Use the **`compilerOptions {}`** DSL: `compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }` (and `kotlin { jvmToolchain(17) }` to set the toolchain once). | 🟠 | S |
| FE-2 | `androidApp/build.gradle.kts` | `composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }`. | **Obsolete** — you already apply the `org.jetbrains.kotlin.plugin.compose` plugin, which manages the compiler. Delete the `composeOptions` block and the `androidx.compiler`/`databinding` compiler dep (`libs.androidx.compiler`, `compiler = "3.2.0-alpha11"`) if nothing else uses data-binding. | 🟠 | S |
| FE-3 | `shared/build.gradle.kts` | `freeCompilerArgs += "-Xmemory-model=experimental"`. | The **new (strict) memory model is the default** in current Kotlin/Native — the flag is a no-op/legacy and should be removed. | 🟡 | S |
| FE-4 | `data/network/BaseClient.kt` `baseApiCall` | Wraps a single call in `flow { … }.flowOn(IO).map { it }.first()` — a flow built only to immediately collect one value, plus a no-op `.map { it }`. Also imports `kotlinx.coroutines.IO` (the experimental `Dispatchers.IO` extension) unusually. | Make it a plain `suspend` function with `withContext(Dispatchers.IO) { runCatching { … } }`; drop the `flow/first()` dance and the `.map { it }`. Cleaner, fewer allocations, easier to read. | 🟠 | M |
| FE-5 | `util/Logger.kt` + 40 `println(...)`/`log_d(...)` sites; `NetworkClient.*.kt` hardcode `LogLevel.ALL`, `prettyPrint = true` | Debug logging (incl. auth headers/tokens) is always on, even in release. | Adopt a real KMP logger (**Kermit** or **Napier**); gate level by build type; set Ktor `LogLevel` from config, not hardcoded `ALL`. Remove token/`authorization` logging. | 🟠 | M |
| FE-6 | `NoteRepository.kt`, models | `ArrayList`/`arrayListOf()` exposed in state (`MutableStateFlow(Notes())` with mutable lists; `notes.notes[index] = …` mutates in place). | Prefer **immutable `List<T>`** in state and `kotlinx.collections.immutable` (`PersistentList`) for Compose stability; produce new lists via `map`/`+` instead of in-place mutation. Improves Compose recomposition correctness. | 🟡 | M |
| FE-7 | `NoteRepository.kt` | Long-lived `CoroutineScope(provideDispatcher().io + SupervisorJob())` created inline for `stateIn`, never cancelled. | Inject a scoped `CoroutineScope` (Koin) tied to a lifecycle, or use a repository `close()`; avoid ad-hoc unmanaged scopes. | 🟡 | M |
| FE-8 | Naming throughout shared/iOS | Typos baked into public API: `Basepreferences.kt`, `baseRepositary`, `repositary`, `Acitvity+ext.kt`, `getDataSourcefromPlatform`, plus iOS `Stroage`, `Recoder`, `Visualier`. | Rename to Kotlin/Swift conventions (`BasePreferences`, `repository`). Do it via IDE refactor in one pass; these leak across the KMM boundary into Swift. | 🟡 | M |
| FE-9 | `data/network/ApiRoute.kt`, base URL | Server base URL hardcoded (dev URL ships per bug register). | Drive base URL from **`BuildConfig`/build flavors** (Android) and an `xcconfig`/scheme (iOS) instead of a constant. | 🟠 | S |
| FE-10 | `androidApp/build.gradle.kts` `buildTypes.release` | `isMinifyEnabled = false`, no R8/proguard, `versionCode = 1`. | Enable **R8 shrinking/obfuscation** for release with a keep-rules file; wire real versioning. | 🟡 | S |

---

## Frontend — Android (Jetpack Compose)

Compose + ViewModel + Navigation-Compose stack is modern. Mostly small idiom items.

| ID    | Area | Current | Modern equivalent | Pri | Effort |
| ----- | ---- | ------- | ----------------- | --- | ------ |
| AND-1 | ViewModels / state | `collectAsStateWithLifecycle()` is already the norm (≈25 sites) but **2 screens still use plain `collectAsState()`**. | Standardize fully on `collectAsStateWithLifecycle()` and one immutable `data class XxxUiState` per screen exposed as `StateFlow`. Mostly done — just close the 2 stragglers. | 🟢 | S |
| AND-2 | `screen/base/BaseViewModel.kt` error path | `error as NetworkError` unchecked cast (also in bug register). | Idiomatic exhaustive `when (error) { is … }`. | 🟡 | S |
| AND-3 | Navigation `screen/navigation/*` | String/route-based nav. | Consider **type-safe navigation** (Navigation-Compose 2.8 supports `@Serializable` route objects) — removes stringly-typed args. | 🟢 | M |
| AND-4 | Theme/widgets | Material3 in use. | Verify dynamic-color/Material-You + edge-to-edge (`enableEdgeToEdge()`) since `targetSdk 35`/Android 15 enforces edge-to-edge. | 🟡 | S |

---

## Frontend — iOS (SwiftUI)

This is the least-modern client. It targets old SwiftUI/Combine idioms and doesn't use Swift Concurrency at all.

| ID    | Area / File | Current | Modern equivalent | Pri | Effort |
| ----- | ----------- | ------- | ----------------- | --- | ------ |
| IOS-1 | 11 `ObservableObject` + 18 `@Published` across VMs (`NotesViewModel`, `NoteEditorViewModel`, `ThemeManager`, …) | Pre-iOS-17 Combine object model. | Adopt the **Observation framework**: `@Observable` classes + `@State`/`@Bindable`, dropping `ObservableObject`/`@Published`/`@StateObject`. Less boilerplate, finer-grained view updates. (Requires iOS 17+ deployment target.) | 🟠 | L |
| IOS-2 | `network/BaseNetworkHandler.swift`, VMs | No `async/await`; callbacks/Combine bridging to the shared Kotlin flows. | Use **Swift Concurrency** (`async`/`await`, `Task`, `@MainActor` on view models). KMP exposes suspend funcs as completion handlers — wrap them with `withCheckedThrowingContinuation` or use a flow-to-`AsyncSequence` bridge (SKIE-style). | 🟠 | L |
| IOS-3 | Navigation (0 `NavigationView`, custom `Router.swift`) | Hand-rolled router. | Standardize on **`NavigationStack` + `NavigationPath`** for typed, value-driven navigation. | 🟡 | M |
| IOS-4 | `view/screens/base/BaseViewModel.swift` | `var baseRepositary`/`noteRepositary` resolved in `init()` via `KoinHelper()`; no `@MainActor`, mutable `var`s. | Mark VMs `@MainActor`, inject dependencies, fix `repositary`→`repository`. | 🟡 | S |
| IOS-5 | Project tooling | No `.swiftlint.yml`/`.swift-format` present. | Add **SwiftLint + swift-format** with a shared config; fixes the spacing/naming drift (`Stroage`, `Recoder`, `Visualier`, `AudioVisualer2`). | 🟡 | S |
| IOS-6 | Build settings | (Verify) likely Swift 5 language mode. | Move toward **Swift 6 language mode / strict concurrency checking** incrementally (`Sendable` audit) once IOS-1/IOS-2 land. | 🟢 | L |

---

## Cross-cutting / tooling

| ID  | Area | Current | Modern equivalent | Pri | Effort |
| --- | ---- | ------- | ----------------- | --- | ------ |
| X-1 | Formatting/lint | None wired for any of the three codebases. | One formatter+linter per stack, all run on commit: **ktlint/detekt** (Kotlin), **SwiftLint/swift-format** (iOS), **ESLint/Prettier** (server). Add a **pre-commit hook** (e.g. `lefthook`) so style is mechanical, not reviewed by hand. | 🟠 | M |
| X-2 | CI | No workflow files observed. | Add **GitHub Actions** (or similar): `lint + build + test` for server, `./gradlew check` for KMP/Android, `xcodebuild`/test for iOS. | 🟠 | M |
| X-3 | Secrets/config | `.env` is correctly gitignored, but there's **no `.env.example`** and no boot-time validation; client URLs hardcoded. | Add a committed **`.env.example`** documenting required vars, and **validate env at boot** (Zod env schema) so a missing `JWT_KEY` fails fast instead of producing `undefined`. | 🟠 | S |
| X-4 | Editorconfig | — | Add a root **`.editorconfig`** so indentation/charset are consistent across IDEs and all three languages. | 🟢 | S |

---

## Suggested sequencing

1. **Gates first (low risk, compounding):** ESLint/Prettier + ktlint + SwiftLint + `.editorconfig` + pre-commit. *(X-1, X-4, BE-3, IOS-5)*
2. **Security/ship-blockers:** remove token logging, add helmet/cors/rate-limit + env validation, JWT `expiresIn`, base-URL from config. *(BE-4, BE-5, BE-8, FE-9)*
3. **Gradle DSL cleanup** (mechanical, unblocks future Kotlin upgrades): `compilerOptions`, drop `composeOptions`/experimental memory flag. *(FE-1, FE-2, FE-3)*
4. **Backend TypeScript + Zod + Express 5 + tests** as one focused track. *(BE-1, BE-2, BE-6, BE-9)*
5. **iOS concurrency/Observation modernization** as its own track (largest, most invasive). *(IOS-1, IOS-2, IOS-3)*
6. **Shared idiom polish:** flatten `baseApiCall`, immutable state lists, scope hygiene, naming fixes. *(FE-4, FE-6, FE-7, FE-8)*

> Nothing here changes the **architecture constraint**: KMM with native SwiftUI + native Compose UI, shared Kotlin data/domain only.
