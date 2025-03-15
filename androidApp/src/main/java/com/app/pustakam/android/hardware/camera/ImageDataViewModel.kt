package com.app.pustakam.android.hardware.camera

import android.graphics.Bitmap
import androidx.camera.video.Recording
import androidx.lifecycle.ViewModel
import com.app.pustakam.android.fileUtils.saveBitmapToFile
import com.app.pustakam.android.fileUtils.toBitmap
import com.app.pustakam.extensions.isUrl
import com.app.pustakam.util.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import org.koin.core.component.KoinComponent
import java.io.File

sealed interface DataStateEvent {
   data object Saved: DataStateEvent
   data object Editing: DataStateEvent

}
sealed interface MediaProcessingEvent {
    /**
     * Screen Handle Following features
     * F1 : Preview Captured Image -> OnPreviewImage(bitmap/filepath)
     * F2 : Crop Image -> CropImage
     * F3 : Save Image -> OnSaveImage
     * F4 : Discard EditChanges -> DiscardChanges
     * F5 : Draw on Image (pencil tool) -> ActivatePenTool
     * F6 : undo and redo changes on Image. -> UndoAction, RedoAction
     * */
    data object DiscardImage : MediaProcessingEvent
    data object DiscardChanges : MediaProcessingEvent
    data class SetNoteId(val noteId: String) : MediaProcessingEvent
    data object CropImage : MediaProcessingEvent
    data class OnSaveImage(val file: File) : MediaProcessingEvent
    data object ActivatePenTool : MediaProcessingEvent
    data object UndoAction : MediaProcessingEvent
    data object RedoAction : MediaProcessingEvent
    data object EditImage : MediaProcessingEvent
    data class OnPreviewFilepath(val filepath: String, val contentType: ContentType) : MediaProcessingEvent
    data class OnPreviewWithBitmap(val bitmap: Bitmap) : MediaProcessingEvent
}

data class MediaFileStateHandler(
    val noteId: String = "",
    val contentType: ContentType? = ContentType.IMAGE,
    val dataStateEvent : DataStateEvent = DataStateEvent.Saved ,
    val bitmap: Bitmap? = null,
    val editedBitmap: Bitmap? = null,
)

class ImageDataViewModel : ViewModel(), KoinComponent {
    private val _paths =
        MutableStateFlow<List<Pair<String, ContentType>>>(emptyList())
    val paths =
        _paths.asStateFlow()
    private val _mediaFileState =
        MutableStateFlow(MediaFileStateHandler())
    val mediaFileState =
        _mediaFileState.asStateFlow()

    fun onHandleMediaOperation(event: MediaProcessingEvent) {
        when (event) {

            MediaProcessingEvent.CropImage -> {
                _mediaFileState.update { it.copy(dataStateEvent = DataStateEvent.Editing) }
            }
            is MediaProcessingEvent.OnSaveImage ->
                saveImage(event.file)

            is MediaProcessingEvent.SetNoteId -> {
                _mediaFileState.update {
                    it.copy(noteId = event.noteId)
                }
            }

            is MediaProcessingEvent.OnPreviewFilepath ->
                onSetMediaToPreview(event.filepath, event.contentType)
            is MediaProcessingEvent.OnPreviewWithBitmap ->
                onTakenPhotoPreview(event.bitmap)
            MediaProcessingEvent.DiscardChanges -> {
                _mediaFileState.update { it.copy(editedBitmap = it.bitmap, dataStateEvent = DataStateEvent.Editing) }
            }

            MediaProcessingEvent.DiscardImage -> onClear()
            MediaProcessingEvent.ActivatePenTool -> TODO()
            MediaProcessingEvent.RedoAction -> TODO()
            MediaProcessingEvent.UndoAction -> TODO()
            MediaProcessingEvent.EditImage -> {  _mediaFileState.update { it.copy(dataStateEvent = DataStateEvent.Editing) }}
        }
    }

    fun onClear() {
        _mediaFileState.update {
            it.copy(
                contentType = null, bitmap = null,
            )
        }
        clearRecording()
    }

    fun onSetMediaToPreview(fileUrl: String, contentType: ContentType) {
        if (fileUrl.isUrl()) {
            //todo handle later api call
        } else {
            when {
                contentType == ContentType.IMAGE -> {
                    onTakenPhotoPreview(fileUrl.toBitmap(), dataStateEvent = DataStateEvent.Saved)
                }

                contentType == ContentType.VIDEO -> {

                }
            }
        }
    }

    fun onTakenPhotoPreview(bitmap: Bitmap,dataStateEvent: DataStateEvent = DataStateEvent.Editing) {
        _mediaFileState.update {
            it.copy(bitmap = bitmap, editedBitmap = bitmap, dataStateEvent = dataStateEvent)
        }
    }

    private fun saveImage(file: File) {
        if (saveBitmapToFile(_mediaFileState.value.editedBitmap!!, file)) {
            _paths.value += Pair(file.absolutePath, ContentType.IMAGE)
        }
        _mediaFileState.update { it.copy(dataStateEvent = DataStateEvent.Saved) }
    }

    var recording: Recording? = null

    fun clearRecording() {
        recording?.stop()
        recording?.close()
        recording = null
    }

    fun clearPaths() {
        _paths.value = emptyList()
    }


    fun saveRecordedVideo(outputFile: File) {
        _paths.value += Pair(outputFile.absolutePath, ContentType.VIDEO)
    }
}