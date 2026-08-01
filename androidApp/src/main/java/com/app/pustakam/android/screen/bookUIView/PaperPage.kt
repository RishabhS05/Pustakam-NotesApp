package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.PaperColor

// 📖 01-Aug-2026: [fillHeight]=false lets a sheet wrap its content. It defaults to true so the
//   page-curl reader and the document reader keep the full-screen sheet they had; only scroll mode
//   opts out. Without this the sheet always filled the screen and no packing could ever be seen.
@Composable
fun PaperPage(
    background: Color = PaperColor,
    fillHeight: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .then(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
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
