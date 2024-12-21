package com.app.pustakam.android.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequiredScreen(
    permissions : List<String> = listOf(),
    modifier: Modifier = Modifier,
    onPermissionGranted : ()-> Unit,

    ) {
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionGranted()
            showRationaleDialog =false
        }
        else {
            showRationaleDialog = true
        }
    }
    if (showRationaleDialog) {
    AlertDialog(onDismissRequest = { showRationaleDialog = false },
        title = { Text("Permission Required") },
        text = {
            Text(
                "This app requires Camera, Microphone, and Storage permissions to " +
                        "function properly. Please grant these permissions."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                showRationaleDialog = false
                permissionState.launchMultiplePermissionRequest()
            }) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = { showRationaleDialog = false }) {
                Text("Cancel")
            }
        }
    )}
    Column(
        modifier = modifier.fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permissionState.allPermissionsGranted) {
            Text(text = "All permissions granted!", color = Color.Green)
        } else {
            Text(text = "Permissions are required to use this feature.", color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text(text = "Request Permissions")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show rationale if necessary
        permissionState.revokedPermissions.forEach { permission ->
            if (permission.status.shouldShowRationale) {
                Column {
                    Text(
                        text = "Permission ${permission.permission} is required for this feature.",
                        color = Color.Red
                    )
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text(text = "Open Settings")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
 fun PermissionPreview() {
         PermissionRequiredScreen( permissions =  listOf(
             android.Manifest.permission.CAMERA,
             android.Manifest.permission.RECORD_AUDIO,
             if (android.os.Build.VERSION.SDK_INT >=
                 android.os.Build.VERSION_CODES.TIRAMISU) {
                 android.Manifest.permission.READ_MEDIA_VIDEO
             } else {
                 android.Manifest.permission.READ_EXTERNAL_STORAGE
             }
         )) {

         }
}
