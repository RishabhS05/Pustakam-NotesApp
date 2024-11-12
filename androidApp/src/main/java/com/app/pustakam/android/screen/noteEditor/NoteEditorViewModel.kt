package com.app.pustakam.android.screen.noteEditor

import com.app.pustakam.android.screen.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.notes.CreateNoteUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteUseCase
import com.app.pustakam.android.screen.notes.ReadNoteUseCase
import com.app.pustakam.android.screen.notes.UpdateNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.checkAnyUpdateOnNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NoteEditorViewModel : BaseViewModel() {
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()
    private val readNoteUseCase = ReadNoteUseCase()
    private val deleteNoteUseCase = DeleteNoteUseCase()
    private val updateNoteUseCase = UpdateNoteUseCase()
    private val createNoteUseCase = CreateNoteUseCase()
    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        when (taskCode) {
            NOTES_CODES.INSERT, NOTES_CODES.READ, NOTES_CODES.UPDATE -> {
                val note = result.data.data as Note
                _noteUiState.update { it.copy(isLoading = false, note = note, isSetupValues = true ) }
            }

            NOTES_CODES.DELETE -> {
                _noteUiState.update { it.copy(isLoading = false, moveBack = true) }
            }
        }
    }
    fun readFromDataBase(id: String) {
        val note = readNoteUseCase.invoke(id)
        if (note.isNotnull()) {
            _noteUiState.update { it.copy(isLoading = false, note = note, isSetupValues = true) }
        }else{
        makeAWish(NOTES_CODES.READ) {
            readNoteUseCase.invoke(id, callApi = true)
        } }
    }

    fun createOrUpdate(id: String?, title: String?, body: String?, note: Note? = null) {
        val createOrUpdateNote = NoteRequest(title = title, body = body, _id = id)
        if (!id.isNullOrEmpty() && note.isNotnull()) updateNote(createOrUpdateNote, note!!)
        else createNote(createOrUpdateNote)
    }

    private fun createNote(createNote: NoteRequest) {
        makeAWish(NOTES_CODES.INSERT) {
            createNoteUseCase.invoke(createNote)
        }
    }

    private fun updateNote(updateNote: NoteRequest, note: Note) {
        if (checkAnyUpdateOnNote(new = updateNote, old = note)) return
        makeAWish(NOTES_CODES.UPDATE) {q
            updateNoteUseCase.invoke(updateNote)
        }
    }

    fun deleteNote(noteId: String) {
        makeAWish(NOTES_CODES.INSERT) {
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
                        moveBack = true
                    )
                }
            }

            else -> _noteUiState.update {
                it.copy(isLoading = false, error = (error as NetworkError).getError())
            }
        }
    }

    override fun clearError() {
        _noteUiState.update {
            it.copy(
                error = null, moveBack = false, successMessage = null, isLoading = false
            )
        }
    }
}