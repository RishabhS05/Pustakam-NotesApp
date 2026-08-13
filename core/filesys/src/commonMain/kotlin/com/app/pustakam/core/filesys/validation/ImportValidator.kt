package com.app.pustakam.core.filesys.validation

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.mime.MimeCatalog

object ImportValidator {

    /** Mime types that are a web page, never a file the user meant to attach. */
    private val PAGE_MIMES = setOf("text/html", "application/xhtml+xml")

    /**
     * True when a link's response is a real file rather than a web page.
     * Lifted verbatim from FileImportHelper.isDownloadableFile so link-import behaviour is
     * bit-identical after the move.
     */
    fun isDownloadable(mime: String?, fileName: String): Boolean {
        val m = MimeCatalog.normalize(mime)
        if (m != null && m in PAGE_MIMES) return false
        return fileName.contains('.') || MimeCatalog.contentTypeForMime(m) != null
    }

    /**
     * Can this file be brought into a note? The app deliberately accepts anything (OTHER is the
     * fallback), so the only hard rejections are an empty name and an empty payload.
     */
    fun canImport(fileName: String, mime: String? = null, sizeBytes: Long = 1L): Boolean {
        if (fileName.isBlank()) return false
        if (sizeBytes <= 0L) return false
        return isDownloadable(mime, fileName)
    }

    /** Every note can be exported; the format decides the renderer, not the content. */
    fun canExport(type: ContentType): Boolean = true

    /** True when we can classify this mime as a concrete type. */
    fun isSupported(mime: String?): Boolean = MimeCatalog.isSupported(mime)

}
