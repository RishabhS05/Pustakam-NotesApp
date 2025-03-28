plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.app.pustakam.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.app.pustakam.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }
    implementation(projects.shared)
//    implementation(projects.camerax)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.ui)
    implementation(libs.coil.svg)
    implementation(libs.coil)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.bundles.material3)
    implementation(libs.compose.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.camerax)
    implementation(libs.androidx.constraintlayout)
    debugImplementation(libs.compose.ui.tooling)
}