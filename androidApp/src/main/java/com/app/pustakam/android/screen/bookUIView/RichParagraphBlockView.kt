package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.PaperColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.smartText.SmartTextColors
import com.app.pustakam.android.widgets.smartText.SmartTextDisplay
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.core.filesys.reader.ReaderBlock

// 📖 15-Aug-2026: a formatted paragraph on a reader page — drawn by the editor's own renderer,
//   re-inked for paper so it stays legible whatever theme the app is in.
@Composable
fun RichParagraphBlockView(
    block: ReaderBlock.RichParagraph,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        SmartTextDisplay(block = block.block, colors = readerTextColors())
        // continuation marker only when the paragraph had to be cut across pages
        if (block.chunkCount > 1) Text(
            "· ${block.chunkIndex} of ${block.chunkCount} ·",
            style = typography.labelSmall, color = PaperInk.copy(alpha = .5f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        )
    }
}

@Composable
private fun readerTextColors(): SmartTextColors = SmartTextTokens.colors.copy(
    page = PaperColor,
    surface = PaperColor,
    onSurface = PaperInk,
    onSurfaceMuted = PaperInk.copy(alpha = .6f),
)
