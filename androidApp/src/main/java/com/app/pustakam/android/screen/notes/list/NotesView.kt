package com.app.pustakam.android.screen.notes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.notes.single.NoteView
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull

@Composable
fun NotesView(onNavigateNote : (note : Note)-> Unit) {
    val notesViewModel: NotesViewModel = viewModel()
    notesViewModel.notesUIState.collectAsState().value.apply {
        OnLifecycleEvent { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (notes.isEmpty()) notesViewModel.getNotes()
                else -> {}
            }
        }
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                notesViewModel.clearError()
            }
            notes.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(count = 2), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notes.forEach { note ->
                    item {
                        NoteView(note = note){ onNavigateNote(note) }
                    }
                }
            }

            isLoading -> LoadingUI()
        }
    }

}