package com.app.pustakam.android.screen.notes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.notes.single.NoteCardView
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesView(onNavigateNote: (note: Note) -> Unit) {
    val notesViewModel: NotesViewModel = viewModel()
    val scrollState = rememberScrollState()
    notesViewModel.notesUIState.collectAsState().value.apply {
        OnLifecycleEvent { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    notesViewModel.getNotes()
                }
                else -> {}
            }
        }
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                notesViewModel.clearError()
            }

            notes.isNotEmpty() ->
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),) {
                    items(notes.size) { index  ->
                        NoteCardView(note = notes[index]) { onNavigateNote(notes[index]) }
                    } }
            isLoading -> LoadingUI()
        }
    }

}