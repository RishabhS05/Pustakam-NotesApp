plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.app.pustakam.core.database"
        // 🔧 30-Jul-2026 02:10 — 35 (was 36): a library must not compile against a HIGHER API than :androidApp (35)
        compileSdk = 35
        minSdk = 24

        withHostTestBuilder {
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // 🔧 30-Jul-2026 02:10 — targets WITHOUT binaries.framework: only the :shared umbrella emits an iOS framework
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                // 🔧 30-Jul-2026 02:10 — NotesDao maps rows to Note/Notes/Tag and takes RichTextMetadata: both are public API here
                api(projects.core.common)
                api(projects.core.model)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.datastore.preferences)
                api(libs.datastore)
                // 🔧 30-Jul-2026 02:10 — NotesDao/BasePreferences are KoinComponents, so Koin is a supertype -> api
                api(libs.koin)
                // 🔧 30-Jul-2026 02:10 — the RichTextMetadata ColumnAdapter serialises through Json
                api(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqldelight.android)
                // 🔧 30-Jul-2026 02:10 — getDatabaseModule.android.kt calls androidContext()
                implementation(libs.koin.android)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
            }
        }

        iosMain {
            dependencies {
                api(libs.sqldelight.native)
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}

// 🔧 30-Jul-2026 02:10 — moved here from :shared with schemaOutputDirectory + verifyMigrations RESTORED.
//   Dropping them silently disables migration-drift detection; the .sq, the 4 .sqm files and the
//   databases/ snapshots all moved together, so the on-disk DB ("Notes.db") and its schema are unchanged.
sqldelight {
    databases {
        create("NotesDatabase") {
            // 🔧 30-Jul-2026 02:10 — package follows the module (was com.app.pustakam.database).
            //   This only renames GENERATED Kotlin; the SQLite file name/schema are untouched, so existing notes survive an in-place upgrade.
            packageName.set("com.app.pustakam.core.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
