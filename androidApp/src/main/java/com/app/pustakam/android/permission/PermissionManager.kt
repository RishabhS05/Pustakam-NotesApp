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
import androidx.compose.runtime.mutableStateListOf
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
    var permissionDialog by remember { mutableStateOf<NeededPermission?>(requiredPermission) }
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = { isGranted ->
        permissionDialog = if (!isGranted) requiredPermission else null
    })
    PermissionAlertDialog(neededPermission = requiredPermission, onDismiss = {
        permissionDialog = null
        onDismiss()
    }, onOkClick = {
        permissionDialog = null
        permissionLauncher.launch(requiredPermission.permission)
        onDismiss()
    }, onGoToAppSettingsClick = {
        permissionDialog = null
        onGrantPermission()
        activity.goToAppSetting()
    }, isPermissionDeclined = !activity.shouldShowRequestPermissionRationale(requiredPermission.permission)
    )
}

@Composable
fun AskPermissions(
    permissionsRequired: List<NeededPermission>,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as Activity
    /**Add Permission Dialog for added all the permissions */
    val permissionDialog = remember { mutableStateListOf<NeededPermission>().apply { addAll(permissionsRequired) } }
    /** convert all the permission into array */
    val permissionsString = permissionsRequired.map { it.permission }.toTypedArray()
   /** launch multiple permission */
    val multiplePermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
        // some time it does not called we need to trigger manually
    })
    // launch the permissions dialog
    LaunchedEffect(key1 = Unit) {
        multiplePermissionLauncher.launch(permissionsString)
    }
    /** check all the permissions granted or not
     * -if granted -> call hardware launch
     * -if not -> call show premission dialog
     * */
    if (hasPermissions(context = activity, permissions = permissionsRequired)) {
        onGrantPermission()
        return
    } else {
        permissionDialog.forEach {
            if (hasPermission(activity, it.permission)) {
                permissionDialog.remove(it)
            }
        }
    }

    // if all the dialogs displayed it will reset the trigger
    if (permissionDialog.isEmpty()) {
        onDismiss()
    }
    // Display dialogs
    permissionDialog.forEach { permission ->
        PermissionAlertDialog(neededPermission = permission, onDismiss = {
            permissionDialog.remove(permission)
        }, onOkClick = {
            permissionDialog.remove(permission)
            multiplePermissionLauncher.launch(arrayOf(permission.permission))
        }, onGoToAppSettingsClick = {
            permissionDialog.remove(permission)
            activity.goToAppSetting()
        }, isPermissionDeclined = !activity.shouldShowRequestPermissionRationale(permission.permission)
        )
    }
}

/**
 * Check for the single permission granted */
fun hasPermission(context: Context, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check for the permissions granted */
fun hasPermissions(context: Context, permissions: List<NeededPermission>): Boolean {
    val permissionsString = permissions.map { it.permission }
    return permissionsString.all { hasPermission(context = context, it) }
}

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
