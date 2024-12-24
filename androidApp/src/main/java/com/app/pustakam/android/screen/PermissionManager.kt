package com.app.pustakam.android.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.permission.getNeededPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun AskSinglePermission(requiredPermission : NeededPermission){
    val activity = LocalContext.current as Activity
    val permissionDialog = remember {
        mutableStateListOf<NeededPermission>()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted)
                permissionDialog.add(requiredPermission)
        }
    )

    PermissionAlertDialog(
        neededPermission = requiredPermission,
        onDismiss = { permissionDialog.remove(requiredPermission) },
        onOkClick = {
            permissionDialog.remove(requiredPermission)
            permissionLauncher.launch(requiredPermission.permission)
        },
        onGoToAppSettingsClick = {
            permissionDialog.remove(requiredPermission)
            activity.goToAppSetting()
        },
        isPermissionDeclined = !activity.shouldShowRequestPermissionRationale(requiredPermission.permission)
    )
}
@SuppressLint("CoroutineCreationDuringComposition", "SuspiciousIndentation")
@Composable
fun AskPermissions(
    permissionsRequired: List<NeededPermission>,
    onGrantPermission: () -> Unit,
) {
    val activity = LocalContext.current as Activity
    val permissionDialog = remember { mutableStateListOf<NeededPermission>() }
    val permissionsString =  permissionsRequired.map { it.permission }.toTypedArray()
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val deniedPermission = permissions.entries
                .firstOrNull { !it.value }
                ?.key
                ?.let { deniedKey -> getNeededPermission(deniedKey) }
            deniedPermission?.let { permissionDialog.add(it) }
        }
    )
    if (hasPermissions(context = activity, permissions = permissionsRequired)) {
        onGrantPermission()
        return
    }
    LaunchedEffect(key1 = Unit) {
        multiplePermissionLauncher.launch(permissionsString)
    }

    permissionDialog.forEach { permission ->
        PermissionAlertDialog(
            neededPermission = permission,
            onDismiss = { permissionDialog.remove(permission) },
            onOkClick = {
                permissionDialog.remove(permission)
                multiplePermissionLauncher.launch(arrayOf(permission.permission))
            },
            onGoToAppSettingsClick = {
                permissionDialog.remove(permission)
                activity.goToAppSetting()
            },
            isPermissionDeclined = !activity.shouldShowRequestPermissionRationale(permission.permission)
        )
    }
}
fun hasPermission(context : Context, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun hasPermissions(context : Context, permissions: List<NeededPermission>): Boolean {
    val permissionsString = permissions.map { it.permission }
    return permissionsString.all { hasPermission(context = context ,it) }
}
@Composable
fun PermissionAlertDialog(
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
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                HorizontalDivider(color = Color.LightGray)
                Text(
                    text = if (isPermissionDeclined) "Go to app setting" else "OK",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPermissionDeclined)
                                onGoToAppSettingsClick()
                            else
                                onOkClick()

                        }
                        .padding(16.dp)
                )
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
fun Activity.goToAppSetting() {
    val i = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(i)
}
@Preview("default")
@Composable
 fun PermissionPreview() {
//         PermissionRequiredScreen( permissions =  listOf(
//             android.Manifest.permission.CAMERA,
//             android.Manifest.permission.RECORD_AUDIO,
//             if (android.os.Build.VERSION.SDK_INT >=
//                 android.os.Build.VERSION_CODES.TIRAMISU) {
//                 android.Manifest.permission.READ_MEDIA_VIDEO
//             } else {
//                 android.Manifest.permission.READ_EXTERNAL_STORAGE
//             }
//         )) {
//
//         }
}
