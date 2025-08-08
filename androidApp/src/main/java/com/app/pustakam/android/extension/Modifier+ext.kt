package com.app.pustakam.android.extension

import allGradient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import sideblueGradient

fun Modifier.glassCrystalEffect(): Modifier {
    return  this.drawBehind {
        drawRect(
            Brush.sweepGradient(
                colors = sideblueGradient,
            ), alpha = 0.3f,
            style = Stroke(width = 10.dp.toPx())
        )
    }
        .blur(60.dp) // needs Accompanist or custom modifier
        .shadow(16.dp, RoundedCornerShape(10.dp), clip = false)
}
fun Modifier.thickGlass() : Modifier {
    return this.clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(alpha = 0.1f))
        .drawBehind {
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .blur(8.dp) // needs Accompanist or custom modifier
}