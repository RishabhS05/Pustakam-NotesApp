package com.app.pustakam.core.filesys.fileimport

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.filesys.mime.MimeCatalog
import com.app.pustakam.core.filesys.naming.FileNameGenerator
import com.app.pustakam.core.filesys.validation.ImportValidator
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteContentObjectHelper

// 🔧 18-Jul-2026: NEW FEATURE (file import) — single shared source of truth for BOTH platforms:
//   extension/mime → ContentType resolution, URL file-name parsing, and the MediaContent factory.
// 🔧 30-Jul-2026 02:10 Phase 1 — the tables and rules MOVED to MimeCatalog / FileNameGenerator /
//   ImportValidator; every function below is now a thin delegate holding no logic of its own.
//   The public API is UNCHANGED on purpose: Swift calls FileImportHelper.shared.* and Android calls
//   FileImportHelper.* today. This object is retired in Phase 5, not before.
object FileImportHelper {

    // 🔧 30-Jul-2026 02:10 delegate → MimeCatalog (was a private extensionMap duplicated here)
    fun resolveContentType(fileName: String, mime: String? = null): ContentType =
        MimeCatalog.contentTypeFor(fileName, mime)

    // 🔧 30-Jul-2026 02:10 delegate → MimeCatalog
    fun contentTypeForMime(mime: String?): ContentType? = MimeCatalog.contentTypeForMime(mime)

    // 🔧 30-Jul-2026 02:10 delegate → MimeCatalog
    fun mimeFor(type: ContentType): String = MimeCatalog.mimeFor(type)

    // 🔧 30-Jul-2026 02:10 delegate → FileNameGenerator. Existing signature kept so no call site
    //   changes; the mime-aware overload is what Phase 2's shared downloader will use.
    fun fileNameFromUrl(url: String): String =
        FileNameGenerator.fromUrl(url, mime = null, timestamp = getCurrentTimestamp())

    fun fileNameFromUrl(url: String, mime: String?): String =
        FileNameGenerator.fromUrl(url, mime, timestamp = getCurrentTimestamp())

    // 🔧 30-Jul-2026 02:10 delegate → ImportValidator
    fun isDownloadableFile(mime: String?, fileName: String): Boolean =
        ImportValidator.isDownloadable(mime, fileName)

    // 🔧 18-Jul-2026: the ONE factory both platforms call after landing a file locally
    fun createImportedMedia(
        noteId: String, positionedAt: Double, localPath: String,
        fileName: String, mime: String? = null, sizeBytes: Long = 0, sourceUrl: String = "",
    ): NoteContentModel.MediaContent {
        val type = resolveContentType(fileName, mime)
        return NoteContentObjectHelper.createMedia(
            contentType = type, noteId = noteId, positionedAt = positionedAt,
            localPath = localPath, url = sourceUrl,
            title = fileName,
            mimeType = mime ?: mimeFor(type),
            sizeBytes = sizeBytes,
        )
    }
}
