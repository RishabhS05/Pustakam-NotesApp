package com.app.pustakam.android.hardware.camera

import android.graphics.Bitmap
import androidx.camera.video.Recording
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.extension.toBitmap
import com.app.pustakam.android.fileUtils.saveBitmapToFile
import com.app.pustakam.extensions.isUrl
import com.app.pustakam.util.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
     * F7 : EditingImage enabled/disabled. -> EditImage
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
    val mediaFilePath :String = "",
    // 🔧 14-Jul-2026: NEW — id of the note content being previewed; lets VideoPreviewScreen drive
    //   the standalone player by mediaId instead of guessing from the file path.
    val mediaId: String? = null,
)

class ImageDataViewModel : ViewModel(), KoinComponent {

//TODO
    //TODO remove path create MediaContent directly and add it in to the list as we have noteId here
    private val _paths =
        MutableStateFlow<List<Pair<String, ContentType>>>(emptyList())
    val paths =
        _paths.asStateFlow()
    private val _mediaFileState =
        MutableStateFlow(MediaFileStateHandler())
    val mediaFileState =
        _mediaFileState.asStateFlow()

   private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()
    var recording: Recording? = null
    fun onHandleMediaOperation(event: MediaProcessingEvent) {
        when (event) {

            MediaProcessingEvent.CropImage -> {
                _mediaFileState.update { it.copy(dataStateEvent = DataStateEvent.Editing) }
            }
            is MediaProcessingEvent.OnSaveImage -> saveImage(event.file)

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

    // 🔧 14-Jul-2026: CHANGED — optional mediaId travels with the preview request (see
    //   MediaFileStateHandler.mediaId). Existing callers without an id keep compiling.
    fun onSetMediaToPreview(fileUrl: String, contentType: ContentType, mediaId: String? = null) {
        if (fileUrl.isUrl()) {
            //todo handle later api call
        } else {
            when {
                // 🔧 14-Jul-2026: PERF — decode the (downsampled) bitmap on Dispatchers.IO so
                //   opening an image no longer blocks the main thread. State is updated on
                //   completion; StateFlow.update is thread-safe.
                contentType == ContentType.IMAGE -> viewModelScope.launch(Dispatchers.IO) {
                    val bitmap = fileUrl.toBitmap()
                    onTakenPhotoPreview(bitmap, dataStateEvent = DataStateEvent.Saved)
                }
                contentType == ContentType.VIDEO -> onVideoFullPreview(fileUrl, mediaId = mediaId)
//                contentType == ContentType.AUDIO -> onAudioFilePreview(fileUrl)
            }
        }
    }

//    private fun onAudioFilePreview(fileUrl: String, dataStateEvent: DataStateEvent = DataStateEvent.Saved) {
//        _mediaFileState.update {
//            it.copy(mediaFilePath = fileUrl,dataStateEvent = dataStateEvent, contentType = ContentType.AUDIO)
//        }
//    }

    private fun onVideoFullPreview(fileUrl : String, dataStateEvent: DataStateEvent= DataStateEvent.Saved, mediaId: String? = null ) {
        _mediaFileState.update {
            // 🔧 14-Jul-2026: carries mediaId through to VideoPreviewScreen
            it.copy(mediaFilePath=fileUrl,dataStateEvent = dataStateEvent, contentType = ContentType.VIDEO, mediaId = mediaId)
        }
    }
    fun onTakenPhotoPreview(bitmap: Bitmap,dataStateEvent: DataStateEvent = DataStateEvent.Editing) {
        _mediaFileState.update {
            it.copy(bitmap = bitmap, editedBitmap = bitmap, dataStateEvent = dataStateEvent, contentType = ContentType.IMAGE)
        }
    }

    // 🔧 14-Jul-2026: PERF — compress+write the bitmap to disk on Dispatchers.IO instead of the
    //   main thread. The path is published to _paths only after a successful write.
    private fun saveImage(file: File) {
        val bitmap = _mediaFileState.value.editedBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (saveBitmapToFile(bitmap, file)) {
                _paths.value += Pair(file.absolutePath, ContentType.IMAGE)
            }
            _mediaFileState.update { it.copy(dataStateEvent = DataStateEvent.Saved) }
        }
    }



    fun clearRecording() {
        recording?.stop()
        recording?.close()
        recording = null
        // 🔧 14-Jul-2026: FIX — the VM owns the recording flag now (the View used to toggle it
        //   blindly, drifting out of sync when permission was denied or recording failed).
        _isRecording.value = false
    }

    fun clearPaths() {
        _paths.value = emptyList()
    }

    fun saveRecordedVideo(outputFile: File) {
        _paths.value += Pair(outputFile.absolutePath, ContentType.VIDEO)
        _isRecording.value = false   // 🔧 14-Jul-2026: recording finished — reflect it in state
    }

    fun startOrStopRecording(value : Boolean) {
        _isRecording.value = value
    }
}