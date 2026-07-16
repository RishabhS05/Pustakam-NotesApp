@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.pustakam.android.screen.notes.list


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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
// 🔧 15-Jul-2026 Phase 0.1: infinite-scroll trigger
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.R
import com.app.pustakam.android.screen.DialogEnum
import com.app.pustakam.android.screen.NotesUIState
import com.app.pustakam.android.screen.features.tags.TagViewModel
import com.app.pustakam.android.screen.notes.list.TagIntent.*
import com.app.pustakam.android.screen.notes.single.NoteCardView
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.TagView
import com.app.pustakam.android.widgets.colorPalete.CreateTagBottomSheet
import com.app.pustakam.data.models.Tag
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull
import sideblueGradient

sealed interface NotesViewIntent {

 }
sealed interface TagIntent{
    data class OnTagClick(val tag: Tag): TagIntent
    data class OnCreateTag(val tag: Tag): TagIntent
    data class OnColorPicked(val color: Color): TagIntent
    data class ShowOrHideUIAlerts(val dialog: DialogEnum): TagIntent
}
@Composable
fun NotesView(onNavigateNote: (noteId: String) -> Unit) {

    val notesViewModel: NotesViewModel = viewModel()
     val state =  notesViewModel.notesUIState.collectAsStateWithLifecycle().value
         .apply {
             // 🔧 15-Jul-2026 Summary query: the list renders summaries; navigation only needs the id
             when {
                 summaries.isEmpty() -> { EmptyNoteUI(modifier = Modifier.fillMaxSize())}
                 summaries.isNotEmpty() -> NotesListView(this,
                     onNavigateNote = onNavigateNote,
                     // 🔧 15-Jul-2026 Phase 0.1: next page loads when the grid reaches its end
                     onLoadMore = notesViewModel::callGetNotes,
                     modifier = Modifier.fillMaxSize(),
                     paddingValues = PaddingValues(0.dp)
                 )
             }
         }
         if(state.error.isNotnull())
             SnackBarUi(error = state.error!!) {
                 notesViewModel.clearError()
             }
        if(state.isLoading) LoadingUI()
}
@Composable
fun NotesListView(
    state : NotesUIState,
    onNavigateNote: (noteId: String) -> Unit,
    // 🔧 15-Jul-2026 Phase 0.1: called when the last card composes (scroll reached the end);
    //   the ViewModel guards against duplicate/past-the-end fetches.
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val tagViewModel: TagViewModel = viewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tagsState = tagViewModel.tagsUiState.collectAsStateWithLifecycle().value.apply {
        when (dialog){
            DialogEnum.CREATE_TAG ->
                CreateTagBottomSheet(sheetState =sheetState, onSubmit = {tagViewModel.onTagIntent(
                    OnCreateTag(it)) },
                    onDismissRequest ={
                        tagViewModel.onTagIntent(ShowOrHideUIAlerts(dialog = DialogEnum.NONE))
                    })

            else -> {}
        }
        if( dialog == DialogEnum.COLOR_PICKER) {

    }
    }
    val notes = state.summaries   // 🔧 15-Jul-2026 Summary query: cards render summaries
    LazyVerticalStaggeredGrid(
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
            TagsListView(
                tags = tagsState.tags,
                onTagIntent= tagViewModel::onTagIntent,
                modifier = Modifier.padding(8.dp)
            )
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(12.dp))
        }
        items(notes.size) { index ->
            NoteCardView(summary = notes[index]) { onNavigateNote(notes[index].id) }
            // 🔧 15-Jul-2026 Phase 0.1: infinite-scroll trigger — fires once per list growth
            //   (keyed on size) when the LAST card enters composition.
            if (index == notes.size - 1) {
                LaunchedEffect(notes.size) { onLoadMore() }
            }
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

@Composable
fun TagsListView(
    tags: ArrayList<Tag>,
    onTagIntent: (tagIntent: TagIntent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)){
        ->
        items(tags.size){index ->
            TagView(tag = tags[index],
                modifier = Modifier
                    .padding(4.dp)
                    .shadow(12.dp,)
                    .clickable {
                        onTagIntent(OnTagClick(tags[index])) })
        }
        item {
            TagView(tag = Tag(id = "Add Tag", label = "+Tag", color = "#246cbc"),
                modifier = Modifier
                    .padding(4.dp)
                    .shadow(12.dp,)
                    .clickable {
                        onTagIntent(ShowOrHideUIAlerts(dialog = DialogEnum.CREATE_TAG))
                    })
        }
    }
}