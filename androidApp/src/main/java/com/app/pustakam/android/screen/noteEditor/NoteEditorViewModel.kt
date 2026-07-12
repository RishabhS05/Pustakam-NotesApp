package com.app.pustakam.android.screen.noteEditor

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.fileUtils.deleteFile
import com.app.pustakam.android.noteContentProvider.addContent
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteContentUiState
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.domain.repositories.usecases.CreateORUpdateNoteUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteNoteContentUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteNoteUseCase
import com.app.pustakam.domain.repositories.usecases.ReadNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.ContentType.AUDIO
import com.app.pustakam.util.ContentType.IMAGE
import com.app.pustakam.util.ContentType.LOCATION
import com.app.pustakam.util.ContentType.TEXT
import com.app.pustakam.util.ContentType.VIDEO
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.log_d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject

class NoteEditorViewModel : BaseViewModel() {
    private val noteContentRepository = get<NoteContentRepository>()
    private val readNoteUseCase by inject<ReadNoteUseCase>()
    private val deleteNoteUseCase by inject<DeleteNoteUseCase>()
    private val deleteNoteContentUseCase by inject<DeleteNoteContentUseCase>()
    private val createUpdateNoteUseCase by inject<CreateORUpdateNoteUseCase>()
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()
    private val _noteContentUiState = MutableStateFlow(NoteContentUiState())
    val noteContentUiState: StateFlow<NoteContentUiState> = _noteContentUiState.asStateFlow()


    //default methods
    override fun onLoading(taskCode: TaskCode) {
        _noteUiState.update {
            it.copy(isLoading = true, successMessage = "")
        }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        when (taskCode) {
            NOTES_CODES.INSERT -> {
                log_d("Loading", "Getting Update data")
                val note = result.data.data as Note
                _noteUiState.update {
                    val noteStatus = if (it.noteStatus == NoteStatus.onBackPress) NoteStatus.onSaveCompletedExit else NoteStatus.onSaveCompleted
                    it.copy(noteStatus = noteStatus)
                }

                _noteContentUiState.update { it.copy(note = note,
                    isAllSetupDone = true) }
            }

            NOTES_CODES.READ -> {
                val note = result.data.data as Note
                if (!noteContentUiState.value.isAllSetupDone) _noteContentUiState.update {
                    it.titleTextState.value = note.title ?: ""
                    it.copy(
                        titleTextState = it.titleTextState, note = note,
                        isAllSetupDone = true,
                        contents = mutableStateListOf(*note.contents.toTypedArray())
                    )
                }
                noteContentRepository.addAllNoteContent(note)
            }

            NOTES_CODES.DELETE -> {
                _noteUiState.update {
                    it.copy(
                        isLoading = false, noteStatus = NoteStatus.exit
                    )
                }
            }
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        when (taskCode) {
            NOTES_CODES.READ -> {
                _noteUiState.update {
                    it.copy(
                        isLoading = false, error = (error as NetworkError).getError(), noteStatus = NoteStatus.onSaveCompletedExit
                    )
                }
            }

            else -> _noteUiState.update {
                it.copy(isLoading = false, error = (error as NetworkError).getError(), noteStatus = null)
            }
        }
    }

    override suspend fun logoutUserForcefully() {
        createUpdateNoteUseCase.logoutUser()
    }

    override fun clearError() {
        _noteUiState.update {
            it.copy(
                error = null, successMessage = null, isLoading = false
            )
        }
    }

    /** CRUD operations on Notes */

    // call make a wish api
    fun createOrUpdateNote() {
        if (_noteContentUiState.value.titleTextState.value.isEmpty()) {
            if (_noteContentUiState.value.note?.contents?.isEmpty() == true) {
                changeNoteStatus(NoteStatus.exit)
                return
            }
        }
        updateNoteObject()
        makeAWish(NOTES_CODES.INSERT) {
            createUpdateNoteUseCase.invoke(_noteContentUiState.value.note!!)
        }
    }

    fun readFromDataBase(id: String?) {
        if (!id.isNullOrEmpty()) _noteUiState.update {
            it.copy(showDeleteButton = true)
        }
        makeAWish(NOTES_CODES.READ) {
            readNoteUseCase.invoke(id)
        }
    }

    fun deleteNote(noteId: String) {
        makeAWish(NOTES_CODES.DELETE) {
            deleteNoteUseCase.invoke(noteId)
        }
    }

    private fun updateNoteObject() {
        // old code built the copy and DISCARDED it — title was never saved.
        // Now: write title + materialize the edited contents back into state before upsert.
        _noteContentUiState.update {
            val updatedNote = it.note?.withTitleAndContents(
                newTitle = it.titleTextState.value,
                newContents = it.contents.toList()
            )
            it.copy(note = updatedNote)
        }
    }


    //action status methods
    fun changeNoteStatus(status: NoteStatus?) {
        _noteUiState.update {
            it.copy(
                noteStatus = status, isLoading = false
            )
        }
    }

    private fun setContentType(contentType: ContentType) {
        _noteUiState.update {
            it.copy(contentType = contentType, isLoading = true)
        }
    }

    //dialog trigger methods
    fun showDeleteAlertBox(value: Boolean, deleteNoteContentId: String? = null) {
        // some time it doesn't update a single value
        _noteUiState.update { it.copy(showDeleteAlert = value, deleteNoteContentId = deleteNoteContentId, isLoading = false) }
    }

    fun showPermissionAlert(value: Boolean?) {
        _noteUiState.update {
            it.copy(showPermissionAlert = value)
        }
    }

    //hardware permission logic
    @SuppressLint("NewApi")
    private fun getPermissions(contentType: ContentType?) = when (contentType) {
        VIDEO -> listOf(NeededPermission.CAMERA, NeededPermission.RECORD_AUDIO)
        AUDIO -> listOf(NeededPermission.RECORD_AUDIO)
        IMAGE -> listOf(NeededPermission.CAMERA)
        LOCATION -> listOf(NeededPermission.COARSE_LOCATION, NeededPermission.FINE_LOCATION,
            NeededPermission.BACKGROUND_LOCATION)
        else -> listOf(NeededPermission.POST_NOTIFICATIONS)
    }

   /**permission dialog setup*/
    fun preparePermissionDialog(contentType: ContentType? = null) {
        val permission = getPermissions(contentType)
        _noteUiState.update {
            it.copy(permissions = permission, contentType = contentType, showPermissionAlert = true, isLoading = false)
        }
    }
      fun addNewText() {
          val note = _noteContentUiState.value.note!!
          if (note.isNotnull()) {
              val textContent =
                  NoteContentObjectHelper.createText(
                      noteId = note.id,
                      positionedAt = note.contents.count().toDouble()   // 🔧 C4
                  )
              setContentType(TEXT)
              updateContent(content = textContent)
          }
      }

    fun locationState(value: Boolean ) {
        _noteUiState.update { it.copy(LocationState = value) }
    }
    fun updateContent(index: Int = -1, content: NoteContentModel) {
        if(index== -1) {
            addContentData(content)
        }else{
            _noteContentUiState.update {
                it.contents[index] = content
                // immutable Note: upsert by id via copy (was: in-place add — also fixed
                // the old bug of ADDING a duplicate on update instead of replacing)
                val updatedNote = it.note?.let { n ->
                    val newContents = n.contents.toMutableList()
                    val i = newContents.indexOfFirst { c -> c.id == content.id }
                    if (i != -1) newContents[i] = content else newContents.add(content)
                    n.withContents(newContents)
                }
                it.copy(note = updatedNote, contents = it.contents, isAllSetupDone = true)
            }
        }
        if (content.isPlayingMedia())
            noteContentRepository.updateNoteContent(content as NoteContentModel.MediaContent)
    }
    /**content logic
     * Add new content to the note content list
     * by selecting it type on the bases of user selection
     * */
    fun addNewContent(context: Context, contentType: ContentType): NoteContentModel {
        setContentType(contentType)
        val content = addContent( context = context, note = _noteContentUiState.value.note!!, contentType = contentType)
        return content
    }
    fun addContentData(content: NoteContentModel){
        _noteContentUiState.update {
            it.contents.add(content)
            val updatedNote = it.note?.let { n -> n.withContents(n.contents + content) }
            it.copy(note = updatedNote, contents = it.contents, isAllSetupDone = true)
        }
    }

    fun startStopAudioRecording(value: Boolean = true) {
        _noteUiState.update { it.copy(isLoading = false, showAudioRecorder = value) }
    }
/**
 * Remove a note content for note
 * */
    fun removeContent(value: String) {
        val find = _noteContentUiState.value.note?.contents?.find { value == it.id }

        if (find?.isMediaFile() == true) {
            find as NoteContentModel.MediaContent
            find.localPath?.let { deleteFile(filePath = it) }
        }
        viewModelScope.launch {
            deleteNoteContentUseCase.invoke(value)
        }
        _noteContentUiState.update {
            val indexContent = it.contents.indexOf(find)
            if (indexContent != -1) it.contents.removeAt(indexContent)
            // immutable Note: rebuild contents without the removed item.
            // (old code removed from a discarded copy — note.contents was never
            //  actually updated; this also fixes that silent bug)
            val updatedNote = it.note?.let { n ->
                n.withContents(n.contents.filterNot { c -> c.id == value })
            }
            it.copy(note = updatedNote, contents = it.contents)
        }
        showDeleteAlertBox(false, null)
    }


 /**
  * Share note with others
  */
    fun shareNote(){}

    override fun onCleared() {
        super.onCleared()
        noteContentRepository.clear()
    }

    fun getMediaData(list: List<Pair<String, ContentType>>) {
         if(list.isEmpty()) return
      list.forEach{ path ->
          _noteContentUiState.update {
              val note = it.note ?: return
              val position: Double = note.contents.count().toDouble()   // 🔧 C4
              val content = NoteContentObjectHelper.createMedia(positionedAt = position,
                  noteId = note.id,
                  localPath = path.first ,
                  contentType = path.second)
                  .copy(title ="${path.second}-$position",)
              it.contents.add(content)
              it.copy(note = note.withContents(note.contents + content),
                  contents = it.contents, isAllSetupDone = true)
          }
      }
    }
}