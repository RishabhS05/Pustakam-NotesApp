package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.document.iconForContentType
import com.app.pustakam.android.widgets.document.readableSize
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 01-Aug-2026: inside a NOTE a document is a compact card, not N inline sheets — tapping it
//   opens the dedicated document reader. This is what stops a 1443-page PDF exploding the note.
// 📖 15-Aug-2026: with InlineDocumentUiState.enabled the card also reads in place — Read opens the
//   first 10 sheets under this card, the arrow icon still opens the dedicated reader.
@Composable
fun DocumentBlockView(
    block: ReaderBlock.Document,
    modifier: Modifier = Modifier,
    state: InlineDocumentUiState = InlineDocumentUiState.Disabled,
    onOpen: (NoteContentModel.MediaContent) -> Unit = {},
) {
    val media = block.item
    val expanded = state.isExpanded(media.id)
    val readsInline = state.enabled && state.isReadable(media.id)
    Row(
        modifier
            .fillMaxWidth()
            .background(CoverColor.copy(alpha = .07f), RoundedCornerShape(8.dp))
            .clickable { if (readsInline) state.onToggle(media) else onOpen(media) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).background(CoverColor.copy(alpha = .12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(iconForContentType(media.type), media.type.name, tint = CoverColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                media.title.ifBlank { "File" }, style = typography.titleSmall,
                color = PaperInk, maxLines = 2,
            )
            Text(
                listOf(media.type.name, readableSize(media.sizeBytes)).filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = typography.labelSmall, color = PaperInk.copy(alpha = .6f),
            )
        }
        if (readsInline) {
            if (state.isBusy(media.id)) {
                CircularProgressIndicator(Modifier.size(18.dp), color = CoverColor, strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = { state.onToggle(media) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        if (expanded) "Close" else "Read",
                        style = typography.labelMedium, color = CoverColor,
                    )
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse document" else "Read document here",
                        tint = CoverColor, modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        // 📖 a probed file we cannot draw inline says so, instead of silently dropping the button
        if (state.enabled && !state.isReadable(media.id)) {
            Text(
                "Can't open here", style = typography.labelSmall,
                color = PaperInk.copy(alpha = .6f),
            )
        }
        if (state.enabled) {
            IconButton(onClick = { onOpen(media) }, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open full document",
                    tint = CoverColor.copy(alpha = .7f), modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
