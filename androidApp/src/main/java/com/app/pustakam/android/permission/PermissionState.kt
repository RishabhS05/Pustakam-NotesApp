package com.app.pustakam.android.permission

import android.app.Activity
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.core.content.edit

enum class PermissionState {
    GRANTED,
    NOT_ASKED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_APPLICABLE,
}

private const val PREFS_FILE = "pustakam_permission_state"
private const val ASKED_PREFIX = "asked_"

private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

fun markPermissionAsked(context: Context, permission: NeededPermission) {
    prefs(context).edit { putBoolean(ASKED_PREFIX + permission.name, true) }
}

fun markPermissionsAsked(context: Context, permissions: List<NeededPermission>) {
    prefs(context).edit {
        permissions.forEach { putBoolean(ASKED_PREFIX + it.name, true) }
    }
}

fun wasPermissionAsked(context: Context, permission: NeededPermission): Boolean =
    prefs(context).getBoolean(ASKED_PREFIX + permission.name, false)

fun Activity.permissionState(permission: NeededPermission): PermissionState = when {
    !permission.isApplicable -> PermissionState.NOT_APPLICABLE
    hasPermission(this, permission.permission) -> PermissionState.GRANTED
    !wasPermissionAsked(this, permission) -> PermissionState.NOT_ASKED
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission.permission) -> PermissionState.DENIED
    else -> PermissionState.PERMANENTLY_DENIED
}

fun Activity.isPermanentlyDenied(permission: NeededPermission): Boolean =
    permissionState(permission) == PermissionState.PERMANENTLY_DENIED
