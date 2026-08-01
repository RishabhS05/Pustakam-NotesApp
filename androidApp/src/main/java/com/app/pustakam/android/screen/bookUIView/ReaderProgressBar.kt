package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme

@Composable
fun ReaderProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.secondary
                )
        )
    }
}
@Preview
@Composable
private fun ReaderProgressBarPreview() {

    MyApplicationTheme() {

        ReaderProgressBar(
            progress = .34f
        )
    }
}