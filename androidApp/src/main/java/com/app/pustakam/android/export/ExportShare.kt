package com.app.pustakam.android.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.filesys.export.ExportFormat
import com.app.pustakam.core.filesys.mime.MimeCatalog
import java.io.File

fun shareExportedFile(context: Context, file: File, format: ExportFormat) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share note as ${format.name}"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Couldn't share the file", Toast.LENGTH_SHORT).show()
    }
}
fun shareMediaFile(context: Context, media: NoteContentModel.MediaContent) {
    val path = media.localPath?.takeIf { it.isNotEmpty() }
    val file = path?.let { File(it) }
    if (file == null || !file.exists()) {
        Toast.makeText(context, "File not available to share", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = media.mimeType.ifBlank { MimeCatalog.mimeFor(media.type) }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${media.title.ifBlank { "file" }}"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Couldn't share the file", Toast.LENGTH_SHORT).show()
    }
}
