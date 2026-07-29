plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.app.pustakam.core.network"
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

    // 🔧 30-Jul-2026 02:10 — framework block REMOVED (was baseName "networkKit"): only :shared emits an iOS framework
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // 🔧 30-Jul-2026 02:10 — ApiCallClient returns Result<BaseResponse<Note>, Error>: all three modules are in its public signature
                api(projects.core.common)
                api(projects.core.model)
                // 🔧 30-Jul-2026 02:10 — BaseClient reads the auth token through IAppPreferences
                api(projects.core.database)
                api(libs.bundles.ktor)
                api(libs.kotlinx.serialization.json)
                // 🔧 30-Jul-2026 02:10 — ApiCallClient is a KoinComponent -> supertype -> api
                api(libs.koin)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
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
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
