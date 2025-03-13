package com.app.pustakam.android.hardware.camera

import android.graphics.Bitmap
import androidx.camera.video.Recording
import androidx.lifecycle.ViewModel
import com.app.pustakam.android.fileUtils.saveBitmapToFile
import com.app.pustakam.util.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import org.koin.core.component.KoinComponent
import java.io.File


sealed interface MediaProcessingEvent {
    data object DiscardImage : MediaProcessingEvent
    data object DiscardChanges : MediaProcessingEvent
    data class SetNoteId(val noteId : String) : MediaProcessingEvent
    data object CropImage : MediaProcessingEvent
    data class OnSaveImage(val file: File) : MediaProcessingEvent

}

data class MediaFileStateHandler(
    val noteId : String ="",
    val filePath: String= "", val contentType: ContentType? = ContentType.IMAGE,
    val bitmap: Bitmap? = null,
    val editedBitmap: Bitmap? = null,
    val showImagePreview: Boolean = false,
    val onDismissCameraPreview: Boolean = false
)

class ImageDataViewModel : ViewModel(), KoinComponent {
    private val _paths =
        MutableStateFlow<List<Pair<String,ContentType>>>(emptyList())
     val paths =
         _paths.asStateFlow()
    private val _mediaFileState =
        MutableStateFlow(MediaFileStateHandler())
    val mediaFileState =
        _mediaFileState.asStateFlow()

    fun onHandleMediaOperation(event: MediaProcessingEvent) {
        when (event) {
            MediaProcessingEvent.CropImage -> {}
            is MediaProcessingEvent.OnSaveImage -> saveImage(event.file)
            is MediaProcessingEvent.SetNoteId -> {
                _mediaFileState.update {
                    it.copy(noteId = event.noteId)
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
            it.copy(filePath = "",
                contentType = null,
                bitmap = null,
                showImagePreview = false,
                onDismissCameraPreview = true)
        }
        clearRecording()
    }
    fun onTakePhoto(bitmap: Bitmap) {
        _mediaFileState.update { it.copy(bitmap = bitmap, editedBitmap = bitmap, showImagePreview = true) }
    }

    private fun saveImage(file: File) {
        if(saveBitmapToFile(_mediaFileState.value.editedBitmap!!, file)){
            _paths.value+= Pair(file.absolutePath, ContentType.IMAGE)
            _mediaFileState.update { it.copy(showImagePreview = false) }
        }
    }
    var recording: Recording? = null

    fun clearRecording() {
        recording?.stop()
        recording?.close()
        recording = null
    }
    fun clearPaths(){
        _paths.value=emptyList()
    }


 fun saveRecordedVideo(outputFile: File) {
     _paths.value+=  Pair(outputFile.absolutePath, ContentType.VIDEO)
    }
}