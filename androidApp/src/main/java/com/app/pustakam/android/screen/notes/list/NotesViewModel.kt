package com.app.pustakam.android.screen.notes.list

import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NotesUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.domain.repositories.usecases.GetNotesUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.NOTES_PAGE_SIZE
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject


class NotesViewModel : BaseViewModel() {
    private var hasLoaded = false
    private val getNotesUseCase by inject<GetNotesUseCase>()
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
                // 🔧 15-Jul-2026 Phase 0.1: a page smaller than NOTES_PAGE_SIZE means the DB has no
                //   more notes — stop asking (isNextPage was never updated before; it stayed true).
                val fetchedCount = (result.data.data as? Notes)?.notes?.size ?: 0
                _notesUiState.update {
                    it.copy(
                        isLoading = false, notes = it.notes,
                        successMessage = result.data.message,
                        page = it.page + 1, isNextPage = fetchedCount >= NOTES_PAGE_SIZE
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
        val state = _notesUiState.value
        // 🔧 15-Jul-2026 Phase 0.1: isLoading guard — the scroll trigger must not queue duplicate
        //   page fetches while one is already in flight.
        if (!state.isNextPage || state.isLoading) return
        makeAWish(NOTES_CODES.GET_NOTES) {
            // 🔧 15-Jul-2026 Phase 0.1: paged overload — loads one page instead of the whole DB
            getNotesUseCase.invoke(page = state.page, limit = NOTES_PAGE_SIZE)
        }
    }

    override fun clearError() {
        _notesUiState.update {
            it.copy(error = null)
        }
    }

}