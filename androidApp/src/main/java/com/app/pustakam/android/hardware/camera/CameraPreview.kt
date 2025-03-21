package com.app.pustakam.android.hardware.camera

import Gray4
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import baseWhite
import com.app.pustakam.android.R
import com.app.pustakam.android.fileUtils.createFileWithFolders
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.audio.RecordingTimer
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.getCurrentTimestamp
import orange30

@Composable
fun CameraStreamingScreen(
    imageViewModel: ImageDataViewModel, onBackPress: () -> Unit, navigateTo: (Any) -> Unit
) {
    val context = LocalContext.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }
    CameraStreamingPreview(controller, imageViewModel, onBackPress, navigateTo)
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun CameraStreamingPreview(
    controller: LifecycleCameraController,
    imageViewModel: ImageDataViewModel,
    onBackPress: () -> Unit,
    navigateTo: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackPress()
    }
    val lifecycle = LocalLifecycleOwner.current
    val context = LocalContext.current
    val elapsedTime = remember { mutableLongStateOf(0) }
    val isRecording = imageViewModel.isRecording.collectAsState()
    val iconSize = 100.dp
val  (timerColor: Color, backgroundColor : Color ) =
    if(isRecording.value){
    (orange30 to baseWhite.copy(alpha = .3f))
}else{
    elapsedTime.value = 0
    (baseWhite to Gray4.copy(alpha = .5f))
}
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycle)
            }
        }, modifier = Modifier.fillMaxSize())
        RecordingTimer(
            isTimerRunning = isRecording.value,
            elapsedTime = elapsedTime,
            style = typography.headlineSmall.copy(color = timerColor, fontWeight = FontWeight.Black),
            modifier = Modifier.padding(vertical = 25.dp,horizontal = 6.dp).align(Alignment.TopCenter)
                .background(color =backgroundColor).padding(horizontal = 8.dp,
                    vertical = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(color = Gray4.copy(alpha = .5f)).align(Alignment.BottomCenter).padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {
                val timeStamp = getCurrentTimestamp()
                recordingVideo(
                    controller, context, imageViewModel, createFileWithFolders(
                        context as Activity, "${ContentType.VIDEO.name.lowercase()}/${timeStamp}", "${timeStamp}${ContentType.VIDEO.getExt()}"
                    )
                )
                imageViewModel.startOrStopRecording(!isRecording.value)

            }, modifier = Modifier) {
                Icon(
                    painter = painterResource(
                        if (isRecording.value) R.drawable.ic_stop else R.drawable.ic_record,
                    ), modifier = Modifier.size(100.dp),
                    contentDescription = "Recording Video",
                    tint =timerColor
                )
            }
            IconButton(onClick = {
                takePhoto(controller, context, onPhotoTaken = { bitmap ->
                    imageViewModel.onTakenPhotoPreview(bitmap)
                    if(isRecording.value) {
                            val timeStamp = getCurrentTimestamp()
                            imageViewModel.onHandleMediaOperation(MediaProcessingEvent.OnSaveImage(
                                createFileWithFolders(context as Activity,
                                    "${ContentType.IMAGE.name.lowercase()}/${timeStamp}"
                                    , "${timeStamp}${ContentType.IMAGE.getExt()}")
                            ))
                    } else if (bitmap.isNotnull()) navigateTo(Route.ImagePreview)
                })

            }, modifier = Modifier) {
                Icon(
                    imageVector = Icons.Filled.Camera, modifier = Modifier.size(iconSize), contentDescription = "Take Photo", tint = baseWhite
                )
            }
//                IconButton(onClick = {
//                    scope.launch {
////                        scaffoldState.bottomSheetState.expand()
//                    }
//                }, modifier = Modifier.offset(16.dp, 16.dp)) {
//                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Open Gallery")
//                }

            IconButton(onClick = {
                controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            }, modifier = Modifier) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    modifier = Modifier.size(iconSize),
                    contentDescription = "Camera Switch",
                    tint = baseWhite
                )
            }
        }
    }

//    val scaffoldState = rememberBottomSheetScaffoldState()
//    BottomSheetScaffold(scaffoldState = scaffoldState, sheetContent = {
//        PhotoBottomSheetContent(bitmaps = bitmapsState, modifier = Modifier.fillMaxWidth())
//    }) { padding ->
//    }
}