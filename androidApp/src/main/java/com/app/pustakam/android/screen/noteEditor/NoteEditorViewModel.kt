package com.app.pustakam.android.screen.noteEditor

import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.notes.CreateORUpdateNoteUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteUseCase
import com.app.pustakam.android.screen.notes.ReadNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.log_d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class NoteEditorViewModel : BaseViewModel() {
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()
    private val readNoteUseCase = ReadNoteUseCase()
    private val deleteNoteUseCase = DeleteNoteUseCase()
    private val createUpdateNoteUseCase = CreateORUpdateNoteUseCase()
    fun changeNoteStatus(status: NoteStatus?){
        _noteUiState.update {
            it.copy( noteStatus = status,
                isLoading = true)
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
                    val noteStatus = if (it.noteStatus === NoteStatus.onBackPress)
                        NoteStatus.onSaveCompletedExit else NoteStatus.onSaveCompleted
                    it.copy(
                        isLoading = false, note = note,
                        isSetupValues = true,
                        noteStatus = noteStatus )
                }
            }
            NOTES_CODES.READ -> {
                val note = result.data.data as Note
                _noteUiState.update {
                    it.copy(
                        isLoading = false, note = note, isSetupValues = true,
                    )
                }
            }
            NOTES_CODES.DELETE -> {
                _noteUiState.update {
                    it.copy(isLoading = false,
                    noteStatus = NoteStatus.onSaveCompletedExit)
                }
            }
        }
    }

    fun readFromDataBase(id: String?) {
            makeAWish(NOTES_CODES.READ) {
                readNoteUseCase.invoke(id!!)
            }
    }
    fun createNewNote(categoryId : String = ""): Note{
        val currentDateTime = Clock.System.todayIn(TimeZone.UTC).toString()
        return Note( title = "", createdAt = currentDateTime, updatedAt = currentDateTime,
            isSynced = false,
            categoryId = categoryId, content = emptyList())
    }
     // call make a wish api
     fun createOrUpdateNote(createNote: Note) {

        makeAWish(NOTES_CODES.INSERT) {
            createUpdateNoteUseCase.invoke(createNote)
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
                        isLoading = false,
                        error = (error as NetworkError).getError(),
                        noteStatus = NoteStatus.onSaveCompletedExit
                    )
                }
            }

            else -> _noteUiState.update {
                it.copy(isLoading = false, error = (error as NetworkError).getError() , noteStatus = null)
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
}