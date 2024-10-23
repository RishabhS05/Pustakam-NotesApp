package com.app.pustakam.android.screen.notes.single

import com.app.pustakam.android.screen.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.notes.CreateNoteUseCase
import com.app.pustakam.android.screen.notes.DeleteNoteUseCase
import com.app.pustakam.android.screen.notes.UpdateNoteUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.checkAnyUpdateOnNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NoteViewModel : BaseViewModel() {
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        when (taskCode) {
            NOTES_CODES.DELETE -> {
                _noteUiState.update { it.copy(isLoading = false) }
            }

            NOTES_CODES.ADD -> {
                _noteUiState.update { it.copy(isLoading = false) }
            }

            NOTES_CODES.UPDATE -> {
                _noteUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    suspend fun createNote(title: String?, body: String?) {
        val createNoteUseCase = CreateNoteUseCase()
        val createNote = NoteRequest(title = title, body = body)
        if(!title.isNullOrEmpty() && !body.isNullOrEmpty()) {
            makeAWish(NOTES_CODES.ADD) {
                createNoteUseCase.invoke(createNote)
            }
        }
    }

    fun updateNote(title: String?, body: String?, id: String , note : Note) {
        val updateNoteUseCase = UpdateNoteUseCase()
        val updateNote = NoteRequest(title = title, body = body ,_id= id)
        if(checkAnyUpdateOnNote(new = updateNote, old= note)) return
        makeAWish(NOTES_CODES.ADD) {
            updateNoteUseCase.invoke(updateNote)
        }
    }

    fun deleteNote(noteId: String) {
        val deleteNoteUseCase = DeleteNoteUseCase()
        makeAWish(NOTES_CODES.ADD) {
            deleteNoteUseCase.invoke(noteId)
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        _noteUiState.update {
            it.copy(isLoading = false, error = (error as NetworkError).getError())
        }
    }

    override fun clearError() {
        _noteUiState.update {
            it.copy(error = null)
        }
    }
}