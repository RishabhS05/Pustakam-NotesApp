package com.app.pustakam.android.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.widgets.bookwidget.BookLoadingAnimation

@Composable
fun LoadingUI(modifier: Modifier = Modifier, alignment: Alignment = Alignment.Center) {
    Box(modifier = modifier
        .fillMaxSize()) {
        BookLoadingAnimation(
            modifier = Modifier.align(alignment)
        )
    }
}