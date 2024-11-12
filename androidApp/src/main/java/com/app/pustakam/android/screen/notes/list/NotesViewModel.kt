package com.app.pustakam.android.screen.notes.list

import com.app.pustakam.android.screen.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NotesUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.notes.GetNotesUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.log_d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotesViewModel : BaseViewModel() {
    private val _notesUiState = MutableStateFlow(NotesUIState(isLoading = false))
    val notesUIState: StateFlow<NotesUIState> = _notesUiState.asStateFlow()
    private val getNotesUseCase = GetNotesUseCase()
    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {

        when (taskCode) {
            NOTES_CODES.GET_NOTES -> {
                val response = (result.data.data as Notes)
                _notesUiState.update {
                    val page = it.page + 1
                    var isNextPage = it.isNextPage
                    if (!response.notes.isNullOrEmpty()) {
                        it.notes.addAll(response.notes!!)
                    } else {
                        isNextPage = false
                    }
                    it.copy(isLoading = false, notes = it.notes,
                        successMessage = result.data.message,
                        page = page, isNextPage = isNextPage)
                }
            }
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        _notesUiState.update {
            it.copy(error = (error as NetworkError).getError())
        }
    }

    fun getNotes() {
        val notes  =  getNotesUseCase.invoke()
        if(!notes?.notes.isNullOrEmpty()) {
            _notesUiState.update {
                it.notes.addAll(notes?.notes!!)
                it.copy(notes = it.notes)
            }
        }else {
            callGetNotesApi()
        }
    }
   private fun callGetNotesApi(){
    if (!_notesUiState.value.isNextPage) return
    makeAWish(NOTES_CODES.GET_NOTES) {
        getNotesUseCase.invoke(page = _notesUiState.value.page)
    }
   }

    override fun clearError() {
        _notesUiState.update {
            it.copy(error = null)
        }
    }
}