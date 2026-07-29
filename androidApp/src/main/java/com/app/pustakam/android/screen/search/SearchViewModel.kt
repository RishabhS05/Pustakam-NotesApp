package com.app.pustakam.android.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.core.model.models.response.notes.NoteSummary
import com.app.pustakam.feature.notes.domain.usecase.SearchNotesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Result

// 🔧 15-Jul-2026 Phase 2.2: state + intents for the search screen (MVI: query intent -> debounced
//   FTS5 search -> results state). Results are NoteSummary items — content matches carry a snippet.
data class SearchUiState(
    val query: String = "",
    val results: List<NoteSummary> = emptyList(),
    val isSearching: Boolean = false,
)

// 🔧 15-Jul-2026 Phase 2.2: NEW — drives the (previously stub) SearchView.
//   How to use: collect `state`, call onQueryChange(text) from the search field; each change is
//   debounced 300ms, the previous in-flight search is cancelled, and results land in state.
class SearchViewModel : ViewModel(), KoinComponent {
    private val searchNotesUseCase by inject<SearchNotesUseCase>()
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)   // debounce: search after the user pauses typing
            searchNotesUseCase(query).collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isSearching = true) }
                    is Result.Success -> _state.update {
                        @Suppress("UNCHECKED_CAST")
                        it.copy(results = (result.data.data as? List<NoteSummary>).orEmpty(), isSearching = false)
                    }
                    is Result.Error -> _state.update { it.copy(isSearching = false) }
                }
            }
        }
    }
}
