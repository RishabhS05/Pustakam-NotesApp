package com.app.pustakam.android.screen.notes.list

import allGradient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.R
import com.app.pustakam.android.extension.thickGlass
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.notes.single.NoteCardView
import com.app.pustakam.android.widgets.LoadImage
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.TagView
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull
import sideblueGradient

@Composable
fun NotesView(onNavigateNote: (note: Note) -> Unit) {
    val notesViewModel: NotesViewModel = viewModel()
    notesViewModel.notesUIState.collectAsStateWithLifecycle().value.apply {
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                notesViewModel.clearError()
            }
            notes.isEmpty ->{ EmptyNoteUI(modifier = Modifier.fillMaxSize())}
            notes.isNotEmpty() -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(sideblueGradient),
                        alpha = 0.2f
                    ),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = StaggeredGridItemSpan.FullLine){
                    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)){
                        items(tags.size){index ->
                            TagView(tag = tags[index], modifier = Modifier.padding(4.dp).shadow(12.dp,).clickable{})
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxWidth().height(12.dp))
                }

                items(notes.size) { index ->
                    NoteCardView(note = notes[index]) { onNavigateNote(notes[index]) }
                }
            }
            isLoading -> LoadingUI()
        }
}
}
@Composable
fun EmptyNoteUI(modifier: Modifier = Modifier) {
    Column(modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(R.mipmap.empty_notes), "", Modifier.size(300.dp))

    }
}