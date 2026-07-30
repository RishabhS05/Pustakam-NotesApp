package com.app.pustakam.android.fileimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.filesys.imports.ImportCoordinator
import com.app.pustakam.core.filesys.model.ImportDecision
import com.app.pustakam.core.filesys.model.ImportPlan
import com.app.pustakam.core.filesys.naming.FileNameGenerator
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
//   (filesDir/imported/<noteId>/). 🔧 30-Jul-2026 02:10 Phase 3 — planning is now ImportCoordinator's;
//   this object performs IO only.
//   All functions are blocking — callers run them on Dispatchers.IO.
object FileImportManager {

    private const val MAX_REDIRECTS = 5

// 🔧 30-Jul-2026 02:10 Phase 3 — ImportCoordinator decides WHERE a file goes and WHAT it is called
    //   (validation, sanitising, duplicate resolution). This object now only performs the IO the
    //   plan describes. Destination folder and collision strategy are unchanged.

    /** Turn a plan's relative path into the real file, parents created. */
    private fun fileFor(context: Context, plan: ImportPlan): File =
        File(context.filesDir, plan.relativePath).apply { parentFile?.mkdirs() }

    /** Probe the coordinator uses to detect an already-taken destination. */
    private fun existsIn(context: Context): (String) -> Boolean =
        { relativePath -> File(context.filesDir, relativePath).exists() }

    /** Build the MediaContent for a written plan — one construction path for picks and downloads. */
    private fun mediaFor(plan: ImportPlan, position: Double, file: File): NoteContentModel.MediaContent =
        NoteContentObjectHelper.createMedia(
            contentType = plan.contentType,
            noteId = plan.destination.folder.substringAfterLast('/'),
            positionedAt = position,
            localPath = file.absolutePath,
            url = plan.sourceUrl,
            title = plan.displayName,
            mimeType = plan.mimeType,
            sizeBytes = if (plan.sizeBytes > 0) plan.sizeBytes else file.length(),
        )

    /** Import multiple SAF-picked uris. Returns one MediaContent per successfully copied file. */
    // 🔧 30-Jul-2026 02:10 Phase 3 — planning moved to ImportCoordinator; this now only reads the
    //   uri metadata, then copies bytes for each accepted plan. Batch planning also means two picked
    //   files with the SAME name can no longer resolve to the same destination — nothing is on disk
    //   yet at plan time, so only in-batch reservation can catch that.
    fun importUris(
        context: Context, noteId: String, startPosition: Double, uris: List<Uri>,
    ): List<NoteContentModel.MediaContent> {
        // 🔧 18-Jul-2026: display name + size from OpenableColumns; mime from the resolver
        // 🔧 30-Jul-2026 02:10 Phase 3 — plan and copy ONE uri at a time, carrying the set of
        //   names already reserved in this batch. Two picked files with the same name therefore
        //   cannot resolve to the same destination — nothing is on disk yet at plan time, so only
        //   in-batch reservation can catch that. Pairing stays trivially correct: no re-matching.
        val reserved = mutableSetOf<String>()
        var position = startPosition

        return uris.mapNotNull { uri ->
            try {
                // 🔧 18-Jul-2026: display name + size from OpenableColumns; mime from the resolver
                var name = "import-${getCurrentTimestamp()}"
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

                // SAF often omits SIZE; 0 means "unknown" here, not "empty", so pass 1 and read the
                // real length back after the copy.
                val decision = ImportCoordinator.planImport(
                    noteId = noteId,
                    requestedName = name,
                    mime = mime,
                    sizeBytes = if (size > 0) size else 1L,
                    timestamp = getCurrentTimestamp(),
                    exists = { path -> path in reserved || File(context.filesDir, path).exists() },
                )
                val plan = (decision as? ImportDecision.Accepted)?.plan
                    ?: return@mapNotNull null
                reserved += plan.relativePath

                val dest = fileFor(context, plan)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                } ?: return@mapNotNull null

                mediaFor(plan, position, dest).also { position += 1.0 }
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
// 🔧 30-Jul-2026 02:10 Phase 3 — naming + validation + destination now come from ImportCoordinator.
            //   The DOWNLOAD MECHANICS ABOVE ARE UNTOUCHED — that path is verified working.
            //   fromUrl() is now mime-aware, so an extensionless URL serving a known type gets the
            //   right extension instead of being rejected (see FIA doc §3.3).
            val fileName = dispositionFileName(disposition)
                ?: FileNameGenerator.fromUrl(urlText, mime, getCurrentTimestamp())
            // Size is unknown before the body is read; 1 means "unknown", not "empty".
            val plan = when (val decision = ImportCoordinator.planImport(
                noteId = noteId,
                requestedName = fileName,
                mime = mime,
                sizeBytes = 1L,
                sourceUrl = urlText,
                timestamp = getCurrentTimestamp(),
                exists = existsIn(context),
            )) {
                is ImportDecision.Accepted -> decision.plan
                // 🔧 18-Jul-2026: html pages are NOT files → the "no file found" case, unchanged
                is ImportDecision.NotAFile -> { connection.disconnect(); return ImportResult.NoFileFound }
                is ImportDecision.Rejected -> { connection.disconnect(); return ImportResult.NoFileFound }
            }
            val dest = fileFor(context, plan)
            connection.inputStream.use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
            connection.disconnect()
            if (dest.length() == 0L) { dest.delete(); return ImportResult.NoFileFound }
            ImportResult.Success(listOf(mediaFor(plan, startPosition, dest)))
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
