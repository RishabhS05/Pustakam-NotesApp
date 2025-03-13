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
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.permission.hasPermission
import com.app.pustakam.android.permission.hasPermissions
import com.app.pustakam.extensions.isNotnull
import java.io.File

@SuppressLint("MissingPermission")
fun recordingVideo(controller: LifecycleCameraController, context: Context, imageDataViewModel: ImageDataViewModel, outputFile : File) {
    if (imageDataViewModel.recording.isNotnull()) {
        imageDataViewModel.clearRecording()
        return
    }
    if (!hasPermissions(context, listOf(NeededPermission.CAMERA, NeededPermission.RECORD_AUDIO))) {
        return
    }
    imageDataViewModel.recording = controller.startRecording(
        FileOutputOptions.Builder(outputFile).build(), AudioConfig.create(true), ContextCompat.getMainExecutor(context)
    ) { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    imageDataViewModel.clearRecording()
                    Toast.makeText(context, "Video capture failed", Toast.LENGTH_LONG).show()
                } else {
                    imageDataViewModel.saveRecordedVideo(outputFile)
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