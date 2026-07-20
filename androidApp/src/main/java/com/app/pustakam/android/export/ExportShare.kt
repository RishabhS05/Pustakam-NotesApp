package com.app.pustakam.android.export

// 🔧 20-Jul-2026: NEW FEATURE (export) — share an exported file via the system share sheet using
//   the app's existing FileProvider authority (declared in the manifest for the book reader).
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.app.pustakam.export.ExportFormat
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
