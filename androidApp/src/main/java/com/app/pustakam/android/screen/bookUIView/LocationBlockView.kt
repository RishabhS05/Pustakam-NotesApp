package com.app.pustakam.android.screen.bookUIView

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.core.filesys.reader.ReaderBlock

@Composable
fun LocationBlockView(block: ReaderBlock.Location, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .background(CoverColor.copy(alpha = .07f), RoundedCornerShape(8.dp))
            .clickable {
                val geo = Uri.parse(
                    "geo:${block.latitude},${block.longitude}?q=${block.latitude},${block.longitude}"
                )
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocationOn, null, tint = CoverColor, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                block.address ?: "%.5f, %.5f".format(block.latitude, block.longitude),
                style = typography.titleSmall, color = PaperInk,
            )
            if (block.address != null) Text(
                "%.5f, %.5f".format(block.latitude, block.longitude),
                style = typography.labelSmall, color = PaperInk.copy(alpha = .6f),
            )
        }
    }
}
