package com.app.pustakam.android.screen.noteEditor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.app.pustakam.android.extension.createFileWithFolders
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteContentUiState
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.notes.CreateORUpdateNoteUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteUseCase
import com.app.pustakam.android.screen.notes.ReadNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.noteContentRepo.NoteContentRepository
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.ContentType.AUDIO
import com.app.pustakam.util.ContentType.DOCX
import com.app.pustakam.util.ContentType.IMAGE
import com.app.pustakam.util.ContentType.LINK
import com.app.pustakam.util.ContentType.LOCATION
import com.app.pustakam.util.ContentType.TEXT
import com.app.pustakam.util.ContentType.VIDEO
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.getCurrentTimestamp
import com.app.pustakam.util.log_d
import com.app.pustakam.util.log_i
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import org.koin.core.component.get

class NoteEditorViewModel : BaseViewModel() {
    private val noteContentRepository  = get<NoteContentRepository>()
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()
    private val _noteContentUiState = MutableStateFlow(NoteContentUiState())
    val noteContentUiState : StateFlow<NoteContentUiState> = _noteContentUiState.asStateFlow()
    private val readNoteUseCase = ReadNoteUseCase()
    private val deleteNoteUseCase = DeleteNoteUseCase()
    private val createUpdateNoteUseCase = CreateORUpdateNoteUseCase()

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
                    val noteStatus = if (it.noteStatus == NoteStatus.onBackPress)
                        NoteStatus.onSaveCompletedExit else NoteStatus.onSaveCompleted
                    it.copy(noteStatus = noteStatus)
                }

                _noteContentUiState.update { it.copy(note= note, isAllSetupDone = true)}
            }

            NOTES_CODES.READ -> {
                val note = result.data.data as Note
                if (!noteContentUiState.value.isAllSetupDone) _noteContentUiState.update {
                    it.titleTextState.value = note.title ?: ""
                    it.copy(
                        titleTextState = it.titleTextState, note = note,
                        isAllSetupDone = true,
                        contents =  if(note.content.isNotnull())  mutableStateListOf(*note.content!!.toTypedArray()) else mutableStateListOf()
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
        if(_noteContentUiState.value.titleTextState.value.isEmpty()){
            if(_noteContentUiState.value.note?.content?.isEmpty() == true) {
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
    fun setContentType(contentType: ContentType) {
        _noteUiState.update {
            it.copy(contentType = contentType, isLoading = true )
        }
    }

    //dialog trigger methods
    fun showDeleteAlertBox(value: Boolean) {
        // some time it doesn't update a single value
        _noteUiState.update { it.copy(showDeleteAlert = value, isLoading = false) }
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
    // permission dialog setup
    fun preparePermissionDialog(contentType: ContentType? = null) {
        val permission = getPermissions(contentType)
        _noteUiState.update {
            it.copy(permissions = permission, contentType = contentType, showPermissionAlert = true, isLoading = false)
        }
    }

    /**content logic
     * Add new content to the note content list
     * by selecting it type on the bases of user selection
     * */
    fun addNewContent(context : Context) {
        val contentType = _noteUiState.value.contentType!!
        val note = _noteContentUiState.value.note
        val position: Long = note?.content?.count()?.toLong() ?: 0
        val noteId = note?.id?: ""
        val content: NoteContentModel
        val folderName = "${contentType.name.lowercase()}/${getCurrentTimestamp()}"
        val fileName = "${getCurrentTimestamp()}${contentType.getExt()}"
        val filePath = createFileWithFolders(context as Activity,folderName,fileName).absolutePath
        log_i("${filePath} is succefully created")

        when (contentType) {
            TEXT -> {
                content = NoteContentModel.TextContent(position = position,
                    noteId = noteId,)
            }

            IMAGE -> {
                content = NoteContentModel.ImageContent(position = position,
                    noteId = noteId, localPath = filePath)
            }

            VIDEO -> {
                content = NoteContentModel.VideoContent(position = position,
                    noteId = noteId, localPath = filePath)
            }

            AUDIO -> {
                content = NoteContentModel.AudioContent(position = position,
                    noteId = noteId, localPath = filePath)
            }

            LINK -> {
                content = NoteContentModel.Link(position = position,
                    noteId = noteId)
            }

            DOCX -> {
                content = NoteContentModel.DocContent(position = position,
                    noteId = noteId)
            }

            LOCATION -> {
                content = NoteContentModel.Location(position = position,
                    noteId = noteId)
            }

            else -> {
                content = NoteContentModel.Location(position = position,
                    noteId = noteId)
            }
        }
        _noteContentUiState.update {
            it.note?.content?.add(content)
            it.contents.add(content)
                 it.copy(note= it.note,
                     contents= it.contents,
                     isAllSetupDone = true )
        }
        if(content.isMediaFile()) noteContentRepository.addNoteContent(content)
    }
    fun updateContent(index: Int, updatedContent: NoteContentModel) {
        _noteContentUiState.update {
        it.contents.set(index, updatedContent)
            it.note?.content?.set(index, updatedContent)
            it.copy(note =  it.note)
        }
        if(updatedContent.isMediaFile()) noteContentRepository.updateNoteContent(index,updatedContent)
    }
    fun removeContent(value: NoteContentModel) {
        _noteContentUiState.update {
            it.contents.remove(value)
            it.note?.content?.remove(value)
            it.copy(note =  it.note)
        }
    }

    override fun onCleared() {
        super.onCleared()
        noteContentRepository.clear()
    }
}