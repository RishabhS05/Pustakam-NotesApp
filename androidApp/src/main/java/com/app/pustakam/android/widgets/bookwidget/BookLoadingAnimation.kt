@file:Suppress("MagicNumber")

package com.app.pustakam.android.widgets.bookwidget

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import tertiaryLight

@Composable
fun BookLoadingAnimation(
    modifier: Modifier = Modifier,
    bookWidth: Int = 80,
    bookHeight: Int = 70
) {

    val transition = rememberInfiniteTransition(label = "book")

    val writeProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "writeProgress"
    )
    Box(
        modifier = modifier
            .size(bookWidth.dp, bookHeight.dp)
            .shadow(elevation = 30.dp, spotColor = Color.Black),
        contentAlignment = Alignment.Center
    ) {

        //----------------------------------------
        // Book Cover
        //----------------------------------------
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.secondary)
        )

        //----------------------------------------
// Writing Animation
//----------------------------------------
        val totalLines = 3
        Box(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .fillMaxWidth(0.48f)
                .align(Alignment.CenterStart)
        ) {
            //----------------------------------------
            // Left Page
            //----------------------------------------
            // Paper
            Page(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterStart)
                    .offset(x = 3.dp),
                isLeft = true
            )
            Canvas(
                modifier = Modifier.fillMaxSize()
            )
            {

                val left = size.width * 0.12f
                val right = size.width * 0.88f

                val lineSpacing = size.height * 0.22f
                val top = size.height * 0.22f



                val overall = writeProgress * totalLines

                val currentLine = overall.toInt().coerceAtMost(totalLines - 1)
                val lineProgress = overall - currentLine

                for (i in 0 until totalLines) {

                    val y = top + i * lineSpacing

                    val endX = when {

                        i < currentLine ->
                            right

                        i == currentLine ->
                            left + (right - left) * lineProgress

                        else ->
                            left
                    }

                    drawLine(
                        color = tertiaryLight,
                        start = Offset(left, y),
                        end = Offset(endX, y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

        }
//
//            //----------------------------------------
//            // Right Page
//            //----------------------------------------
        Box(modifier = Modifier.fillMaxSize()
            .align(Alignment.CenterEnd)) {
            Page(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .fillMaxWidth(0.48f)
                    .align(Alignment.CenterEnd)
                    .offset(
                        x = -3.dp
                    ),
                isLeft = false
            )
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight(0.93f)

                    .background(MaterialTheme.colorScheme.secondary)
                    .align(Alignment.Center)
            )
            // Pencil
            val overall = writeProgress * totalLines
            val currentLine = overall.toInt().coerceAtMost(totalLines - 1)
            val lineProgress = overall - currentLine

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = tertiaryLight,
                modifier = Modifier
                    .size(20.dp)
                    .offset(
                        x = (8 + lineProgress * 24).dp,
                        y = (currentLine * 14).dp
                    )
                    .rotate(0f)
            )
        }
        //----------------------------------------
        // Bottom Shadow
        //----------------------------------------

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .align(Alignment.BottomCenter)
        ) {
            drawOval(
                color = Color.Black.copy(alpha = 0.12f),
                topLeft = Offset(
                    size.width * 0.18f,
                    size.height * 0.1f
                ),
                size = Size(
                    size.width * 0.64f,
                    size.height * 0.8f
                )
            )
        }
    }
}

@Composable
private fun Page(
    modifier: Modifier,
    isLeft: Boolean
) {

    Box(
        modifier
            .clip(
                RoundedCornerShape(
                    topStart = if (isLeft) 4.dp else 0.dp,
                    bottomStart = if (isLeft) 4.dp else 0.dp,
                    topEnd = if (!isLeft) 4.dp else 0.dp,
                    bottomEnd = if (!isLeft) 4.dp else 0.dp
                )
            )
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF6F6F6),
                        Color.White
                    )
                )
            )
    ) {

        Canvas(Modifier.fillMaxSize()) {

            val lineColor = Color(0xFFE8E8E8)

            val gap = size.height / 9f

            var y = gap

            repeat(8) {
                drawLine(
                    color = lineColor,
                    start = Offset(6f, y),
                    end = Offset(size.width - 6f, y),
                    strokeWidth = 1.2f
                )

                y += gap
            }

        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
    widthDp = 200,
    heightDp = 200
)
@Composable
private fun BookLoadingAnimationPreview() {

    MyApplicationTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BookLoadingAnimation()
        }
    }
}