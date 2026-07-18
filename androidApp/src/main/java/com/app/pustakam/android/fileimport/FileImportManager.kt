package com.app.pustakam.android.fileimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.FileImportHelper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// 🔧 18-Jul-2026: NEW FEATURE (file import) — outcome of a link import; NoFileFound drives the
//   "No file found" message the user asked for.
sealed class ImportResult {
    data class Success(val contents: List<NoteContentModel.MediaContent>) : ImportResult()
    data object NoFileFound : ImportResult()
    data class Failed(val message: String) : ImportResult()
}

// 🔧 18-Jul-2026: NEW FEATURE (file import) — copies device picks / URL downloads into app storage
//   (filesDir/imported/<noteId>/) and builds MediaContent via the SHARED FileImportHelper.
//   All functions are blocking — callers run them on Dispatchers.IO.
object FileImportManager {

    private const val MAX_REDIRECTS = 5

    // 🔧 18-Jul-2026: private destination folder per note — SAF grants are transient, a copy is not
    private fun destinationDir(context: Context, noteId: String): File =
        File(context.filesDir, "imported/$noteId").apply { mkdirs() }

    // 🔧 18-Jul-2026: unique dest file — timestamp prefix only when the name already exists
    private fun destinationFile(dir: File, fileName: String): File {
        val safe = fileName.replace('/', '_')
        val candidate = File(dir, safe)
        return if (!candidate.exists()) candidate else File(dir, "${System.currentTimeMillis()}_$safe")
    }

    /** Import multiple SAF-picked uris. Returns one MediaContent per successfully copied file. */
    fun importUris(
        context: Context, noteId: String, startPosition: Double, uris: List<Uri>,
    ): List<NoteContentModel.MediaContent> {
        val dir = destinationDir(context, noteId)
        var position = startPosition
        return uris.mapNotNull { uri ->
            try {
                // 🔧 18-Jul-2026: display name + size from OpenableColumns; mime from the resolver
                var name = "import-${System.currentTimeMillis()}"
                var size = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) cursor.getString(nameIdx)?.let { name = it }
                        if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                    }
                }
                val mime = context.contentResolver.getType(uri)
                val dest = destinationFile(dir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                } ?: return@mapNotNull null
                if (size == 0L) size = dest.length()
                // 🔧 18-Jul-2026: shared factory resolves ContentType (ext → mime → OTHER fallback)
                FileImportHelper.createImportedMedia(
                    noteId = noteId, positionedAt = position, localPath = dest.absolutePath,
                    fileName = name, mime = mime, sizeBytes = size,
                ).also { position += 1.0 }
            } catch (e: Exception) {
                e.printStackTrace(); null
            }
        }
    }

    /** Import from ANY link: downloads when the response is a real file, else NoFileFound. */
    fun importFromUrl(
        context: Context, noteId: String, startPosition: Double, rawUrl: String,
    ): ImportResult {
        // 🔧 18-Jul-2026: be forgiving with pasted links — default to https
        val urlText = rawUrl.trim().let { if (it.startsWith("http")) it else "https://$it" }
        return try {
            var connection = (URL(urlText).openConnection() as HttpURLConnection)
            var redirects = 0
            // 🔧 18-Jul-2026: manual redirect loop — HttpURLConnection won't hop http↔https itself
            while (true) {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: return ImportResult.NoFileFound
                    connection.disconnect()
                    if (++redirects > MAX_REDIRECTS) return ImportResult.NoFileFound
                    connection = (URL(URL(urlText), location).openConnection() as HttpURLConnection)
                    continue
                }
                if (code !in 200..299) return ImportResult.NoFileFound
                break
            }
            val mime = connection.contentType
            // 🔧 18-Jul-2026: prefer the server-declared name (Content-Disposition) over the URL tail
            val disposition = connection.getHeaderField("Content-Disposition")
            val fileName = dispositionFileName(disposition) ?: FileImportHelper.fileNameFromUrl(urlText)
            // 🔧 18-Jul-2026: html pages are NOT files → exactly the "no file found" case requested
            if (!FileImportHelper.isDownloadableFile(mime, fileName)) {
                connection.disconnect()
                return ImportResult.NoFileFound
            }
            val dest = destinationFile(destinationDir(context, noteId), fileName)
            connection.inputStream.use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
            connection.disconnect()
            if (dest.length() == 0L) { dest.delete(); return ImportResult.NoFileFound }
            val content = FileImportHelper.createImportedMedia(
                noteId = noteId, positionedAt = startPosition, localPath = dest.absolutePath,
                fileName = fileName, mime = mime?.substringBefore(';')?.trim(),
                sizeBytes = dest.length(), sourceUrl = urlText,
            )
            ImportResult.Success(listOf(content))
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Failed("Couldn't download from this link. Check the URL and your connection.")
        }
    }

    // 🔧 18-Jul-2026: parses filename="x.pdf" / filename*=UTF-8''x.pdf out of Content-Disposition
    private fun dispositionFileName(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        val starMatch = Regex("filename\\*=(?:UTF-8''|utf-8'')?\"?([^\";]+)\"?").find(disposition)
        val plainMatch = Regex("filename=\"?([^\";]+)\"?").find(disposition)
        return (starMatch ?: plainMatch)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}
