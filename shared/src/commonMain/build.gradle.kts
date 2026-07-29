plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    tasks.register("testClasses")
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
            linkerOpts.add("-lsqlite3")

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
            //put your multiplatform dependencies here
            implementation(libs.kotlinx.coroutines.core)
            api(libs.bundles.ktor)
            api(libs.kotlinx.datetime)
            api(libs.datastore.preferences)
            api(libs.datastore)
            api(libs.koin)

            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.database)
            api(projects.core.network)
            api(projects.core.data)
            api(projects.feature.notes)
            api(projects.feature.auth)
            api(projects.feature.export)
            api(libs.koin)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
            api(libs.bundles.koinAndroid)
        }
        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
//        desktopMain.dependencies{
//            implementation(compose.desktop.currentOs)
//            implementation(libs.ktor.client.okhttp)
//        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.app.pustakam"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(libs.androidx.compiler)
}
sqldelight {
    databases {
        create("NotesDatabase") {
            packageName.set("com.app.pustakam.database")
        }
    }
}

