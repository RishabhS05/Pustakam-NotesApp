package com.app.pustakam.android.screen.bookUIView

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.typography
import com.app.pustakam.core.filesys.reader.ReaderBlock

@Composable
fun LinkBlockView(block: ReaderBlock.Link, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .background(CoverColor.copy(alpha = .07f), RoundedCornerShape(8.dp))
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(block.url)))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(14.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = CoverColor)
        Text(
            block.url, style = typography.bodyMedium, color = Color(0xFF1A5276),
            maxLines = 2, modifier = Modifier.padding(top = 6.dp),
        )
    }
}
