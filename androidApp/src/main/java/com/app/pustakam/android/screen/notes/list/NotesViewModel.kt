package com.app.pustakam.android.screen.notes.list

import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NotesUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.domain.repositories.usecases.GetNoteSummariesUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.NOTES_PAGE_SIZE
import com.app.pustakam.util.NetworkError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject


class NotesViewModel : BaseViewModel() {
    private var hasLoaded = false
    // 🔧 15-Jul-2026 Summary query: the list now fetches/observes light summaries —
    //   full Note contents never load for the list screen.
    private val getNoteSummariesUseCase by inject<GetNoteSummariesUseCase>()
    private val _notesUiState = MutableStateFlow(NotesUIState(isLoading = false,
        isNextPage = true))

    val notesUIState = _notesUiState
        .onStart {
            viewModelScope.launch {
                getNoteSummariesUseCase.noteSummaries.collect { summaries ->
                    _notesUiState.update {currentState ->
                        currentState.copy(summaries = summaries, isLoading = false )
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
                // 🔧 15-Jul-2026 Summary query: the payload is now a page of NoteSummary items.
                val fetchedCount = (result.data.data as? List<*>)?.size ?: 0
                _notesUiState.update {
                    it.copy(
                        isLoading = false,
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
        getNoteSummariesUseCase.logoutUser()
    }


    fun callGetNotes() {
        val state = _notesUiState.value
        // 🔧 15-Jul-2026 Phase 0.1: isLoading guard — the scroll trigger must not queue duplicate
        //   page fetches while one is already in flight.
        if (!state.isNextPage || state.isLoading) return
        makeAWish(NOTES_CODES.GET_NOTES) {
            // 🔧 15-Jul-2026 Summary query: one page of summaries instead of full notes
            getNoteSummariesUseCase.invoke(page = state.page, limit = NOTES_PAGE_SIZE)
        }
    }

    override fun clearError() {
        _notesUiState.update {
            it.copy(error = null)
        }
    }

}