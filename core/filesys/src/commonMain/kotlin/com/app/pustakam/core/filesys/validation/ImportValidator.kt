package com.app.pustakam.core.filesys.validation

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.mime.MimeCatalog

// 🔧 30-Jul-2026 02:10 Phase 1 — the app's file-acceptance rules, in ONE place.
//   Pure business decisions only: nothing here opens, stats or touches a file.
//   isBook() mirrors BookPageFactory.pagesFor exactly — see ImportValidatorTest.
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

    /**
     * Types the book reader paginates into pages.
     * EXCLUDES EPUB on purpose: BookPageFactory.pagesFor routes EPUB to DocFilePage today, so
     * claiming it here would change reader behaviour. Revisit when EPUB rendering lands.
     */
    fun isBook(type: ContentType): Boolean =
        type == ContentType.PDF || type == ContentType.TXT || type == ContentType.MD

    /** Types with a timeline or a frame — anything a player or gallery handles. */
    fun isMedia(type: ContentType): Boolean =
        type == ContentType.IMAGE || type == ContentType.GIF ||
            type == ContentType.VIDEO || type == ContentType.AUDIO

    /** Still or animated raster image. */
    fun isImage(type: ContentType): Boolean =
        type == ContentType.IMAGE || type == ContentType.GIF

    /** Types that can be written into the system gallery rather than a document folder. */
    fun isGalleryEligible(type: ContentType): Boolean =
        type == ContentType.IMAGE || type == ContentType.VIDEO
}
