plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    androidLibrary {
        namespace = "com.app.pustakam.core.data"
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

    // 🔧 30-Jul-2026 02:10 — framework block REMOVED (was baseName "dataKit"): only :shared emits an iOS framework
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // 🔧 30-Jul-2026 02:10 — BaseRepository sits on network AND database at once and is the supertype of every feature repo, so all four are api()
                api(projects.core.common)
                api(projects.core.model)
                api(projects.core.database)
                api(projects.core.network)
                // 🔧 30-Jul-2026 02:10 — BaseRepository/BaseUseCase are KoinComponents -> supertype -> api
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
                // 🔧 30-Jul-2026 02:10 — FlowBridge (subscribeTo/subscribe/watch) lives here: it needs Closeable/BridgeError from :core:common and BaseResponse from :core:model, both already api() above
            }
        }
    }
}
