plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    androidLibrary {
        namespace = "com.app.pustakam.core.filesys"
        // 🔧 30-Jul-2026 02:10 — 35 (was 36): a library must not compile against a HIGHER API than :androidApp (35), AGP fails the build on that
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

    // 🔧 30-Jul-2026 02:10 — targets WITHOUT binaries.framework: only the :shared umbrella emits an iOS framework; N static frameworks duplicate the Kotlin runtime at link time
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // 🔧 30-Jul-2026 02:10 — ExportBlock/FileImportHelper expose Note, NoteContentModel and ContentType
                api(projects.core.common)
                api(projects.core.model)
                api(projects.core.richtext)
                // 🔧 30-Jul-2026 02:10 Phase 3 — getFileSystemModule() returns a Koin Module, so Koin is in this module's public API
                api(libs.koin)
            }
        }

        androidMain {
            dependencies {
                // 🔧 30-Jul-2026 02:10 Phase 3 — the Android bindings resolve filesDir via androidContext()
                implementation(libs.koin.android)
            }
        }

        iosMain {
            dependencies {
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
            }
        }
    }
}
