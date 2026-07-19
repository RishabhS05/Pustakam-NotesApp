package com.app.pustakam.android.screen.bookReader

// 🔧 18-Jul-2026: NEW FEATURE (book reader) — opens a note as a REAL book: paper pages, spine,
//   3D page-flip; renders text, images, PDFs (page-per-page), audio/video, docs, links, locations.
import PaperColor
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.bookwidget.BookWidgetUpdater
import com.app.pustakam.android.widgets.document.iconForContentType
import com.app.pustakam.android.widgets.document.readableSize
import com.app.pustakam.android.widgets.zoom.zoomable   // 🔧 19-Jul-2026: pinch-zoom on pages
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.FileImportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

// 🔧 18-Jul-2026: warm paper palette — a book stays paper-colored in any app theme

private val PaperInk = Color(0xFF3E2F1C)
private val CoverColor = Color(0xFF5D4033)

@Composable
fun BookReaderScreen(
    noteId: String,
    startContentId: String? = null,
    singleContent: Boolean = false,   // 🔧 19-Jul-2026: open ONLY the tapped file as a book
    bookReaderViewModel: BookReaderViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by bookReaderViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) { bookReaderViewModel.load(context, noteId, startContentId, singleContent) }
    // 🔧 18-Jul-2026: remember this book for the home-screen widget once it opens
    LaunchedEffect(state.note?.id) {
        state.note?.let { BookWidgetUpdater.saveLastBook(context, it.id, it.title ?: "Untitled note") }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF241C14))) {   // dark desk behind the book
        when {
            // 🔧 19-Jul-2026: FIX — loader ONLY while pages aren't built; never over loaded pages,
            //   and no error flash on first load (VM suppresses transient read failures).
            state.pages.isEmpty() && state.isLoading -> LoadingUI()
            state.error != null -> SnackBarUi(error = state.error!!) { bookReaderViewModel.clearError(); onBack() }
            state.pages.isNotEmpty() -> {
                var pageIndex by remember { mutableIntStateOf(state.startPageIndex) }
                BookPager(
                    pageCount = state.pages.size,
                    initialPage = state.startPageIndex,
                    onPageChanged = { pageIndex = it },
                ) { index ->
                    BookPageContent(page = state.pages[index])
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close book", tint = PaperColor)
        }
    }
}

// 🔧 18-Jul-2026: shared paper frame — spine gradient left, soft page edge right
@Composable
private fun PaperPage(background: Color = PaperColor, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .background(background, RoundedCornerShape(6.dp))
    ) {
        content()
        Box(
            Modifier.fillMaxHeight().width(14.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = .18f), Color.Transparent)
                    ), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                )
        )
    }
}

@Composable
fun BookPageContent(page: BookPage) {
    when (page) {
        is BookPage.Cover -> PaperPage(background = CoverColor) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    page.title, style = typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                    color = PaperColor, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(60.dp).height(2.dp).background(PaperColor.copy(alpha = .6f)))
                Spacer(Modifier.height(12.dp))
                Text(page.subtitle, style = typography.titleSmall, color = PaperColor.copy(alpha = .8f))
            }
        }

        is BookPage.TextPage -> PaperPage {
            Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp)) {
                Text(
                    page.text,
                    style = typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 26.sp),
                    color = PaperInk,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                )
                if (page.chunkCount > 1) Text(
                    "· ${page.chunkIndex} of ${page.chunkCount} ·", style = typography.labelSmall,
                    color = PaperInk.copy(alpha = .5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }

        is BookPage.ImagePage -> PaperPage {
            Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = page.path, contentDescription = page.title,
                    contentScale = ContentScale.Fit,   // never fills/crops — full image visible
                    // 🔧 19-Jul-2026: pinch/double-tap zoom on image pages
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp).zoomable()
                )
                if (page.title.isNotBlank()) Text(
                    page.title, style = typography.labelMedium.copy(fontStyle = FontStyle.Italic),
                    color = PaperInk.copy(alpha = .7f), maxLines = 1
                )
            }
        }

        is BookPage.PdfSheet -> PaperPage(background = Color.White) { PdfBookPage(page) }

        is BookPage.MediaPage -> PaperPage { MediaBookPage(page.media) }

        is BookPage.DocFilePage -> PaperPage { DocFileBookPage(page.media) }

        is BookPage.LinkPage -> PaperPage {
            val context = LocalContext.current
            Column(
                Modifier.fillMaxSize().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("A link lives on this page", style = typography.titleMedium, color = PaperInk)
                Spacer(Modifier.height(8.dp))
                Text(page.url, style = typography.bodyMedium, color = Color(0xFF1A5276), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page.url)))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Open link") }
            }
        }

        is BookPage.LocationPage -> PaperPage {
            val context = LocalContext.current
            Column(
                Modifier.fillMaxSize().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.LocationOn, null, tint = CoverColor, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    page.address ?: "%.5f, %.5f".format(page.latitude, page.longitude),
                    style = typography.titleSmall, color = PaperInk, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val geo = Uri.parse("geo:${page.latitude},${page.longitude}?q=${page.latitude},${page.longitude}")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
                    catch (_: ActivityNotFoundException) { Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show() }
                }) { Text("Open in Maps") }
            }
        }
    }
}

// 🔧 18-Jul-2026: renders ONE pdf page lazily off the main thread; result cached per page slot
@Composable
private fun PdfBookPage(page: BookPage.PdfSheet) {
    var bitmap by remember(page.path, page.pageIndex) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page.path, page.pageIndex) {
        bitmap = withContext(Dispatchers.IO) { renderPdfPage(page.path, page.pageIndex) }
    }
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(), contentDescription = "${page.title} page ${page.pageIndex + 1}",
                contentScale = ContentScale.Fit,
                // 🔧 19-Jul-2026: pinch/double-tap zoom on PDF sheets
                modifier = Modifier.weight(1f).fillMaxWidth().zoomable()
            )
        } ?: Box(Modifier.weight(1f).fillMaxWidth()) { LoadingUI() }
        Text(
            "${page.title} — ${page.pageIndex + 1}/${page.pageCount}",
            style = typography.labelSmall, color = PaperInk.copy(alpha = .6f), maxLines = 1
        )
    }
}

// 🔧 18-Jul-2026: open/close the renderer per page — memory-safe for huge PDFs
private fun renderPdfPage(path: String, index: Int): Bitmap? = try {
    val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    PdfRenderer(descriptor).use { renderer ->
        renderer.openPage(index).use { p ->
            val scale = min(2f, 2048f / maxOf(p.width, 1))
            val bmp = Bitmap.createBitmap((p.width * scale).toInt(), (p.height * scale).toInt(), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }
} catch (e: Exception) { e.printStackTrace(); null }

// 🔧 18-Jul-2026: audio/video page — self-contained ExoPlayer, released when the page leaves
@Composable
private fun MediaBookPage(media: NoteContentModel.MediaContent) {
    val context = LocalContext.current
    val source = media.localPath?.takeIf { it.isNotEmpty() } ?: media.url
    val player = remember(media.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(source))
            prepare()
        }
    }
    androidx.compose.runtime.DisposableEffect(media.id) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            media.title.ifBlank { media.type.name }, style = typography.titleSmall,
            color = PaperInk, maxLines = 1, modifier = Modifier.padding(bottom = 8.dp)
        )
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = true } },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

// 🔧 18-Jul-2026: DOCX/EPUB/OTHER page — file identity + "Open" via the system viewer (FileProvider)
@Composable
private fun DocFileBookPage(media: NoteContentModel.MediaContent) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(74.dp).background(CoverColor.copy(alpha = .12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconForContentType(media.type), media.type.name, tint = CoverColor, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(media.title.ifBlank { "File" }, style = typography.titleSmall, color = PaperInk, textAlign = TextAlign.Center)
        Text(
            listOf(media.type.name, readableSize(media.sizeBytes)).filter { it.isNotBlank() }.joinToString(" · "),
            style = typography.labelSmall, color = PaperInk.copy(alpha = .6f)
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = { openWithSystemViewer(context, media) }) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open")
        }
    }
}

// 🔧 18-Jul-2026: private file → content:// grant → ACTION_VIEW (reader apps handle docx/epub)
fun openWithSystemViewer(context: Context, media: NoteContentModel.MediaContent) {
    val path = media.localPath?.takeIf { it.isNotEmpty() } ?: run {
        Toast.makeText(context, "File not available offline", Toast.LENGTH_SHORT).show(); return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, media.mimeType.ifBlank { FileImportHelper.mimeFor(media.type) })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Couldn't open the file", Toast.LENGTH_SHORT).show()
    }
}
