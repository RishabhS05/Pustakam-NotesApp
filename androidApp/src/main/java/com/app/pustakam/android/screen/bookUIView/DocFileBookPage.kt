package com.app.pustakam.android.screen.bookUIView

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.app.pustakam.core.filesys.mime.MimeCatalog
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import java.io.File

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.document.iconForContentType
import com.app.pustakam.android.widgets.document.readableSize

@Composable
 fun DocFileBookPage(media: NoteContentModel.MediaContent) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(74.dp).background(CoverColor.copy(alpha = .12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconForContentType(media.type), media.type.name, tint = CoverColor, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(media.title.ifBlank { "File" }, style = typography.titleSmall, color = PaperInk, textAlign = TextAlign.Center)
        Text(
            listOf(media.type.name, readableSize(media.sizeBytes)).filter { it.isNotBlank() }.joinToString(" · "),
            style = typography.labelSmall, color = PaperInk.copy(alpha = .6f)
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = { openWithSystemViewer(context, media) }) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open")
        }
    }
}
// 🔧 18-Jul-2026: private file → content:// grant → ACTION_VIEW (reader apps handle docx/epub)
fun openWithSystemViewer(context: Context, media: NoteContentModel.MediaContent) {
    val path = media.localPath?.takeIf { it.isNotEmpty() } ?: run {
        Toast.makeText(context, "File not available offline", Toast.LENGTH_SHORT).show(); return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, media.mimeType.ifBlank { MimeCatalog.mimeFor(media.type) })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Couldn't open the file", Toast.LENGTH_SHORT).show()
    }
}
