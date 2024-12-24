package com.app.pustakam.android.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.permission.getNeededPermission

@Composable
fun Permissions() {

    val activity = LocalContext.current as Activity

    val permissionDialog = remember {
        mutableStateListOf<NeededPermission>()
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted)
                permissionDialog.add(NeededPermission.RECORD_AUDIO)
        }
    )

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            permissions.entries.forEach { entry ->
                if (!entry.value)
                    permissionDialog.add(getNeededPermission(entry.key))
            }
        }
    )


    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        )
    ) {
        Button(
            onClick = {
                microphonePermissionLauncher.launch(NeededPermission.RECORD_AUDIO.permission)
            }
        ) {
            Text(text = "Request bluetooth Permission")
        }

        Button(
            onClick = {

                multiplePermissionLauncher.launch(
                    arrayOf(
                        NeededPermission.RECORD_AUDIO.permission,
                        NeededPermission.CAMERA.permission
                    )
                )
            }
        ) {
            Text(text = "Request multiple Permissions")
        }

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
                Divider(color = Color.LightGray)
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
