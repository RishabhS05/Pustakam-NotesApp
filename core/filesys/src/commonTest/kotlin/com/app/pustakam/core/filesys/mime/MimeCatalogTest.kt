package com.app.pustakam.core.filesys.mime

import com.app.pustakam.core.common.util.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 1 — pins the mime table. These expectations ARE the contract:
//   changing one means changing what lands on a user's device.
class MimeCatalogTest {

    // ---- mimeFor: every ContentType, including the three the old Android copy got wrong ----

    @Test
    fun mimeFor_returns_canonical_type_for_every_mapped_content_type() {
        assertEquals("image/png", MimeCatalog.mimeFor(ContentType.IMAGE))
        assertEquals("image/gif", MimeCatalog.mimeFor(ContentType.GIF))
        assertEquals("video/mp4", MimeCatalog.mimeFor(ContentType.VIDEO))
        assertEquals("audio/mpeg", MimeCatalog.mimeFor(ContentType.AUDIO))
        assertEquals("application/pdf", MimeCatalog.mimeFor(ContentType.PDF))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            MimeCatalog.mimeFor(ContentType.DOCX),
        )
    }

    @Test
    fun mimeFor_TXT_MD_EPUB_are_not_octet_stream() {
        // Regression guard for the stale FileOps.mimeTypeFor copy, which returned octet-stream
        // for these three and made saved .txt/.md/.epub files unopenable.
        assertEquals("text/plain", MimeCatalog.mimeFor(ContentType.TXT))
        assertEquals("text/markdown", MimeCatalog.mimeFor(ContentType.MD))
        assertEquals("application/epub+zip", MimeCatalog.mimeFor(ContentType.EPUB))
    }

    @Test
    fun mimeFor_unmapped_types_fall_back_to_octet_stream() {
        assertEquals(MimeCatalog.OCTET_STREAM, MimeCatalog.mimeFor(ContentType.OTHER))
        assertEquals(MimeCatalog.OCTET_STREAM, MimeCatalog.mimeFor(ContentType.TEXT))
        assertEquals(MimeCatalog.OCTET_STREAM, MimeCatalog.mimeFor(ContentType.LINK))
        assertEquals(MimeCatalog.OCTET_STREAM, MimeCatalog.mimeFor(ContentType.LOCATION))
    }

    @Test
    fun every_content_type_yields_a_non_blank_mime() {
        ContentType.entries.forEach { assertTrue(MimeCatalog.mimeFor(it).isNotBlank(), "$it") }
    }

    // ---- contentTypeForMime ----

    @Test
    fun gif_is_matched_before_the_generic_image_prefix() {
        assertEquals(ContentType.GIF, MimeCatalog.contentTypeForMime("image/gif"))
        assertEquals(ContentType.IMAGE, MimeCatalog.contentTypeForMime("image/jpeg"))
    }

    @Test
    fun markdown_is_matched_before_the_generic_text_prefix() {
        assertEquals(ContentType.MD, MimeCatalog.contentTypeForMime("text/markdown"))
        assertEquals(ContentType.TXT, MimeCatalog.contentTypeForMime("text/plain"))
    }

    @Test
    fun mime_parameters_and_case_are_ignored() {
        assertEquals(ContentType.TXT, MimeCatalog.contentTypeForMime("TEXT/PLAIN; charset=utf-8"))
        assertEquals(ContentType.PDF, MimeCatalog.contentTypeForMime("  application/PDF  "))
    }

    @Test
    fun unsupported_and_absent_mimes_return_null() {
        assertNull(MimeCatalog.contentTypeForMime(null))
        assertNull(MimeCatalog.contentTypeForMime(""))
        assertNull(MimeCatalog.contentTypeForMime("   "))
        assertNull(MimeCatalog.contentTypeForMime("application/x-unknown-thing"))
        assertNull(MimeCatalog.contentTypeForMime(MimeCatalog.OCTET_STREAM))
    }

    @Test
    fun isSupported_tracks_contentTypeForMime() {
        assertTrue(MimeCatalog.isSupported("application/pdf"))
        assertFalse(MimeCatalog.isSupported("application/x-unknown-thing"))
        assertFalse(MimeCatalog.isSupported(null))
    }

    // ---- extension resolution ----

    @Test
    fun extension_lookup_is_case_and_dot_insensitive() {
        assertEquals(ContentType.IMAGE, MimeCatalog.contentTypeForExtension("PNG"))
        assertEquals(ContentType.IMAGE, MimeCatalog.contentTypeForExtension(".png"))
        assertEquals(ContentType.MD, MimeCatalog.contentTypeForExtension("markdown"))
        assertNull(MimeCatalog.contentTypeForExtension(""))
        assertNull(MimeCatalog.contentTypeForExtension(null))
        assertNull(MimeCatalog.contentTypeForExtension("zzz"))
    }

    @Test
    fun contentTypeFor_prefers_the_file_name_over_the_mime() {
        // A server that mislabels a PDF as octet-stream must not defeat the extension.
        assertEquals(ContentType.PDF, MimeCatalog.contentTypeFor("report.pdf", MimeCatalog.OCTET_STREAM))
    }

    @Test
    fun contentTypeFor_falls_back_to_mime_then_OTHER() {
        assertEquals(ContentType.PDF, MimeCatalog.contentTypeFor("report", "application/pdf"))
        assertEquals(ContentType.OTHER, MimeCatalog.contentTypeFor("report", null))
        assertEquals(ContentType.OTHER, MimeCatalog.contentTypeFor("archive.zzz", "application/x-zzz"))
    }

    @Test
    fun contentTypeFor_handles_dotfiles_and_multi_dot_names() {
        assertEquals(ContentType.OTHER, MimeCatalog.contentTypeFor(".gitignore", null))
        assertEquals(ContentType.PDF, MimeCatalog.contentTypeFor("my.report.v2.pdf", null))
    }

    @Test
    fun extensionFor_matches_the_ContentType_declaration() {
        assertEquals(".png", MimeCatalog.extensionFor(ContentType.IMAGE))
        assertEquals(".epub", MimeCatalog.extensionFor(ContentType.EPUB))
        assertEquals("", MimeCatalog.extensionFor(ContentType.OTHER))
    }
}
