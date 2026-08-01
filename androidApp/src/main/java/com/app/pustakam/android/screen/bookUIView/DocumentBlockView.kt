package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
@Composable
fun DocumentBlockView(
    block: ReaderBlock.Document,
    modifier: Modifier = Modifier,
    onOpen: (NoteContentModel.MediaContent) -> Unit = {},
) {
    val media = block.item
    Row(
        modifier
            .fillMaxWidth()
            .background(CoverColor.copy(alpha = .07f), RoundedCornerShape(8.dp))
            .clickable { onOpen(media) }
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
    }
}
