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
            freeCompilerArgs += listOf("-Xmemory-model=experimental")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(libs.bundles.ktor)
            api(libs.kotlinx.datetime)
            api(libs.datastore.preferences)
            api(libs.datastore)
            api(libs.koin)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
            api(libs.bundles.koinAndroid)
            api(libs.bundles.media3)
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
    compileSdk = 35
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
            // 🔧 P1/6: migration infrastructure.
            // Baseline = CURRENT schema (version 1). Every future schema change:
            //   1. edit NotesDatabase.sq to the NEW shape
            //   2. add <version>.sqm next to it with the ALTER/rebuild statements
            //   3. run: ./gradlew generateCommonMainNotesDatabaseSchema  (commits a new snapshot)
            // Drivers (AndroidSqliteDriver / NativeSqliteDriver) auto-run .sqm on version bump —
            // NO app reinstall needed anymore.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // Build-time proof that snapshot + migrations == current .sq (catches drift):
            verifyMigrations.set(true)
        }
    }
}

