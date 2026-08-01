package com.app.pustakam.android.screen.bookUIView

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PAGES_PER_BATCH = 10

// 📖 01-Aug-2026: the document is READ INSIDE the note — file title, then its pages rendered inline
//   in batches of 10 with a Load more button. A 1000-page PDF costs 10 renders, not 1000. Page count
//   is read here rather than in the engine, which keeps commonMain free of file IO.
@Composable
fun DocumentBlockView(
    block: ReaderBlock.Document,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
) {
    val media = block.item
    var pageCount by remember(media.id) { mutableIntStateOf(0) }
    var loadedPages by remember(media.id) { mutableIntStateOf(PAGES_PER_BATCH) }

    LaunchedEffect(media.id) {
        pageCount = withContext(Dispatchers.IO) { pdfPageCount(media.localPath) }
    }
    val shown = minOf(loadedPages, pageCount)

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            media.title.ifBlank { "Document" },
            style = typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = PaperInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (pageCount > 0) Text(
            "$pageCount pages",
            style = typography.labelSmall, color = PaperInk.copy(alpha = .6f),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )

        repeat(shown) { index ->
            DocumentPageImage(path = media.localPath, index = index, policy = policy)
        }

        if (shown < pageCount) OutlinedButton(
            onClick = { loadedPages += PAGES_PER_BATCH },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Load more · ${pageCount - shown} left")
        }
    }
}

@Composable
private fun DocumentPageImage(path: String?, index: Int, policy: PageLayoutPolicy) {
    var bitmap by remember(path, index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path, index) {
        bitmap = withContext(Dispatchers.IO) { renderPdfPageAt(path, index) }
    }
    // an A4 sheet at the usable width — the placeholder holds the same box so nothing jumps
    val sheetHeight = (policy.usableWidth * PageLayoutPolicy.A4_RATIO).dp
    val frame = Modifier.fillMaxWidth().height(sheetHeight).clip(RoundedCornerShape(4.dp))
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.Fit,
            modifier = frame.background(PaperInk.copy(alpha = .04f)),
        )
    } ?: Box(frame.background(PaperInk.copy(alpha = .06f)), contentAlignment = Alignment.Center) {
        Text("${index + 1}", style = typography.labelSmall, color = PaperInk.copy(alpha = .4f))
    }
}
