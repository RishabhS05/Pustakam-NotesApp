package com.app.pustakam.core.filesys.path

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.export.ExportFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 1 — pins the on-disk layout. Every expectation below describes files
//   that ALREADY EXIST on user devices. A failure here means an upgrade would orphan them.
class PathPolicyTest {

    private val ts = 1_753_832_400_000L
    private val noteId = "note-42"

    // ---- capture ----

    @Test
    fun capture_folder_is_lowercase_type_then_note_id() {
        val d = PathPolicy.capturePath(ContentType.IMAGE, noteId, ts)
        assertEquals("image/note-42", d.folder)
        assertEquals("$ts.png", d.fileName)
        assertEquals("image/note-42/$ts.png", d.relativePath)
    }

    @Test
    fun capture_folder_is_lowercase_for_every_media_type() {
        listOf(
            ContentType.IMAGE to "image", ContentType.GIF to "gif", ContentType.VIDEO to "video",
            ContentType.AUDIO to "audio", ContentType.PDF to "pdf", ContentType.DOCX to "docx",
            ContentType.TXT to "txt", ContentType.MD to "md", ContentType.EPUB to "epub",
            ContentType.OTHER to "other",
        ).forEach { (type, expected) ->
            assertEquals(expected, PathPolicy.folderForContentType(type), "folder for $type")
        }
    }

    @Test
    fun capture_folder_never_contains_an_uppercase_letter() {
        // Guards against re-introducing the iOS UPPERCASE variant (FIA doc §2.3b).
        ContentType.entries.forEach {
            val f = PathPolicy.folderForContentType(it)
            assertEquals(f.lowercase(), f, "folder for $it must be lowercase")
        }
    }

    // ---- import ----

    @Test
    fun import_folder_is_imported_then_note_id() {
        val d = PathPolicy.importPath(noteId, "Report.pdf")
        assertEquals("imported/note-42", d.folder)
        assertEquals("Report.pdf", d.fileName)
        assertEquals("imported/note-42/Report.pdf", d.relativePath)
    }

    @Test
    fun import_name_is_sanitized() {
        assertEquals("a_b.pdf", PathPolicy.importPath(noteId, "a/b.pdf").fileName)
    }

    // ---- thumbnail ----

    @Test
    fun thumbnail_folder_is_flat() {
        val d = PathPolicy.thumbnailPath("/data/x/photo.png")
        assertEquals("thumbnails", d.folder)
        assertEquals("photo_thumb.jpg", d.fileName)
        assertEquals("thumbnails/photo_thumb.jpg", d.relativePath)
    }

    // ---- export ----

    @Test
    fun export_folder_is_flat_and_name_carries_the_format_extension() {
        val d = PathPolicy.exportPath("My Note", ExportFormat.PDF, ts)
        assertEquals("exports", d.folder)
        assertEquals("My_Note-$ts.pdf", d.fileName)
    }

    @Test
    fun export_uses_the_right_extension_per_format() {
        assertTrue(PathPolicy.exportPath("n", ExportFormat.PDF, ts).fileName.endsWith(".pdf"))
        assertTrue(PathPolicy.exportPath("n", ExportFormat.IMAGE, ts).fileName.endsWith(".png"))
        assertTrue(PathPolicy.exportPath("n", ExportFormat.DOCX, ts).fileName.endsWith(".docx"))
    }

    // ---- roots are frozen ----

    @Test
    fun roots_match_what_is_already_on_disk() {
        assertEquals("imported", PathPolicy.IMPORT_ROOT)
        assertEquals("exports", PathPolicy.EXPORT_ROOT)
        assertEquals("thumbnails", PathPolicy.THUMBNAIL_ROOT)
    }

    // ---- relativePath / isManaged ----

    @Test
    fun relativePath_joins_with_a_single_separator() {
        assertEquals("a/b.pdf", PathPolicy.relativePath("a", "b.pdf"))
        assertEquals("b.pdf", PathPolicy.relativePath("", "b.pdf"))
    }

    @Test
    fun isManaged_accepts_our_roots_and_capture_folders() {
        assertTrue(PathPolicy.isManaged("imported/n/x.pdf"))
        assertTrue(PathPolicy.isManaged("exports/x.pdf"))
        assertTrue(PathPolicy.isManaged("thumbnails/x_thumb.jpg"))
        assertTrue(PathPolicy.isManaged("image/n/1.png"))
        assertTrue(PathPolicy.isManaged("/audio/n/1.mp3"))
    }

    @Test
    fun isManaged_rejects_anything_else() {
        assertFalse(PathPolicy.isManaged("../../etc/passwd"))
        assertFalse(PathPolicy.isManaged("Downloads/x.pdf"))
        assertFalse(PathPolicy.isManaged("IMAGE/n/1.png")) // uppercase is NOT ours
    }
}
