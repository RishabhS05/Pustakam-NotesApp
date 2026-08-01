package com.app.pustakam.android.screen.book

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme

@Composable
fun ReaderActionButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    FilledTonalIconButton(
        modifier = modifier.size(42.dp),
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun ReaderActionButtonPreview() {
    MyApplicationTheme() {
        ReaderActionButton(
            icon = Icons.Outlined.Bookmark
        )
    }
}