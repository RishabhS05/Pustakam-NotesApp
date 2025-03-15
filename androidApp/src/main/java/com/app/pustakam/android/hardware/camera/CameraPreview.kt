package com.app.pustakam.android.hardware.camera

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.pustakam.android.fileUtils.createFileWithFolders
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.extensions.isNotnull

import com.app.pustakam.util.ContentType
import com.app.pustakam.util.getCurrentTimestamp

@Composable
fun CameraStreamingScreen(
    imageViewModel: ImageDataViewModel, onBackPress: () -> Unit,
    navigateTo: (Any)-> Unit
) {
    val context = LocalContext.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }
    CameraStreamingPreview(controller, imageViewModel, onBackPress, navigateTo)
}
@Composable
fun CameraStreamingPreview(
    controller: LifecycleCameraController,
    imageViewModel: ImageDataViewModel,
    onBackPress: () -> Unit,
    navigateTo: (Any)-> Unit ,

    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackPress()
    }
    val lifecycle = LocalLifecycleOwner.current
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AndroidView(factory = {
                    PreviewView(it).apply {
                        this.controller = controller
                        controller.bindToLifecycle(lifecycle)
                    }
                }, modifier = Modifier.fillMaxSize())
                IconButton(onClick = {
                    controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
                }, modifier = Modifier.offset(16.dp, 16.dp)) {
                    Icon(imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Camera Switch")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        takePhoto(controller, context, onPhotoTaken ={bitmap->
                            imageViewModel.onTakenPhotoPreview(bitmap)
                            if(bitmap.isNotnull())
                                navigateTo(Route.ImagePreview)
                        })

                    }, modifier = Modifier.offset(16.dp, 16.dp)) {
                        Icon(imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Photo",
                            tint = colorScheme.surfaceTint)
                    }
//                IconButton(onClick = {
//                    scope.launch {
////                        scaffoldState.bottomSheetState.expand()
//                    }
//                }, modifier = Modifier.offset(16.dp, 16.dp)) {
//                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Open Gallery")
//                }
                    IconButton(onClick = {
                        val timeStamp = getCurrentTimestamp()
                        recordingVideo(controller, context, imageViewModel,
                            createFileWithFolders(context as
                                    Activity,
                                "${ContentType.VIDEO.name.lowercase()}/${timeStamp}"
                                , "${timeStamp}${ContentType.VIDEO.getExt()}"))
                    }, modifier = Modifier.offset(16.dp, 16.dp)) {
                        Icon(imageVector = Icons.Default.Videocam,
                            contentDescription = "Recording Video",
                            tint = colorScheme.surfaceTint)
                    }
                }
            }

//    val scaffoldState = rememberBottomSheetScaffoldState()
//    BottomSheetScaffold(scaffoldState = scaffoldState, sheetContent = {
//        PhotoBottomSheetContent(bitmaps = bitmapsState, modifier = Modifier.fillMaxWidth())
//    }) { padding ->
//    }
}