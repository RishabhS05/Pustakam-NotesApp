package com.app.pustakam.android.screen.bookUIView

// 📖 02-Aug-2026: NEW — standard continuous PDF viewer used by SCROLL mode only.
//   BookScrollReader is untouched and still serves mixed-content notes.
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.screen.notebookReader.BookPage
import com.app.pustakam.android.widgets.LoadingUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

// 📖 bitmaps follow zoom in whole steps only — sharp when zoomed, cheap at 1x, no churn mid-pinch
private const val MAX_RENDER_STEP = 3
private const val MAX_RENDER_WIDTH_PX = 2400
private const val A4_RATIO = 1.414f
private val PAGE_GAP = 8.dp

/**
 * 📖 02-Aug-2026: the whole PDF as ONE scrollable document — pages at their real proportions,
 *   edge to edge, and a single pinch/double-tap zoom over the entire file.
 */
@Deprecated("it is taking too long to load pdf dont use it.")
@Composable
fun PdfScrollReader(
    path: String,
    startPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessionState = remember(path) { mutableStateOf<PdfSession?>(null) }
    var pageCount by remember(path) { mutableIntStateOf(0) }
    var ratios by remember(path) { mutableStateOf(FloatArray(0)) }

    // 📖 one renderer for the whole session, closed when the reader leaves the screen
    DisposableEffect(path) {
        onDispose { sessionState.value?.close(); sessionState.value = null }
    }
    LaunchedEffect(path) {
        val opened = withContext(Dispatchers.IO) {
            runCatching { PdfSession(path).also { it.measureAll() } }.getOrNull()
        }
        sessionState.value = opened
        pageCount = opened?.pageCount ?: 0
        ratios = opened?.ratios ?: FloatArray(0)
    }

    val session = sessionState.value
    if (session == null || pageCount == 0) {
        Box(modifier.fillMaxSize()) { LoadingUI() }
        return
    }

    val scaleState = remember(path) { mutableFloatStateOf(MIN_ZOOM) }
    val lastScale = remember(path) { mutableFloatStateOf(MIN_ZOOM) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startPageIndex.coerceIn(0, pageCount - 1)
    )
    val hScroll = rememberScrollState()

    // 📖 same progress reporting the old scroll reader did — reading position still persists
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { onPageChanged(it) }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportWidth = maxWidth
        val viewportWidthPx = with(density) { viewportWidth.toPx() }
        val scale = scaleState.floatValue
        val pageWidth = viewportWidth * scale

        // 📖 quantised: only 1x/2x/3x trigger a re-render, so a pinch never thrashes the renderer
        val renderStep = ceil(scale).toInt().coerceIn(1, MAX_RENDER_STEP)
        val renderWidthPx = (viewportWidthPx * renderStep).roundToInt()
            .coerceIn(1, MAX_RENDER_WIDTH_PX)

        // 📖 keep the same point under the fingers while zooming; snap back to edge at 1x
        LaunchedEffect(scale) {
            val previous = lastScale.floatValue
            lastScale.floatValue = scale                 // set before suspending: cancellation can't stale it
            if (scale <= MIN_ZOOM) {
                hScroll.scrollTo(0)
            } else {
                val centre = hScroll.value + viewportWidthPx / 2f
                val target = centre * (scale / previous) - viewportWidthPx / 2f
                hScroll.scrollTo(target.roundToInt().coerceAtLeast(0))
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .horizontalScroll(hScroll)
                // 📖 Initial pass: the pinch is claimed before the lists can read it as a drag
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var pinched = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.count { it.pressed }
                            if (pressed == 0) break
                            if (pressed > 1) {
                                pinched = true
                                val change = event.calculateZoom()
                                if (change != 1f) {
                                    scaleState.floatValue =
                                        (scaleState.floatValue * change).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pinched) {
                                // 📖 swallow the leftover finger so it can't scroll on pinch release
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scaleState.floatValue =
                            if (scaleState.floatValue > MIN_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM
                    })
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(pageWidth).fillMaxHeight(),
                contentPadding = PaddingValues(vertical = PAGE_GAP),
                verticalArrangement = Arrangement.spacedBy(PAGE_GAP),
            ) {
                items(pageCount) { index ->
                    PdfPageSheet(
                        session = session,
                        index = index,
                        ratio = ratios.getOrElse(index) { A4_RATIO },
                        renderWidthPx = renderWidthPx,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// 📖 one page, sized to its real proportions so nothing is cropped or letterboxed
@Composable
private fun PdfPageSheet(
    session: PdfSession,
    index: Int,
    ratio: Float,
    renderWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(index, renderWidthPx) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(index, renderWidthPx) {
        bitmap = session.render(index, renderWidthPx)
    }
    Box(modifier.aspectRatio(1f / ratio).background(Color.White)) {
        bitmap?.let {
            Image(
                it.asImageBitmap(),
                "Page ${index + 1}",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

// 📖 every page is a sheet of the SAME pdf → it can scroll as one document
fun List<BookPage>.singlePdfPathOrNull(): String? {
    val first = firstOrNull() as? BookPage.PdfSheet ?: return null
    return if (all { it is BookPage.PdfSheet && it.path == first.path }) first.path else null
}

// 📖 one open renderer per document; PdfRenderer allows a single page at a time, hence the lock
private class PdfSession(path: String) {
    private val descriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val lock = Mutex()

    val pageCount: Int = renderer.pageCount
    val ratios = FloatArray(pageCount) { A4_RATIO }

    // 📖 reads page dictionaries only — no rasterising, so the layout is right from the first frame
    fun measureAll() {
        for (i in 0 until pageCount) {
            runCatching {
                renderer.openPage(i).use { p ->
                    ratios[i] = p.height.toFloat() / p.width.coerceAtLeast(1).toFloat()
                }
            }
        }
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? = lock.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                renderer.openPage(index).use { p ->
                    val w = widthPx.coerceAtLeast(1)
                    val h = (w * (p.height.toFloat() / p.width.coerceAtLeast(1).toFloat()))
                        .roundToInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }.getOrNull()
        }
    }

    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}
