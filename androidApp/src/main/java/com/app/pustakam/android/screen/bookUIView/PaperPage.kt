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
import com.app.pustakam.android.widgets.zoom.zoomable


@Composable
fun PaperPage(
    modifier: Modifier = Modifier,
    background: Color = PaperColor,
    fillHeight: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .then(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
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
