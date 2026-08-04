package com.app.pustakam.android.permission

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build

enum class NeededPermission(
    val permission: String,
    val title: String,
    val description: String,
    val permanentlyDeniedDescription: String,
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val maxSdk: Int = Int.MAX_VALUE,
) {

    @SuppressLint("InlinedApi")
    BACKGROUND_LOCATION(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        title = "Background Location Permission",
        description = "This permission is needed to get your approximate location in background. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to get your approximate location. Please grant the permission in app settings.",
        minSdk = Build.VERSION_CODES.Q,
    ),

    FINE_LOCATION(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        title = "Accurate Location Permission",
        description = "This permission is needed to get your accurate location . Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to get your approximate location. Please grant the permission in app settings.",
    ),
    COARSE_LOCATION(
        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
        title = "Approximate Location Permission",
        description = "This permission is needed to get your approximate location. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to get your approximate location. Please grant the permission in app settings.",
    ),
    READ_CALENDAR(
        permission = Manifest.permission.READ_CALENDAR,
        title = "Read Calendar Permission",
        description = "This permission is needed to read your calendar. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to read your calendar. Please grant the permission in app settings.",
    ),
    READ_CONTACTS(
        permission = Manifest.permission.READ_CONTACTS,
        title = "Read Contacts Permission",
        description = "This permission is needed to read your contacts. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to read your contacts. Please grant the permission in app settings.",
    ),
    RECORD_AUDIO(
        permission = Manifest.permission.RECORD_AUDIO,
        title = "Record Audio permission",
        description = "This permission is needed to access your microphone. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to access your microphone. Please grant the permission in app settings.",
    ),
    CAMERA(
        permission = Manifest.permission.CAMERA,
        title = "Camera permission",
        description = "This permission is needed to access your camera. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to access your camera. Please grant the permission in app settings.",
    ),

    @SuppressLint("InlinedApi")
    READ_MEDIA_IMAGES(
        permission = Manifest.permission.READ_MEDIA_IMAGES,
        title = "Photos Permission",
        description = "This permission is needed to read photos on your device. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to read photos. Please grant the permission in app settings.",
        minSdk = Build.VERSION_CODES.TIRAMISU,
    ),

    @SuppressLint("InlinedApi")
    READ_EXTERNAL_STORAGE(
        permission = Manifest.permission.READ_EXTERNAL_STORAGE,
        title = "Storage Permission",
        description = "This permission is needed to read files on your device. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed to read files. Please grant the permission in app settings.",
        maxSdk = Build.VERSION_CODES.S_V2,
    ),

    @SuppressLint("InlinedApi")
    POST_NOTIFICATIONS(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        title = "Post Notification Permission",
        description = "This permission is needed to show you the Notifications. Please grant the permission.",
        permanentlyDeniedDescription = "This permission is needed for Showing Notifications. Please grant the permission in app settings.",
        minSdk = Build.VERSION_CODES.TIRAMISU,
    );

    val isApplicable: Boolean
        get() = Build.VERSION.SDK_INT in minSdk..maxSdk

    fun permissionTextProvider(isPermanentDenied: Boolean): String {
        return if (isPermanentDenied) this.permanentlyDeniedDescription else this.description
    }
}

fun List<NeededPermission>.applicable(): List<NeededPermission> = filter { it.isApplicable }
