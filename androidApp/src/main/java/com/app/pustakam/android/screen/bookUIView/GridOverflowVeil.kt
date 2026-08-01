package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.pustakam.android.theme.typography

// 📖 01-Aug-2026: the "+N" veil. It sits ON TOP of the last cell and owns the click, which is why
//   tapping it opens the full set instead of the single item underneath.
@Composable
fun GridOverflowVeil(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (count <= 0) return
    Box(
        modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text("+$count", style = typography.headlineSmall, color = Color.White)
    }
}
