package com.app.pustakam.android.hardware.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.permission.hasPermission
import com.app.pustakam.android.permission.hasPermissions
import com.app.pustakam.extensions.isNotnull
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreview(
    controller: LifecycleCameraController, modifier: Modifier = Modifier
) {
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageViewModel: ImageDataViewModel = viewModel<ImageDataViewModel>()
    val bitmapsState by imageViewModel.bitmaps.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(scaffoldState = scaffoldState, sheetContent = {
        PhotoBottomSheetContent(bitmaps = bitmapsState, modifier = Modifier.fillMaxWidth())
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = {
                PreviewView(it).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycle)
                }
            }, modifier = Modifier.fillMaxSize())
            IconButton(onClick = {
                controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            }, modifier = Modifier.offset(16.dp, 16.dp)) {
                Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Camera Switch")
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    takePhoto(controller, context, onPhotoTaken = imageViewModel::onTakePhoto)
                }, modifier = Modifier.offset(16.dp, 16.dp)) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Take Photo")
                }
                IconButton(onClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                }, modifier = Modifier.offset(16.dp, 16.dp)) {
                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Open Gallery")
                }
                IconButton(onClick = {
                    recordingVideo(controller, context, imageViewModel)
                }, modifier = Modifier.offset(16.dp, 16.dp)) {
                    Icon(imageVector = Icons.Default.Videocam, contentDescription = "Recording Video")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun recordingVideo(controller: LifecycleCameraController, context: Context, imageDataViewModel: ImageDataViewModel) {

    if (imageDataViewModel.recording.isNotnull()) {
        imageDataViewModel.recording!!.stop()
        imageDataViewModel.recording = null
        return
    }
    if (!hasPermissions(context, listOf(NeededPermission.CAMERA, NeededPermission.RECORD_AUDIO))) {
        return
    }
    //todo change with specific path
    val outputFile = File(context.filesDir, "my-recording.mp4")
    imageDataViewModel.recording = controller.startRecording(
        FileOutputOptions.Builder(outputFile).build(), AudioConfig.create(true), ContextCompat.getMainExecutor(context)
    ) { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    imageDataViewModel.recording?.close()
                    imageDataViewModel.recording = null
                    Toast.makeText(context, "Video capture failed", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Video capture successfully", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

fun takePhoto(
    controller: LifecycleCameraController, context: Context, onPhotoTaken: (Bitmap) -> Unit
) {
    if (!hasPermission(context, NeededPermission.CAMERA.permission)) {
        return
    }
    controller.takePicture(ContextCompat.getMainExecutor(context), object : OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            val matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
                if (controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) postScale(-1f, 1f)
            }
            val rotatedBitmap = Bitmap.createBitmap(
                image.toBitmap(), 0, 0, image.width, image.height, matrix, true
            )
            onPhotoTaken(rotatedBitmap)

        }

        override fun onError(exception: ImageCaptureException) {
            super.onError(exception)
            Log.e("ImageCapture", "Couldn't take photo $exception")
        }
    })

}

fun openGallery(

) {
}