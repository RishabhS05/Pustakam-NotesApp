package com.app.pustakam.android.hardware.camera

import android.graphics.Bitmap
import androidx.camera.video.Recording
import androidx.lifecycle.ViewModel
import com.app.pustakam.android.extension.saveBitmapToFile
import com.app.pustakam.util.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import org.koin.core.component.KoinComponent

sealed interface MediaProcessingEvent {
    data object DiscardImage : MediaProcessingEvent
    data object DiscardChanges : MediaProcessingEvent
    data class SaveFilePath(
        val path: String, val contentType: ContentType
    ) : MediaProcessingEvent

    data object CropImage : MediaProcessingEvent
    data object OnSaveMedia : MediaProcessingEvent
}

data class MediaFileStateHandler(
    val filePath: String? = "", val contentType: ContentType? = ContentType.IMAGE, val bitmap: Bitmap? = null, val editedBitmap: Bitmap? = null, val showImagePreview: Boolean = false, val onDismissCameraPreview: Boolean = false
)

class ImageDataViewModel : ViewModel(), KoinComponent {
    //private val _bitmaps =  MutableStateFlow<List<Bitmap>>(emptyList())
//    val bitmaps = _bitmaps.asStateFlow()
    private val _mediaFileState = MutableStateFlow(MediaFileStateHandler())
    val mediaFileState = _mediaFileState.asStateFlow()

    fun onHandleMediaOperation(event: MediaProcessingEvent) {
        when (event) {
            MediaProcessingEvent.CropImage -> {}
            MediaProcessingEvent.OnSaveMedia -> saveImage()

            is MediaProcessingEvent.SaveFilePath -> {
                _mediaFileState.update {
                    it.copy(event.path, event.contentType)
                }
            }

            MediaProcessingEvent.DiscardChanges -> {
                _mediaFileState.update { it.copy(editedBitmap = it.bitmap) }
            }

            MediaProcessingEvent.DiscardImage -> onClear()
        }
    }

    fun onClear() {
        _mediaFileState.update {
            it.copy(filePath = null, contentType = null, bitmap = null, showImagePreview = false, onDismissCameraPreview = true)
        }
    }

    fun onTakePhoto(bitmap: Bitmap) {
//        _bitmaps.value+= bitmap
        _mediaFileState.update { it.copy(bitmap = bitmap, editedBitmap = bitmap, showImagePreview = true) }
    }

    private fun saveImage() {
        val isSaved = _mediaFileState.value.filePath?.let { saveBitmapToFile(_mediaFileState.value.bitmap!!, it) } == true
        if(isSaved){
        onClear()
        }
    }

    var recording: Recording? = null

    fun clearRecording() {
        recording?.stop()
        recording?.close()
        recording = null
    }
}