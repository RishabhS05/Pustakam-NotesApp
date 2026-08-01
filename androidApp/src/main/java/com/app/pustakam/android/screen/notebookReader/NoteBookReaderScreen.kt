package com.app.pustakam.android.screen.notebookReader
import com.app.pustakam.android.theme.PaperColor
// 🔧 30-Jul-2026 02:10 shared-player protocol: same ViewModel + events the editor drives
import com.app.pustakam.android.hardware.audio.player.MediaPlayingUIEvent
import com.app.pustakam.android.hardware.audio.player.PlayMediaViewModel
  import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.android.widgets.audio.AudioPlayerUIState
import com.app.pustakam.android.widgets.video.VideoCard
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List // 📖 25-Jul-2026 scroll-mode toggle
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook // 📖 25-Jul-2026 page-curl toggle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.app.pustakam.android.screen.bookUIView.BookPageContent
import com.app.pustakam.android.screen.bookUIView.BookScrollReader
import com.app.pustakam.android.screen.bookUIView.PaperPage
import com.app.pustakam.android.screen.bookUIView.ReaderPageContent
import com.app.pustakam.android.screen.bookUIView.ReaderScrollReader
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.bookwidget.BookWidgetUpdater
import com.app.pustakam.android.widgets.document.iconForContentType
import com.app.pustakam.android.widgets.document.readableSize
import com.app.pustakam.android.widgets.zoom.zoomable   // 🔧 19-Jul-2026: pinch-zoom on pages
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.filesys.mime.MimeCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min


// 📖 01-Aug-2026: shared with the document reader (screen/bookReading) — one definition, no copy
@Composable
fun NoteBookReaderScreen(
    noteId: String = "",
    startContentId: String? = null,
    singleContent: Boolean = false,
    bookReaderViewModel: NoteBookReaderViewModel = viewModel(),
    onBack: () -> Unit = {},
    // 📖 01-Aug-2026: a document block opens the full document reader; media opens its preview
    onOpenDocument: (NoteContentModel.MediaContent) -> Unit = {},
    onOpenMedia: (NoteContentModel.MediaContent) -> Unit = {},
) {
    val context = LocalContext.current
    val state by bookReaderViewModel.uiState.collectAsStateWithLifecycle()
    // 📖 01-Aug-2026: pages are laid out against the DEVICE width at A4 proportions
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()

    LaunchedEffect(noteId, screenWidth) {
        if (noteId.isNotEmpty()) bookReaderViewModel.load(noteId, startContentId, singleContent, screenWidth)
    }

    DisposableEffect(state.note?.id) {
        onDispose {
            state.note?.let { BookWidgetUpdater.saveLastBook(context, it.id, it.title ?: "Untitled note") }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF241C14))) {   // dark desk behind the book
        when {
            // 🔧 19-Jul-2026: FIX — loader ONLY while pages aren't built; never over loaded pages,
            //   and no error flash on first load (VM suppresses transient read failures).
            state.pages.isEmpty() && state.isLoading -> LoadingUI()
            state.error != null -> SnackBarUi(error = state.error!!) { bookReaderViewModel.clearError(); onBack() }
            state.pages.isNotEmpty() -> {
                var pageIndex by remember { mutableIntStateOf(state.startPageIndex) }

                // 📖 01-Aug-2026: both modes render the SAME state.pages — switching never rebuilds
                when (state.readingMode) {
                    ReadingMode.PAGE ->
                        BookPager(
                            pageCount = state.pages.size,
                            initialPage = state.startPageIndex,
                            onPageChanged = {
                                pageIndex = it
                                // 📖 23-Jul-2026: persist progress on every turn (page-curl mode)
                                bookReaderViewModel.onPageChanged(it)
                            },
                        ) { index ->
                            ReaderPageContent(
                                page = state.pages[index],
                                policy = bookReaderViewModel.layoutPolicy,
                                onOpenDocument = onOpenDocument,
                                onOpenMedia = onOpenMedia,
                            )
                        }

                    ReadingMode.SCROLL ->
                        ReaderScrollReader(
                            pages = state.pages,
                            policy = bookReaderViewModel.layoutPolicy,
                            startPageIndex = state.startPageIndex,
                            onPageChanged = {
                                pageIndex = it
                                bookReaderViewModel.onPageChanged(it)   // 📖 same progress save as page mode
                            },
                            onOpenDocument = onOpenDocument,
                            onOpenMedia = onOpenMedia,
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close book", tint = colorScheme.secondary)
        }
        // 📖 25-Jul-2026: reading-mode toggle (parity with the iOS reader toolbar). Only shown once
        //   pages exist. Writes the SAME persisted pref Settings uses.
        if (state.pages.isNotEmpty()) {
            IconButton(
                onClick = { bookReaderViewModel.toggleReadingMode() },
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            ) {
                Icon(
                    if (state.readingMode == ReadingMode.PAGE) Icons.AutoMirrored.Filled.List
                    else Icons.Filled.MenuBook,
                    contentDescription = if (state.readingMode == ReadingMode.PAGE) "Switch to scrolling"
                    else "Switch to page curl",
                    tint = colorScheme.secondary
                )
            }
        }
    }
}


private val previewCover = BookPage.Cover(title = "Field Notes", subtitle = "6 entries")

private val previewShortText = BookPage.TextPage(
    text = "One short line — this is the case that exposes the full-page problem.",
    chunkIndex = 1, chunkCount = 1, sourceContentId = "t1",
)

private val previewLongText = BookPage.TextPage(
    text = ("Paragraph text that runs long enough to fill a sheet and show how the paper frame, " +
        "margins and typography behave when a page is genuinely full. ").repeat(6),
    chunkIndex = 1, chunkCount = 3, sourceContentId = "t2",
)

private val previewLink = BookPage.LinkPage(url = "https://example.com/a-reference", sourceContentId = "l1")

private val previewLocation = BookPage.LocationPage(
    latitude = 19.076, longitude = 72.8777, address = "Mumbai, Maharashtra", sourceContentId = "loc1",
)

@Preview(name = "Page · cover", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewCoverPage() { BookPageContent(page = previewCover) }

// 🔧 the one to look at: a single short line should NOT need a whole screen.
@Preview(name = "Page · short text", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewShortTextPage() { BookPageContent(page = previewShortText) }

@Preview(name = "Page · long text", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewLongTextPage() { BookPageContent(page = previewLongText) }

@Preview(name = "Page · link", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewLinkPage() { BookPageContent(page = previewLink) }

@Preview(name = "Page · location", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun PreviewLocationPage() { BookPageContent(page = previewLocation) }

// 🔧 30-Jul-2026 02:10 the important one — scroll mode with mixed content. Shows the inter-page
//   gutter and how much vertical space each sheet claims, which is where the layout work belongs.
@Preview(name = "Scroll · mixed content", showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun PreviewScrollMixed() {
    BookScrollReader(
        pages = listOf(previewCover, previewShortText, previewLink, previewLocation, previewLongText),
        startPageIndex = 0,
        onPageChanged = {},
    )
}
