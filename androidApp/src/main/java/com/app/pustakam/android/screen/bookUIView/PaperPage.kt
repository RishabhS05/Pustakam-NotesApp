package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.PaperColor

@Composable
fun PaperPage(background: Color = PaperColor, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
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