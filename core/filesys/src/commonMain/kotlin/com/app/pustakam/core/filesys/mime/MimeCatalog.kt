package com.app.pustakam.core.filesys.mime

import com.app.pustakam.core.common.util.ContentType

// 🔧 30-Jul-2026 02:10 Phase 1 — THE single mime/extension table for the whole app.
//   Tables lifted VERBATIM from FileImportHelper (the correct copy). Supersedes the second, stale
//   copy in androidApp/fileUtils/FileOps.mimeTypeFor, which was missing TXT/MD/EPUB.
//   Pure: no platform API, no clock, no IO.
object MimeCatalog {

    const val OCTET_STREAM = "application/octet-stream"

    // extension (lowercased, no dot) -> ContentType. OTHER is the attach-anything fallback.
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

    /** Strips parameters and case: "text/plain; charset=utf-8" -> "text/plain". */
    fun normalize(mime: String?): String? =
        mime?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    /** Canonical mime for a ContentType. Unmapped types fall back to octet-stream. */
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
        else -> OCTET_STREAM
    }

    /** mime prefix/exact -> ContentType; null when the mime says nothing useful. Order matters:
     *  image/gif is tested before the image/ prefix. */
    fun contentTypeForMime(mime: String?): ContentType? {
        val m = normalize(mime) ?: return null
        return when {
            m.startsWith("image/gif") -> ContentType.GIF
            m.startsWith("image/") -> ContentType.IMAGE
            m.startsWith("video/") -> ContentType.VIDEO
            m.startsWith("audio/") -> ContentType.AUDIO
            m == "application/pdf" -> ContentType.PDF
            m == "application/msword" ||
                m == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ContentType.DOCX
            m == "application/epub+zip" -> ContentType.EPUB
            m == "text/markdown" -> ContentType.MD
            m.startsWith("text/") -> ContentType.TXT
            else -> null
        }
    }

    /** ContentType -> canonical extension including the dot (""), sourced from ContentType itself. */
    fun extensionFor(type: ContentType): String = type.getExt()

    /** Extension (with or without dot, any case) -> ContentType, or null when unknown. */
    fun contentTypeForExtension(extension: String?): ContentType? {
        val ext = extension?.removePrefix(".")?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return extensionMap[ext]
    }

    /** Name first, mime second, OTHER last — an import is never rejected purely on type. */
    fun contentTypeFor(fileName: String, mime: String? = null): ContentType =
        contentTypeForExtension(fileName.substringAfterLast('.', "")) 
            ?: contentTypeForMime(mime)
            ?: ContentType.OTHER

    /** True when we recognise this mime as a concrete file type we can classify. */
    fun isSupported(mime: String?): Boolean = contentTypeForMime(mime) != null
}
