package com.app.pustakam.util

import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.NoteContentObjectHelper

// 🔧 18-Jul-2026: NEW FEATURE (file import) — single shared source of truth for BOTH platforms:
//   extension/mime → ContentType resolution, URL file-name parsing, and the MediaContent factory.
object FileImportHelper {

    // 🔧 18-Jul-2026: extension → ContentType (lowercased, no dot); OTHER = attach-anything fallback
    private val extensionMap: Map<String, ContentType> = mapOf(
        "png" to ContentType.IMAGE, "jpg" to ContentType.IMAGE, "jpeg" to ContentType.IMAGE,
        "webp" to ContentType.IMAGE, "heic" to ContentType.IMAGE, "bmp" to ContentType.IMAGE,
        "gif" to ContentType.GIF,
        "mp4" to ContentType.VIDEO, "mov" to ContentType.VIDEO, "mkv" to ContentType.VIDEO,
        "webm" to ContentType.VIDEO, "3gp" to ContentType.VIDEO, "avi" to ContentType.VIDEO,
        "mp3" to ContentType.AUDIO, "m4a" to ContentType.AUDIO, "wav" to ContentType.AUDIO,
        "aac" to ContentType.AUDIO, "ogg" to ContentType.AUDIO, "flac" to ContentType.AUDIO,
        "pdf" to ContentType.PDF,
        "doc" to ContentType.DOCX, "docx" to ContentType.DOCX,
        "txt" to ContentType.TXT, "log" to ContentType.TXT, "json" to ContentType.TXT,
        "csv" to ContentType.TXT, "xml" to ContentType.TXT,
        "md" to ContentType.MD, "markdown" to ContentType.MD,
        "epub" to ContentType.EPUB,
    )

    // 🔧 18-Jul-2026: mime prefix/exact → ContentType (fallback when the name has no extension)
    fun contentTypeForMime(mime: String?): ContentType? = when {
        mime.isNullOrBlank() -> null
        mime.startsWith("image/gif") -> ContentType.GIF
        mime.startsWith("image/") -> ContentType.IMAGE
        mime.startsWith("video/") -> ContentType.VIDEO
        mime.startsWith("audio/") -> ContentType.AUDIO
        mime == "application/pdf" -> ContentType.PDF
        mime == "application/msword" ||
                mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ContentType.DOCX
        mime == "application/epub+zip" -> ContentType.EPUB
        mime == "text/markdown" -> ContentType.MD
        mime.startsWith("text/") -> ContentType.TXT
        else -> null
    }

    // 🔧 18-Jul-2026: name first, mime second, OTHER last — imports never get rejected by type
    fun resolveContentType(fileName: String, mime: String? = null): ContentType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return extensionMap[ext] ?: contentTypeForMime(mime) ?: ContentType.OTHER
    }

    // 🔧 18-Jul-2026: canonical mime for a ContentType (upload/save/export need one)
    fun mimeFor(type: ContentType): String = when (type) {
        ContentType.IMAGE -> "image/png"
        ContentType.GIF -> "image/gif"
        ContentType.VIDEO -> "video/mp4"
        ContentType.AUDIO -> "audio/mpeg"
        ContentType.PDF -> "application/pdf"
        ContentType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ContentType.TXT -> "text/plain"
        ContentType.MD -> "text/markdown"
        ContentType.EPUB -> "application/epub+zip"
        else -> "application/octet-stream"
    }

    // 🔧 18-Jul-2026: last path segment of a URL, query/fragment stripped; safe default when absent
    fun fileNameFromUrl(url: String): String {
        val cleaned = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val segment = cleaned.substringAfterLast('/')
        return if (segment.isNotBlank() && segment.contains('.')) segment
        else "download-${getCurrentTimestamp()}"
    }

    // 🔧 18-Jul-2026: html/no-body responses are NOT files — callers show "No file found"
    fun isDownloadableFile(mime: String?, fileName: String): Boolean {
        val m = mime?.substringBefore(';')?.trim()?.lowercase()
        if (m != null && (m == "text/html" || m == "application/xhtml+xml")) return false
        return fileName.contains('.') || contentTypeForMime(m) != null
    }

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
