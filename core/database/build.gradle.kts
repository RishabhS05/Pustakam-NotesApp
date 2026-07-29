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
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // 🔧 29-Jul-2026 01:52 — targets declared WITHOUT binaries.framework: only the :shared umbrella emits an iOS framework, N static frameworks would duplicate the Kotlin runtime at link time
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                // 🔧 29-Jul-2026 01:52 — api() not implementation(): these types sit in public signatures of every module above, and export() to Swift only follows api deps
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.datastore.preferences)
                api(libs.datastore)
            }
        }


        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies{
            implementation(libs.sqldelight.android)
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
sqldelight {
    databases {
        create("NotesDatabase") {
            packageName.set("com.app.pustakam.core.database")
        }
    }
}