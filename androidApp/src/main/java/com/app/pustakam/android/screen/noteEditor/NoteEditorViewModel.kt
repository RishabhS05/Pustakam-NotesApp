package com.app.pustakam.android.screen.noteEditor

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.collectAsState
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
import com.app.pustakam.android.screen.notes.CreateORUpdateNoteUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteContentUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteUseCase
import com.app.pustakam.android.screen.notes.ReadNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.get

class NoteEditorViewModel : BaseViewModel() {
    private val noteContentRepository = get<NoteContentRepository>()
    private val readNoteUseCase = ReadNoteUseCase()
    private val deleteNoteUseCase = DeleteNoteUseCase()
    private val deleteNoteContentUseCase = DeleteNoteContentUseCase()
    private val createUpdateNoteUseCase = CreateORUpdateNoteUseCase()
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
                        contents = if (note.contents.isNotnull())
                            mutableStateListOf(*note.contents!!.toTypedArray())
                        else mutableStateListOf()
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
        _noteContentUiState.value.note?.copy(
            title = _noteContentUiState.value.titleTextState.value
        )
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
        LOCATION -> listOf(NeededPermission.COARSE_LOCATION)
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
                  NoteContentModel.TextContent(
                      noteId = note.id!!,
                      position = note.contents?.count()?.toLong() ?: 0
                  )
              setContentType(TEXT)
              updateContent(content =textContent)
          }
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
            it.note?.contents?.add(content)
            it.contents.add(content)
            it.copy(note = it.note, contents = it.contents, isAllSetupDone = true)
        }
    }

    fun startStopAudioRecording(value: Boolean = true) {
        _noteUiState.update { it.copy(isLoading = false, showAudioRecorder = value) }
    }
/**
 * Remove a note content for note
 * */
    fun updateContent(index: Int = -1, content: NoteContentModel) {
        if(index== -1) {
            addContentData(content)
        }else{
        _noteContentUiState.update {
            it.contents[index] = content
            it.note?.contents?.set(index, content)
            it.copy(note = it.note,contents = it.contents, isAllSetupDone = true)
        }
        }
        if (content.isPlayingMedia())
            noteContentRepository.updateNoteContent(content as NoteContentModel.MediaContent)
    }
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
            val index= it.note?.contents?.indexOf(find)
            val indexContent = it.contents.indexOf(find)
            if( indexContent != -1 )   it.contents.removeAt(indexContent)
          if(index.isNotnull() && index != -1 )  it.note?.contents?.removeAt(index!!)
            it.copy(note = it.note, contents = it.contents)
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
              if(!it.note.isNotnull()) return
              val position: Long = it.note?.contents?.count()?.toLong() ?: 0
              val content = NoteContentModel.MediaContent(position = position,
                  title = "$${path.second}-$position",
                  noteId =it.note!!.id!!, localPath = path.first , type = path.second)
              it.note.contents?.add(content)
              it.contents.add(content)
              it.copy(note = it.note, contents = it.contents, isAllSetupDone = true)
          }
      }
    }

}