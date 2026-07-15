package com.app.pustakam.android.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.notes.single.NoteCardView

// 🔧 15-Jul-2026 Phase 2.2: IMPLEMENTED — this screen was a magenta placeholder. It is now the
//   FTS5-backed note search: type to search all note text (and titles); results are summary cards
//   (snippet from the matching block); tapping one opens the note in the editor.
//   Usage from the nav graph: SearchView(onNavigateNote = { id -> navigate(NotesEditor/id) })
@Composable
fun SearchView(onNavigateNote: (noteId: String) -> Unit) {
    val viewModel: SearchViewModel = viewModel()
    val state = viewModel.state.collectAsStateWithLifecycle().value
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            singleLine = true,
            placeholder = { Text("Search your notes…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (state.isSearching) CircularProgressIndicator(
                    strokeWidth = 2.dp, modifier = Modifier.padding(12.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        when {
            state.query.isBlank() -> CenterHint("Search across every note — text, titles.")
            state.results.isEmpty() && !state.isSearching -> CenterHint("No notes match \"${state.query}\".")
            else -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.results.size) { index ->
                    val summary = state.results[index]
                    NoteCardView(summary = summary) { onNavigateNote(summary.id) }
                }
            }
        }
    }
}

@Composable
private fun CenterHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface.copy(alpha = 0.6f)),
            modifier = Modifier.padding(24.dp)
        )
    }
}
