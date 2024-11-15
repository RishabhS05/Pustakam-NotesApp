package com.app.pustakam.android.screen.notes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    notesViewModel.notesUIState.collectAsState().value.apply {
        OnLifecycleEvent { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (notes.isEmpty()) notesViewModel.getNotes()
                else -> {}
            }
        }
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                notesViewModel.clearError()
            }

            notes.isNotEmpty() ->
                FlowColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    notes.forEach { note ->
                        NoteCardView(note = note) { onNavigateNote(note) }
                    }
                }

            isLoading -> LoadingUI()
        }
    }

}