package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.core.filesys.reader.ReaderBlock

@Composable
fun ParagraphBlockView(block: ReaderBlock.Paragraph, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            block.text,
            style = typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 26.sp),
            color = PaperInk,
        )
        // continuation marker only when the engine had to split this paragraph across pages
        if (block.chunkCount > 1) Text(
            "· ${block.chunkIndex} of ${block.chunkCount} ·",
            style = typography.labelSmall, color = PaperInk.copy(alpha = .5f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        )
    }
}
