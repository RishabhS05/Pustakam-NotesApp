package com.app.pustakam.android.screen.bookUIView

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.screen.notebookReader.BookPage
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.zoom.zoomable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.use

@Composable
 fun PdfBookPage(page: BookPage.PdfSheet) {
    var bitmap by remember(page.path, page.pageIndex) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page.path, page.pageIndex) {
        bitmap = withContext(Dispatchers.IO) { renderPdfPage(page.path, page.pageIndex) }
    }
    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(), contentDescription = "${page.title} page ${page.pageIndex + 1}",
                contentScale = ContentScale.Fit,
                // 🔧 19-Jul-2026: pinch/double-tap zoom on PDF sheets
                modifier = Modifier.weight(1f).fillMaxWidth().zoomable()
            )
        } ?: Box(Modifier.weight(1f).fillMaxWidth()) { LoadingUI() }
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