plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.app.pustakam.core.model"
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
                // 🔧 30-Jul-2026 02:10 — models use ContentType/UniqueIdGenerator/Result from :core:common in public signatures
                api(projects.core.common)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                // 🔧 30-Jul-2026 02:10 — every model is @Serializable; until now the runtime only arrived transitively via the ktor bundle
                api(libs.kotlinx.serialization.json)
            }
        }

        androidMain {
            dependencies {
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
