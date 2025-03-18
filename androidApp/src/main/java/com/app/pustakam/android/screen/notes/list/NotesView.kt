package com.app.pustakam.android.screen.notes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.notes.single.NoteCardView
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull
@Composable
fun NotesView(onNavigateNote: (note: Note) -> Unit) {
    val notesViewModel: NotesViewModel = viewModel()
    notesViewModel.notesUIState.collectAsStateWithLifecycle().value.apply {
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                notesViewModel.clearError()
            }
            notes.isNotEmpty() -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notes.size) { index ->
                    NoteCardView(note = notes[index]) { onNavigateNote(notes[index]) }
                }
            }
            isLoading -> LoadingUI()
        }
    }

}