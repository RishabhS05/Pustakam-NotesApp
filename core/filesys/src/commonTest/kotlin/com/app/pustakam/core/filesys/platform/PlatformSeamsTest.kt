package com.app.pustakam.core.filesys.platform

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.model.ThumbnailPolicy
import com.app.pustakam.core.filesys.model.ThumbnailResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 3 — exercises each seam through the fake, which proves the interfaces
//   are implementable as specified and locks in the contract the platform bindings must honour.
class PlatformSeamsTest {

    private fun bytes(s: String) = s.encodeToByteArray()

    // ---- FileReader ----

    @Test
    fun reading_a_missing_file_returns_null_rather_than_throwing() {
        val fs = FakeFileSystem()
        assertNull(fs.read("nope.txt"))
        assertNull(fs.readText("nope.txt"))
    }

    @Test
    fun readText_truncates_at_the_byte_cap() {
        val fs = FakeFileSystem(mapOf("a.txt" to bytes("0123456789")))
        assertEquals("0123456789", fs.readText("a.txt"))
        assertEquals("01234", fs.readText("a.txt", maxBytes = 5))
    }

    @Test
    fun readText_ignores_a_cap_larger_than_the_file() {
        val fs = FakeFileSystem(mapOf("a.txt" to bytes("abc")))
        assertEquals("abc", fs.readText("a.txt", maxBytes = 1000))
    }

    // ---- FileWriter + DirectoryManager ----

    @Test
    fun writing_creates_the_file_and_registers_its_folder() {
        val fs = FakeFileSystem()
        assertTrue(fs.write("imported/n/a.pdf", bytes("hi")))
        assertTrue(fs.exists("imported/n/a.pdf"))
        assertEquals(listOf("a.pdf"), fs.list("imported/n"))
        assertEquals(2L, fs.sizeOf("imported/n/a.pdf"))
    }

    @Test
    fun listing_an_unknown_folder_is_empty_not_an_error() {
        assertTrue(FakeFileSystem().list("nowhere").isEmpty())
    }

    @Test
    fun sizeOf_an_absent_file_is_zero() {
        assertEquals(0L, FakeFileSystem().sizeOf("nope"))
    }

    @Test
    fun list_only_returns_direct_children() {
        val fs = FakeFileSystem()
        fs.write("a/one.txt", bytes("1"))
        fs.write("a/b/two.txt", bytes("2"))
        assertEquals(listOf("one.txt"), fs.list("a"))
    }

    // ---- FileCopier ----

    @Test
    fun copying_an_unknown_handle_fails_cleanly() {
        assertFalse(FakeFileSystem().copyIn("content://nope", "imported/n/a.pdf"))
    }

    @Test
    fun copying_places_the_bytes_at_the_destination() {
        val fs = FakeFileSystem()
        fs.external["content://pick/a.pdf"] = bytes("payload")
        assertTrue(fs.copyIn("content://pick/a.pdf", "imported/n/a.pdf"))
        assertEquals("payload", fs.readText("imported/n/a.pdf"))
    }

    // ---- FileDeleter ----

    @Test
    fun deleting_something_already_gone_still_succeeds() {
        // Both platforms treat this as success; callers rely on it when removing a note block.
        assertTrue(FakeFileSystem().delete("nope"))
    }

    @Test
    fun deleting_removes_the_file() {
        val fs = FakeFileSystem(mapOf("a.txt" to bytes("x")))
        assertTrue(fs.delete("a.txt"))
        assertFalse(fs.exists("a.txt"))
    }

    // ---- MetadataReader ----

    @Test
    fun describe_resolves_name_extension_type_and_size() {
        val fs = FakeFileSystem(mapOf("imported/n/Report.pdf" to bytes("1234")))
        val meta = assertNotNull(fs.describe("imported/n/Report.pdf"))
        assertEquals("Report.pdf", meta.fileName)
        assertEquals("pdf", meta.extension)
        assertEquals(ContentType.PDF, meta.contentType)
        assertEquals("application/pdf", meta.mimeType)
        assertEquals(4L, meta.sizeBytes)
    }

    @Test
    fun describing_a_missing_file_or_handle_returns_null() {
        val fs = FakeFileSystem()
        assertNull(fs.describe("nope.pdf"))
        assertNull(fs.describeHandle("content://nope"))
    }

    @Test
    fun one_class_can_implement_every_file_io_seam_at_once() {
        // Regression guard: FileReader.read and MetadataReader.read once had identical signatures,
        // which made this impossible. Compiling this test IS the assertion.
        val fs: Any = FakeFileSystem()
        assertTrue(fs is FileReader && fs is FileWriter && fs is DirectoryManager)
        assertTrue(fs is FileCopier && fs is FileDeleter && fs is MetadataReader)
    }

    // ---- ThumbnailGenerator + ThumbnailPolicy ----

    @Test
    fun thumbnail_policy_constants_match_both_platforms() {
        assertEquals(512, ThumbnailPolicy.MAX_DIMENSION_PX)
        assertEquals(70, ThumbnailPolicy.JPEG_QUALITY)
        assertEquals(1_000_000L, ThumbnailPolicy.VIDEO_FRAME_MICROS)
    }

    // 🔧 30-Jul-2026 Phase 4 — iOS reads these through the accessors, not the consts.
    @Test
    fun swift_facing_thumbnail_accessors_match_the_constants() {
        assertEquals(ThumbnailPolicy.MAX_DIMENSION_PX, ThumbnailPolicy.maxDimensionPx())
        assertEquals(ThumbnailPolicy.JPEG_QUALITY, ThumbnailPolicy.jpegQuality())
        assertEquals(ThumbnailPolicy.VIDEO_FRAME_MICROS, ThumbnailPolicy.videoFrameMicros())
    }

    @Test
    fun only_image_gif_and_video_get_thumbnails() {
        assertTrue(ThumbnailPolicy.isEligible(ContentType.IMAGE))
        assertTrue(ThumbnailPolicy.isEligible(ContentType.GIF))
        assertTrue(ThumbnailPolicy.isEligible(ContentType.VIDEO))
        listOf(ContentType.PDF, ContentType.AUDIO, ContentType.TXT, ContentType.OTHER)
            .forEach { assertFalse(ThumbnailPolicy.isEligible(it), "$it") }
    }

    @Test
    fun requestFor_targets_the_thumbnails_folder_and_carries_the_policy() {
        val request = assertNotNull(
            ThumbnailPolicy.requestFor("image/n/1753832400000.png", ContentType.IMAGE),
        )
        assertEquals("thumbnails/1753832400000_thumb.jpg", request.destinationRelativePath)
        assertEquals(512, request.maxDimensionPx)
        assertEquals(70, request.quality)
    }

    @Test
    fun requestFor_returns_null_for_a_type_with_no_thumbnail() {
        assertNull(ThumbnailPolicy.requestFor("pdf/n/a.pdf", ContentType.PDF))
    }

    @Test
    fun an_ineligible_type_is_reported_as_NotSupported_not_a_failure() {
        val generator = FakeThumbnailGenerator()
        val request = ThumbnailPolicy
            .requestFor("image/n/a.png", ContentType.IMAGE)!!
            .copy(contentType = ContentType.PDF)
        assertIs<ThumbnailResult.NotSupported>(generator.generate(request))
    }

    // ---- sample size / scale maths (the three duplicated Android copies) ----

    @Test
    fun sample_size_is_the_largest_power_of_two_that_still_covers_the_target() {
        assertEquals(1, ThumbnailPolicy.sampleSizeFor(512, 512))
        assertEquals(1, ThumbnailPolicy.sampleSizeFor(1000, 800))
        assertEquals(2, ThumbnailPolicy.sampleSizeFor(1024, 1024))
        assertEquals(4, ThumbnailPolicy.sampleSizeFor(4096, 3000))
    }

    @Test
    fun sample_size_is_safe_for_degenerate_bounds() {
        assertEquals(1, ThumbnailPolicy.sampleSizeFor(0, 0))
        assertEquals(1, ThumbnailPolicy.sampleSizeFor(-5, 100))
        assertEquals(1, ThumbnailPolicy.sampleSizeFor(100, 100, maxDimension = 0))
    }

    @Test
    fun scale_factor_never_upscales() {
        assertEquals(1f, ThumbnailPolicy.scaleFactorFor(100, 100))
        assertEquals(1f, ThumbnailPolicy.scaleFactorFor(512, 400))
        assertEquals(0.5f, ThumbnailPolicy.scaleFactorFor(1024, 400))
    }

    // ---- DocumentRenderer ----

    @Test
    fun page_count_is_zero_for_an_unknown_document() {
        assertEquals(0, FakeDocumentRenderer().pageCount("nope.pdf"))
        assertEquals(12, FakeDocumentRenderer(mapOf("a.pdf" to 12)).pageCount("a.pdf"))
    }

    // ---- TextMeasurer ----

    @Test
    fun text_styles_carry_the_sizes_the_exporter_draws_at() {
        assertEquals(44f, TextStyle.TITLE.sizePx)
        assertEquals(30f, TextStyle.BODY.sizePx)
        assertEquals(24f, TextStyle.CAPTION.sizePx)
        assertEquals(28f, TextStyle.ACCENT.sizePx)
        assertTrue(TextStyle.TITLE.bold)
        assertFalse(TextStyle.BODY.bold)
    }

    @Test
    fun measuring_empty_text_or_zero_width_is_zero() {
        val measurer = FakeTextMeasurer()
        assertEquals(0f, measurer.measureHeight("", TextStyle.BODY, 500f))
        assertEquals(0f, measurer.measureHeight("abc", TextStyle.BODY, 0f))
    }

    @Test
    fun measured_height_grows_with_text_length() {
        val measurer = FakeTextMeasurer()
        val short = measurer.measureHeight("a".repeat(10), TextStyle.BODY, 500f)
        val long = measurer.measureHeight("a".repeat(500), TextStyle.BODY, 500f)
        assertTrue(long > short)
    }
}
