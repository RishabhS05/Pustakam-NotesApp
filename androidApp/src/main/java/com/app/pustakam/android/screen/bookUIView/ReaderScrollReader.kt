package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderPage
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 01-Aug-2026: scroll mode over the SAME pages the curl mode renders — no second pagination.
//   Sheets wrap their content here (fillHeight=false), which is what removes the empty space that
//   the old fixed 560dp box forced onto every page.
@Composable
fun ReaderScrollReader(
    pages: List<ReaderPage>,
    policy: PageLayoutPolicy,
    startPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    onOpenDocument: (NoteContentModel.MediaContent) -> Unit = {},
    onOpenMedia: (NoteContentModel.MediaContent) -> Unit = {},
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    )
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { onPageChanged(it) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // keyed so a page's widgets survive scroll recycling instead of recomposing from scratch
        items(count = pages.size, key = { pages[it].index }) { index ->
            ReaderPageContent(
                page = pages[index],
                policy = policy,
                fillHeight = false,
                onOpenDocument = onOpenDocument,
                onOpenMedia = onOpenMedia,
            )
        }
    }
}
