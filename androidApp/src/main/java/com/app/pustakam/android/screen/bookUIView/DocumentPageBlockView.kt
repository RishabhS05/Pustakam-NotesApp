package com.app.pustakam.android.screen.bookUIView

import android.R.attr.scheme
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.bookwidget.BookLoadingAnimation
import com.app.pustakam.android.widgets.document.iconForContentType
import com.app.pustakam.android.widgets.zoom.zoomable
import com.app.pustakam.core.filesys.reader.EmbeddedDocumentKind
import com.app.pustakam.core.filesys.reader.ReaderBlock

// 📖 15-Aug-2026: ONE sheet of a document opened inside the note. The bar on top belongs to every
//   sheet, so the document can be collapsed from anywhere in either reading mode.
@Composable
fun DocumentPageBlockView(
    block: ReaderBlock.DocumentPage,
    modifier: Modifier = Modifier,
    state: InlineDocumentUiState = InlineDocumentUiState.Disabled,
) {
    val ref = block.ref
    val media = block.item
    Column(modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(CoverColor.copy(alpha = .07f), RoundedCornerShape(6.dp))
                .clickable { state.onToggle(media) }

                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                iconForContentType(media.type), media.type.name,
                tint = CoverColor, modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                media.title.ifBlank { "File" }, style = typography.labelMedium, color = PaperInk,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text(
                "${ref.pageNumber} of ${ref.pageCount}",
                style = typography.labelSmall, color = PaperInk.copy(alpha = .6f),
            )
            Icon(
                Icons.Filled.KeyboardArrowUp, contentDescription = "Collapse document",
                tint = CoverColor, modifier = Modifier.size(18.dp),
            )
        }
        // 📖 zoomable() consumes gestures only while pinching or already zoomed, so a one-finger
        //   scroll or page flip still passes through at 1x
        Box(Modifier.weight(1f).fillMaxWidth().zoomable()) {
            when (ref.kind) {
                EmbeddedDocumentKind.PDF -> PdfSheet(
                    path = media.localPath,
                    pageIndex = ref.pageIndex,
                    label = "${media.title} page ${ref.pageNumber}",
                )

                EmbeddedDocumentKind.TEXT -> Text(
                    block.text.orEmpty(),
                    style = typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 26.sp),
                    color = PaperInk,
                )

                EmbeddedDocumentKind.UNSUPPORTED -> SheetMessage("This file can only be opened outside the note")
            }
        }
        if (ref.showsLoadMore) {
            TextButton(
                onClick = { state.onLoadMore(ref.contentId) },
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Load more (${ref.remaining} left)", style = typography.labelLarge, color = colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PdfSheet(path: String?, pageIndex: Int, label: String) {
    if (path.isNullOrBlank()) {
        SheetMessage("File not available offline")
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // 📖 ask for exactly the width this sheet draws at — never a full-screen bitmap in a card
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        var bitmap by remember(path, pageIndex, widthPx) {
            mutableStateOf<Bitmap?>(PdfPageRenderer.cached(path, pageIndex, widthPx))
        }
        LaunchedEffect(path, pageIndex, widthPx) {
            if (bitmap == null) bitmap = PdfPageRenderer.render(path, pageIndex, widthPx)
        }
        bitmap?.let {
            // 📖 the sheet spans the full width of the page and starts at the top
            Image(
                bitmap = it.asImageBitmap(), contentDescription = label,
                contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth(),
            )
        } ?: BookLoadingAnimation(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SheetMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message, style = typography.labelMedium, color = PaperInk.copy(alpha = .6f),
            textAlign = TextAlign.Center,
        )
    }
}
