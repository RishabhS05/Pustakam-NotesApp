package com.app.pustakam.core.filesys.validation

import com.app.pustakam.core.common.util.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 1 — pins the acceptance rules, including the exact "No file found"
//   behaviour the link-import UX depends on.
class ImportValidatorTest {

    // ---- isDownloadable: the "No file found at this link" contract ----

    @Test
    fun a_web_page_is_never_a_file() {
        assertFalse(ImportValidator.isDownloadable("text/html", "index.html"))
        assertFalse(ImportValidator.isDownloadable("text/html; charset=utf-8", "page.html"))
        assertFalse(ImportValidator.isDownloadable("application/xhtml+xml", "page.xhtml"))
    }

    @Test
    fun a_name_with_an_extension_is_enough() {
        assertTrue(ImportValidator.isDownloadable(null, "report.pdf"))
        assertTrue(ImportValidator.isDownloadable("application/octet-stream", "report.pdf"))
    }

    @Test
    fun a_recognised_mime_is_enough_even_without_an_extension() {
        assertTrue(ImportValidator.isDownloadable("application/pdf", "download"))
    }

    @Test
    fun neither_extension_nor_recognised_mime_is_rejected() {
        assertFalse(ImportValidator.isDownloadable(null, "download"))
        assertFalse(ImportValidator.isDownloadable("application/octet-stream", "download"))
    }

    // ---- canImport ----

    @Test
    fun canImport_rejects_blank_names_and_empty_payloads() {
        assertFalse(ImportValidator.canImport("", "application/pdf", 10))
        assertFalse(ImportValidator.canImport("   ", "application/pdf", 10))
        assertFalse(ImportValidator.canImport("a.pdf", "application/pdf", 0))
        assertFalse(ImportValidator.canImport("a.pdf", "application/pdf", -1))
    }

    @Test
    fun canImport_accepts_any_recognisable_file_including_unknown_types() {
        assertTrue(ImportValidator.canImport("a.pdf", "application/pdf", 10))
        // "attach anything" is deliberate: an unknown extension still imports as OTHER.
        assertTrue(ImportValidator.canImport("weird.zzz", null, 10))
    }

    @Test
    fun canImport_rejects_a_web_page() {
        assertFalse(ImportValidator.canImport("index.html", "text/html", 4096))
    }

    // ---- classification ----

    @Test
    fun isBook_matches_BookPageFactory_exactly() {
        assertTrue(ImportValidator.isBook(ContentType.PDF))
        assertTrue(ImportValidator.isBook(ContentType.TXT))
        assertTrue(ImportValidator.isBook(ContentType.MD))
        // EPUB routes to DocFilePage today — claiming it here would change reader behaviour.
        assertFalse(ImportValidator.isBook(ContentType.EPUB))
        assertFalse(ImportValidator.isBook(ContentType.IMAGE))
        assertFalse(ImportValidator.isBook(ContentType.DOCX))
    }

    @Test
    fun isMedia_covers_image_gif_video_audio_only() {
        listOf(ContentType.IMAGE, ContentType.GIF, ContentType.VIDEO, ContentType.AUDIO)
            .forEach { assertTrue(ImportValidator.isMedia(it), "$it should be media") }
        listOf(ContentType.PDF, ContentType.TXT, ContentType.MD, ContentType.EPUB,
            ContentType.DOCX, ContentType.OTHER, ContentType.TEXT, ContentType.LINK, ContentType.LOCATION)
            .forEach { assertFalse(ImportValidator.isMedia(it), "$it should not be media") }
    }

    @Test
    fun isImage_covers_still_and_animated_raster_only() {
        assertTrue(ImportValidator.isImage(ContentType.IMAGE))
        assertTrue(ImportValidator.isImage(ContentType.GIF))
        assertFalse(ImportValidator.isImage(ContentType.VIDEO))
        assertFalse(ImportValidator.isImage(ContentType.PDF))
    }

    @Test
    fun isGalleryEligible_is_image_and_video_only() {
        // GIF is deliberately excluded: both platforms route it through the document picker.
        assertTrue(ImportValidator.isGalleryEligible(ContentType.IMAGE))
        assertTrue(ImportValidator.isGalleryEligible(ContentType.VIDEO))
        assertFalse(ImportValidator.isGalleryEligible(ContentType.GIF))
        assertFalse(ImportValidator.isGalleryEligible(ContentType.AUDIO))
    }

    @Test
    fun the_classifications_partition_the_media_types_without_overlap() {
        ContentType.entries.forEach { t ->
            assertFalse(
                ImportValidator.isBook(t) && ImportValidator.isMedia(t),
                "$t cannot be both a book and media",
            )
        }
    }

    @Test
    fun canExport_allows_every_type() {
        ContentType.entries.forEach { assertTrue(ImportValidator.canExport(it), "$it") }
    }

    @Test
    fun isSupported_delegates_to_the_mime_catalog() {
        assertTrue(ImportValidator.isSupported("application/epub+zip"))
        assertFalse(ImportValidator.isSupported("application/x-unknown-thing"))
    }

    @Test
    fun classification_is_total_no_type_throws() {
        ContentType.entries.forEach {
            ImportValidator.isBook(it); ImportValidator.isMedia(it)
            ImportValidator.isImage(it); ImportValidator.isGalleryEligible(it)
        }
        assertEquals(13, ContentType.entries.size, "a new ContentType needs classifying above")
    }
}
