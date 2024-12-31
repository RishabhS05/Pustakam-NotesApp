package com.app.pustakam.android.screen.noteEditor

import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.notes.CreateORUpdateNoteUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteUseCase
import com.app.pustakam.android.screen.notes.ReadNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.ContentType.AUDIO
import com.app.pustakam.util.ContentType.DOCX
import com.app.pustakam.util.ContentType.GIF
import com.app.pustakam.util.ContentType.IMAGE
import com.app.pustakam.util.ContentType.LINK
import com.app.pustakam.util.ContentType.LOCATION
import com.app.pustakam.util.ContentType.PDF
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

class NoteEditorViewModel : BaseViewModel() {
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()
    private val readNoteUseCase = ReadNoteUseCase()
    private val deleteNoteUseCase = DeleteNoteUseCase()
    private val createUpdateNoteUseCase = CreateORUpdateNoteUseCase()

    fun changeNoteStatus(status: NoteStatus?) {
        _noteUiState.update {
            it.copy(
                noteStatus = status, isLoading = true
            )
        }
    }
    override fun onLoading(taskCode: TaskCode) {
        _noteUiState.update {
            it.copy(isLoading = true)
        }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        when (taskCode) {
            NOTES_CODES.INSERT, NOTES_CODES.UPDATE -> {
                log_d("Loading", "Getting Update data")
                val note = result.data.data as Note
                _noteUiState.update {
                    val noteStatus = if (it.noteStatus == NoteStatus.onBackPress)
                        NoteStatus.onSaveCompletedExit else NoteStatus.onSaveCompleted
                    it.copy(
                        isLoading = false, note = note, isSetupValues = true, noteStatus = noteStatus
                    )
                }
            }

            NOTES_CODES.READ -> {
                val note = result.data.data as Note
                if (!noteUIState.value.isSetupValues) _noteUiState.update {
                    it.titleTextState.value = note.title ?: ""
                    it.copy(
                        titleTextState = it.titleTextState,
                        isLoading = false, note = note, isSetupValues = true,
                    )
                }
            }

            NOTES_CODES.DELETE -> {
                _noteUiState.update {
                    it.copy(
                        isLoading = false, noteStatus = NoteStatus.onSaveCompletedExit
                    )
                }
            }
        }
    }

    fun updateNoteObject() {
        val note = _noteUiState.value.note?.copy(
            title = _noteUiState.value.titleTextState.value
        )
        _noteUiState.update {
            it.copy(note = note)
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

    // call make a wish api
    fun createOrUpdateNote() {
        if(_noteUiState.value.titleTextState.value.isEmpty()){
            if(_noteUiState.value.note?.content?.isEmpty() == true) return
        }
        updateNoteObject()
        makeAWish(NOTES_CODES.INSERT) {
            createUpdateNoteUseCase.invoke(_noteUiState.value.note!!)
        }
    }

    fun deleteNote(noteId: String) {
        makeAWish(NOTES_CODES.DELETE) {
            deleteNoteUseCase.invoke(noteId)
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

    fun showDeleteAlert(value: Boolean) {
        _noteUiState.update { it.copy(showDeleteAlert = value) }
    }

   private fun getPermissions(contentType: ContentType?) = when (contentType) {
        VIDEO -> listOf(NeededPermission.CAMERA, NeededPermission.RECORD_AUDIO)
        AUDIO -> listOf(NeededPermission.RECORD_AUDIO)
        IMAGE -> listOf(NeededPermission.CAMERA)
        LOCATION -> listOf(NeededPermission.COARSE_LOCATION)
        else -> emptyList()
    }

    fun preparePermissionDialog(contentType: ContentType? = null) {
        val permission = getPermissions(contentType)
        _noteUiState.update {
            it.copy(permissions = permission, contentType = contentType, showPermissionAlert = true)
        }
    }

    fun prepareInitialContent() {
        val contentType = _noteUiState.value.contentType
        val note = _noteUiState.value.note
        val position: Long = note?.content?.count()?.toLong() ?: 0
        val noteId = note?.id!!
        val content: NoteContentModel
        when (contentType) {
            TEXT -> {
                content = NoteContentModel.TextContent(position = position, noteId = noteId)
            }

            IMAGE -> {
                content = NoteContentModel.ImageContent(position = position, noteId = noteId)
            }

            VIDEO -> {
                content = NoteContentModel.VideoContent(position = position, noteId = noteId)
            }

            AUDIO -> {
                content = NoteContentModel.AudioContent(position = position, noteId = noteId)
            }

            LINK -> {
                content = NoteContentModel.Link(position = position, noteId = noteId)
            }

            DOCX -> {
                content = NoteContentModel.DocContent(position = position, noteId = noteId)
            }

            LOCATION -> {
                content = NoteContentModel.Location(position = position, noteId = noteId)
            }

            PDF -> {
                content = NoteContentModel.Location(position = position, noteId = noteId)
            }

            DOCX -> {
                content = NoteContentModel.Location(position = position, noteId = noteId)
            }

            GIF -> {
                content = NoteContentModel.Location(position = position, noteId = noteId)
            }

            else -> {
                content = NoteContentModel.Location(position = position, noteId = noteId)
            }
        }
        _noteUiState.update {
            it.note?.content?.add(content)
            it.copy(note = it.note)
        }
    }

    fun showPermissionAlert(value: Boolean?) {
        _noteUiState.update {
            it.copy(showPermissionAlert = value)
        }
    }

    fun setContentType(contentType: ContentType) {
        _noteUiState.update {
            it.copy(contentType = contentType )
        }
    }
}