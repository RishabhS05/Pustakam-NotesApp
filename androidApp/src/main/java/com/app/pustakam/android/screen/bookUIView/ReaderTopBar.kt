package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.theme.typography

@Composable
fun ReaderTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onTextSettings: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onHighlight: () -> Unit = {}
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            style = typography.titleMedium,
            maxLines = 1
        )

        Spacer(Modifier.weight(1f))

        Row {

            ReaderActionButton(
                icon = Icons.Outlined.TextFields,
                onClick = onTextSettings
            )

            Spacer(Modifier.width(8.dp))

            ReaderActionButton(
                icon = Icons.Outlined.BookmarkBorder,
                onClick = onBookmark
            )

            Spacer(Modifier.width(8.dp))

            ReaderActionButton(
                icon = Icons.Outlined.BorderColor,
                onClick = onHighlight
            )
        }
    }
}
@Preview
@Composable
private fun ReaderProgressBarPreview() {

    MyApplicationTheme() {
        ReaderTopBar(title = "Title of the book")
    }
}