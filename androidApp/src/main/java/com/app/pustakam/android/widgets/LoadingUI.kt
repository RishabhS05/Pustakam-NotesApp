package com.app.pustakam.android.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingUI(modifier: Modifier = Modifier, alignment: Alignment = Alignment.Center) {
    Box(modifier = modifier
        .fillMaxSize()) {
        CircularProgressIndicator(
            strokeWidth = 6.dp, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.secondary, strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap, modifier = Modifier.align(alignment)
        )
    }
}