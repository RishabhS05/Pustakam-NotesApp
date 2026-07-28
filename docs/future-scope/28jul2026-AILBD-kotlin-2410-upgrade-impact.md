# Kotlin 2.4.10 Upgrade — Dependency Impact

**Date:** 28-Jul-2026
**Modules affected:** `androidApp` (A), `shared` → iOS framework (I), SQLDelight local DB (LBD)
**Trigger:** SceneView 4.x is built with Kotlin 2.3.20–2.4.10 and cannot be consumed by our Kotlin 2.1.20 compiler. Currently pinned down to SceneView 3.6.0 as a workaround.

---

## 1. Does KMM/KMP support Kotlin 2.4.10?

**Yes.** Kotlin Multiplatform ships as part of every Kotlin release, so 2.4.10 supports it by definition. Notes:

- "KMM" is a retired brand name. The standalone KMM Android Studio plugin was discontinued; tooling is now the Kotlin Multiplatform plugin. Our build already applies `org.jetbrains.kotlin.multiplatform`, so **nothing changes structurally** in `shared/build.gradle.kts` or the iOS framework setup.
- `iosX64 / iosArm64 / iosSimulatorArm64` targets, `binaries.framework { isStatic = true }`, and cinterop all work the same on 2.4.x.

**The real constraint is klib binary compatibility, not KMP support.** Every multiplatform dependency publishes `.klib` artifacts for iOS. A klib produced by an old Kotlin can become unreadable by a newer Kotlin/Native compiler. That is why the risky dependencies below are the *multiplatform* ones (Ktor, SQLDelight, coroutines, datetime, Koin) — not the Android-only ones.

> One `freeCompilerArgs` entry needs review: `-Xmemory-model=experimental` in `shared/build.gradle.kts`. The new memory model has been the default since Kotlin 1.7.20 and the flag is long obsolete — it may now be rejected outright. Remove it as part of this upgrade.

---

## 2. Changes that happen automatically

These all read `version.ref = "kotlin"`, so editing one line moves five things:

| Plugin / library | Catalog alias |
|---|---|
| `org.jetbrains.kotlin.android` | `kotlinAndroid` |
| `org.jetbrains.kotlin.multiplatform` | `kotlinMultiplatform` |
| `org.jetbrains.kotlin.plugin.compose` | `compose-compiler` |
| `org.jetbrains.kotlin.plugin.serialization` | `kotlin-serialization` |
| `org.jetbrains.kotlin:kotlin-test` | `kotlin-test` |

No action needed beyond `kotlin = "2.4.10"`.

---

## 3. Must change — blocking

| Dependency | Current | Target | Why | Source break? |
|---|---|---|---|---|
| `kotlin` | 2.1.20 | **2.4.10** | The upgrade itself | Low |
| `ktor` | 2.3.12 | **3.5.0** | Ktor 2.x klibs predate Kotlin 2.2; the 3.x line is what's built against current Kotlin | **Yes — large** |
| `sqldelight` | 2.0.2 | **2.3.2** | Gradle plugin + codegen are Kotlin-version coupled; 2.0.2 predates Kotlin 2.2 | Low–medium |
| `coroutines` | 1.9.0 | **1.11.0** | 1.9.0 klibs built pre-2.2 | Low |
| `datetime` | 0.6.1 | **0.8.0-0.6.x-compat** | 0.7.0+ moved `Instant` into `kotlin.time`. The `-0.6.x-compat` artifact keeps the 0.6 API surface | Low if you use the compat artifact |
| `koin` | 4.0.0 | **4.2.2** | Multiplatform klibs; 4.0.0 predates Kotlin 2.2 | Low |

### Ktor 3 is the expensive one

This is the bulk of the migration effort, and it lands in `shared/commonMain` — the code that feeds both Android and iOS:

- Ktor 3 replaced `io.ktor.utils.io.core` (kotlinx-io based) — byte-handling APIs changed
- `HttpResponse.body<T>()` and channel/streaming APIs were reworked
- The `ktor-client-auth`, `ktor-client-logging`, `ktor-client-content-negotiation` plugin registration syntax shifted
- Darwin and OkHttp engines both moved to the 3.x line together — they must match

**Unrelated but worth fixing while you're in the catalog:** `ktor-client-auth` is declared as `"io.ktor: ktor-client-auth"` — there's a stray space after the colon. It resolves today only because nothing forces it.

---

## 4. Should change — not Kotlin-blocking, but overdue

| Item | Current | Note |
|---|---|---|
| `kotlinCompilerExtensionVersion = "1.5.15"` | in `androidApp/build.gradle.kts` | **Dead config.** Since Kotlin 2.0 the Compose compiler is a Kotlin plugin (`compose-compiler`, already applied). Delete the whole `composeOptions { }` block. |
| `androidx.databinding:compiler` | 3.2.0-alpha11 | A 2018 alpha, applied in `shared`. Almost certainly unused — verify and drop. |
| Compose version skew | `compose = 1.7.8`, `animation = 1.10.0` | animation 1.10.0 drags compose-ui to 1.10.x anyway, so 1.7.8 is fiction. Move to the Compose BOM. |
| material3 declared twice | `compose-material3 = 1.3.1` **and** `material3 = 1.4.0` | Both are `androidx.compose.material3:material3`, both in `androidApp` deps. Pick one. |
| `agp` / Gradle | 8.10.0 / 8.11.1 | **Verify against the KGP 2.4 compatibility matrix before starting** — I have not confirmed the exact floor. Kotlin releases usually raise the minimum Gradle version. |

---

## 5. Unaffected — no change needed

ARCore 1.54.0 · media3 1.5.1 · CameraX 1.4.1 · Coil 2.7.0 · Firebase BOM 34.16.0 · play-services-location · work-runtime-ktx · constraintlayout · appcompat · navigation-compose · activity-compose · datastore

These are Android-only (Java/Kotlin JVM), publish no klibs, and are compiled with older Kotlin — which a **newer** compiler reads without issue. Metadata compatibility only breaks in the forward direction.

---

## 6. The payoff

| | Before | After |
|---|---|---|
| SceneView | 3.6.0 (pinned down) | **4.25.0** — the version you actually wanted |
| ARCore API surface | 3.x node DSL | 4.x, current docs and samples apply |
| compileSdk | 36 | 36 (unchanged) |

---

## 7. Suggested order of work

1. Branch off. This is not a mid-feature change.
2. Verify the AGP + Gradle floor for KGP 2.4.10 first — if that forces an AGP bump, do it as step zero.
3. Bump `kotlin`, `coroutines`, `datetime`, `koin`. Build `:shared:compileKotlinIosArm64` **before** touching Android — Native is where klib problems surface first and loudest.
4. Remove `-Xmemory-model=experimental` and the `composeOptions { }` block.
5. Bump `sqldelight`. Regenerate and run the DB migration tests.
6. Ktor 2 → 3 migration. Budget the most time here; it is a source migration, not a version bump.
7. Bump SceneView back to 4.25.0 and restore the 4.x `ARSceneView` composable API in `ARWorkspaceContent.kt`.
8. Tidy the Compose version skew and the duplicate material3.

## 8. How to verify at each step

```bash
cd Pustakm
./gradlew --stop
rm -rf .gradle build androidApp/build shared/build

# Native first — klib incompatibilities fail here before anything else
./gradlew :shared:compileKotlinIosArm64

# Then the full build
./gradlew :androidApp:assembleDebug --no-configuration-cache
```

Then open the iOS project in Xcode and confirm the shared framework links and the app launches.

---

## 9. Fallback if the upgrade stalls

Current state on `main` already builds: Kotlin 2.1.20 + SceneView 3.6.0 + compileSdk 36. The 3.x AR API (`ARScene`, `AnchorNode`, `ModelNode`, node DSL) is fully featured — staying there indefinitely is a legitimate choice, not a stopgap. The only thing given up is the 4.x API shape.
