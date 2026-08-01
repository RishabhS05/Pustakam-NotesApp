package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import com.app.pustakam.android.screen.notebookReader.BookPage


import com.app.pustakam.android.theme.PaperColor
@Composable
fun BookScrollReader(
    pages: List<BookPage>,
    startPageIndex: Int,
    onPageChanged: (Int) -> Unit,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = startPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    )
    // report the top-most fully/partly visible sheet as the current page (debounced by the VM)
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }
            .collect { onPageChanged(it) }
    }
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(pages.size) { index ->

            Box(Modifier.fillMaxWidth().height(560.dp)) {
                BookPageContent(page = pages[index])
            }
        }
    }
}
