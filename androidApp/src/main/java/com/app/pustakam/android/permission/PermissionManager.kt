package com.app.pustakam.android.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.app.pustakam.android.extension.goToAppSetting


@Composable
fun AskSinglePermission(requiredPermission: NeededPermission, onGrantPermission: () -> Unit, onDismiss: () -> Unit) {
    val activity = LocalContext.current as Activity
    var state by remember { mutableStateOf(activity.permissionState(requiredPermission)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            markPermissionAsked(activity, requiredPermission)
            state = activity.permissionState(requiredPermission)
        }
    )

    LaunchedEffect(state) {
        when (state) {
            PermissionState.NOT_APPLICABLE, PermissionState.GRANTED -> onGrantPermission()
            PermissionState.NOT_ASKED -> permissionLauncher.launch(requiredPermission.permission)
            else -> Unit
        }
    }

    if (state == PermissionState.DENIED || state == PermissionState.PERMANENTLY_DENIED) {
        PermissionAlertDialog(
            neededPermission = requiredPermission,
            isPermissionDeclined = state == PermissionState.PERMANENTLY_DENIED,
            onDismiss = onDismiss,
            onOkClick = { permissionLauncher.launch(requiredPermission.permission) },
            onGoToAppSettingsClick = {
                activity.goToAppSetting()
                onDismiss()
            },
        )
    }
}

@Composable
fun AskPermissions(
    permissionsRequired: List<NeededPermission>,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as Activity
    val applicable = permissionsRequired.applicable()
    var pending by remember { mutableStateOf(applicable.filterNot { hasPermission(activity, it.permission) }) }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            markPermissionsAsked(activity, applicable)
            pending = applicable.filterNot { hasPermission(activity, it.permission) }
            if (pending.isEmpty()) onGrantPermission()
        }
    )

    LaunchedEffect(Unit) {
        val neverAsked = pending.filterNot { wasPermissionAsked(activity, it) }
        if (pending.isEmpty()) onGrantPermission()
        else if (neverAsked.isNotEmpty()) {
            multiplePermissionLauncher.launch(neverAsked.map { it.permission }.toTypedArray())
        }
    }

    val blocking = pending.firstOrNull { wasPermissionAsked(activity, it) } ?: return

    PermissionAlertDialog(
        neededPermission = blocking,
        isPermissionDeclined = activity.isPermanentlyDenied(blocking),
        onDismiss = onDismiss,
        onOkClick = { multiplePermissionLauncher.launch(arrayOf(blocking.permission)) },
        onGoToAppSettingsClick = {
            activity.goToAppSetting()
            onDismiss()
        },
    )
}

/**
 * Check for the single permission granted */
fun hasPermission(context: Context, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check for the permissions granted */
fun hasPermissions(context: Context, permissions: List<NeededPermission>): Boolean =
    permissions.applicable().all { hasPermission(context = context, it.permission) }

fun hasPermission(context: Context, permission: NeededPermission): Boolean =
    !permission.isApplicable || hasPermission(context, permission.permission)

@Composable
private  fun PermissionAlertDialog(
    neededPermission: NeededPermission,
    isPermissionDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(color = Color.LightGray)
                Text(text = if (isPermissionDeclined) "Go to app setting" else "OK",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable {
                    if (isPermissionDeclined) onGoToAppSettingsClick() else onOkClick()
                }.padding(16.dp))
            }
        },
        title = {
            Text(
                text = neededPermission.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = neededPermission.permissionTextProvider(isPermissionDeclined),
            )
        },
    )
}
