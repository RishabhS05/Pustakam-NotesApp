package com.app.pustakam.android.screen.notes.list

import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NotesUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.notes.GetNotesUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class NotesViewModel : BaseViewModel() {
    private var hasLoaded = false
    private val getNotesUseCase = GetNotesUseCase()
    private val _notesUiState = MutableStateFlow(NotesUIState(isLoading = false,
        isNextPage = true))

    val notesUIState = _notesUiState
        .onStart {
            viewModelScope.launch {
                getNotesUseCase.notes.collect {notes ->
                    _notesUiState.update {currentState ->
                        currentState.copy(notes = notes.notes, isLoading = false )
                    }
                }
            }
        if(!hasLoaded){
            callGetNotes()
            hasLoaded = true
        }
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _notesUiState.value
    )

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        when (taskCode) {
            NOTES_CODES.GET_NOTES -> {
                _notesUiState.update {
                    it.copy(
                        isLoading = false, notes = it.notes,
                        successMessage = result.data.message,
                        page = it.page + 1, isNextPage = it.isNextPage
                    )
                }
            }
        }
    }
    override fun onLoading(taskCode: TaskCode) {
        _notesUiState.update {
            it.copy(isLoading = true)
        }
    }
    override fun onFailure(taskCode: TaskCode, error: Error) {
        super.onFailure(taskCode, error)
        _notesUiState.update {
            it.copy(error = (error as NetworkError).getError())
        }
    }

    override suspend fun logoutUserForcefully() {
        getNotesUseCase.logoutUser()
    }


    fun callGetNotes() {
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