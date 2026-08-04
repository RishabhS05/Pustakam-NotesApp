package com.app.pustakam.android.screen.bookReading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.bookUIView.BookPageContent
import com.app.pustakam.android.screen.bookUIView.BookScrollReader
import com.app.pustakam.android.screen.notebookReader.BookPager

import com.app.pustakam.android.screen.notebookReader.ReadingMode
import com.app.pustakam.android.theme.PaperColor
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi

@Composable
fun BookReaderScreen(
    modifier: Modifier = Modifier,
    bookId: String? = null,
    viewModel: BookReaderViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.bookUiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookId) { viewModel.onHandleIntent(BookReaderIntent.LoadBook(bookId)) }

    Box(modifier.fillMaxSize().background(Color(0xFF241C14))) {   // dark desk behind the book
        when {
            state.pages.isEmpty() && state.isLoading -> LoadingUI()

            state.error != null -> SnackBarUi(error = state.error!!) {
                viewModel.clearError(); onBack()
            }

            state.pages.isNotEmpty() -> {
                var pageIndex by remember { mutableIntStateOf(state.pageProgress) }
                val doc = state.doc
                when (state.readingMode) {
                    ReadingMode.PAGE ->
                        BookPager(
                            pageCount = state.pages.size,
                            initialPage = pageIndex,
                            onPageChanged = { index ->
                                pageIndex = index
                                doc?.let { viewModel.onHandleIntent(BookReaderIntent.PageChanged(it, index)) }
                            },
                        ) { index -> BookPageContent(page = state.pages[index]) }

                    ReadingMode.SCROLL ->
                        BookScrollReader(
                            pages = state.pages,
                            startPageIndex = pageIndex,
                            onPageChanged = { index ->
                                pageIndex = index
                                doc?.let { viewModel.onHandleIntent(BookReaderIntent.PageChanged(it, index)) }
                            },
                        )
                }
                // page counter chip
                Text(
                    "${pageIndex + 1} / ${state.pages.size}",
                    style = typography.labelMedium, color = PaperColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .background(Color.Black.copy(alpha = .45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close document", tint = colorScheme.secondary)
        }
        if (state.pages.isNotEmpty()) {
            IconButton(
                onClick = {
                    viewModel.onHandleIntent(BookReaderIntent.ToggleReadingMode(state.readingMode.toggled()))
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            ) {
                Icon(
                    if (state.readingMode == ReadingMode.PAGE)
                        Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = if (state.readingMode == ReadingMode.PAGE)
                        "Switch to scrolling" else "Switch to page curl",
                    tint = colorScheme.secondary
                )
            }
        }
    }
}
