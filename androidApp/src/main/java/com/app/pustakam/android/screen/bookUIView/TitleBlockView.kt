package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.core.filesys.reader.ReaderBlock

@Composable
fun TitleBlockView(block: ReaderBlock.Title, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            block.text,
            style = typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = PaperInk, textAlign = TextAlign.Center,
        )
        Box(Modifier.padding(vertical = 6.dp).width(60.dp).height(2.dp).background(CoverColor.copy(alpha = .5f)))
        block.subtitle?.let {
            Text(it, style = typography.labelMedium, color = PaperInk.copy(alpha = .6f))
        }
    }
}
