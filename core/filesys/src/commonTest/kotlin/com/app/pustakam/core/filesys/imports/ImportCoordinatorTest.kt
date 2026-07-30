package com.app.pustakam.core.filesys.imports

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.model.ImportDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 2 — pins import planning. The coordinator must never touch a file.
class ImportCoordinatorTest {

    private val ts = 1_753_832_400_000L
    private val noteId = "note-42"

    private fun accept(d: ImportDecision) = assertIs<ImportDecision.Accepted>(d).plan

    // ---- happy path ----

    @Test
    fun a_plain_file_lands_under_imported_note_id() {
        val plan = accept(
            ImportCoordinator.planImport(noteId, "Report.pdf", "application/pdf", 1024, timestamp = ts),
        )
        assertEquals("imported/note-42/Report.pdf", plan.relativePath)
        assertEquals(ContentType.PDF, plan.contentType)
        assertEquals("application/pdf", plan.mimeType)
        assertEquals("Report.pdf", plan.displayName)
        assertEquals(1024, plan.sizeBytes)
    }

    @Test
    fun display_name_keeps_the_original_even_when_the_file_is_uniqued() {
        val plan = accept(
            ImportCoordinator.planImport(
                noteId, "a.pdf", "application/pdf", 10, timestamp = ts,
                exists = { it == "imported/note-42/a.pdf" },
            ),
        )
        assertEquals("imported/note-42/${ts}_a.pdf", plan.relativePath)
        assertEquals("a.pdf", plan.displayName, "the media title must stay human-readable")
    }

    @Test
    fun mime_is_resolved_from_the_extension_when_the_server_says_nothing() {
        val plan = accept(ImportCoordinator.planImport(noteId, "notes.md", null, 10, timestamp = ts))
        assertEquals(ContentType.MD, plan.contentType)
        assertEquals("text/markdown", plan.mimeType)
    }

    @Test
    fun a_mislabelled_server_mime_does_not_beat_the_extension() {
        val plan = accept(
            ImportCoordinator.planImport(noteId, "book.epub", "application/octet-stream", 10, timestamp = ts),
        )
        assertEquals(ContentType.EPUB, plan.contentType)
    }

    @Test
    fun an_unknown_type_still_imports_as_OTHER() {
        val plan = accept(ImportCoordinator.planImport(noteId, "thing.zzz", null, 10, timestamp = ts))
        assertEquals(ContentType.OTHER, plan.contentType)
    }

    @Test
    fun the_source_url_is_carried_for_link_imports_and_empty_otherwise() {
        val link = accept(
            ImportCoordinator.planImport(
                noteId, "a.pdf", "application/pdf", 10, "https://x.com/a.pdf", ts,
            ),
        )
        assertEquals("https://x.com/a.pdf", link.sourceUrl)
        val pick = accept(ImportCoordinator.planImport(noteId, "a.pdf", "application/pdf", 10, timestamp = ts))
        assertEquals("", pick.sourceUrl)
    }

    // ---- rejection ----

    @Test
    fun a_blank_name_is_rejected() {
        assertIs<ImportDecision.Rejected>(ImportCoordinator.planImport(noteId, "", timestamp = ts))
        assertIs<ImportDecision.Rejected>(ImportCoordinator.planImport(noteId, "   ", timestamp = ts))
    }

    @Test
    fun an_empty_payload_is_rejected() {
        assertIs<ImportDecision.Rejected>(
            ImportCoordinator.planImport(noteId, "a.pdf", "application/pdf", 0, timestamp = ts),
        )
        assertIs<ImportDecision.Rejected>(
            ImportCoordinator.planImport(noteId, "a.pdf", "application/pdf", -5, timestamp = ts),
        )
    }

    @Test
    fun a_web_page_is_not_a_file() {
        assertIs<ImportDecision.NotAFile>(
            ImportCoordinator.planImport(noteId, "index.html", "text/html", 4096, timestamp = ts),
        )
        assertIs<ImportDecision.NotAFile>(
            ImportCoordinator.planImport(noteId, "download", "application/octet-stream", 10, timestamp = ts),
        )
    }

    // ---- duplicate resolution ----

    @Test
    fun a_free_name_is_used_as_is() {
        val plan = accept(ImportCoordinator.planImport(noteId, "a.pdf", null, 10, timestamp = ts))
        assertEquals("imported/note-42/a.pdf", plan.relativePath)
    }

    @Test
    fun a_second_collision_falls_through_to_a_counter() {
        val taken = setOf("imported/note-42/a.pdf", "imported/note-42/${ts}_a.pdf")
        val plan = accept(
            ImportCoordinator.planImport(noteId, "a.pdf", null, 10, timestamp = ts, exists = { it in taken }),
        )
        assertEquals("imported/note-42/${ts}_1_a.pdf", plan.relativePath)
    }

    @Test
    fun the_name_is_sanitized_before_the_collision_probe() {
        val plan = accept(ImportCoordinator.planImport(noteId, "a/b.pdf", null, 10, timestamp = ts))
        assertEquals("imported/note-42/a_b.pdf", plan.relativePath)
    }

    // ---- Swift-facing overload ----

    @Test
    fun the_platform_overload_matches_the_lambda_form_exactly() {
        val taken = listOf("imported/note-42/a.pdf")
        val viaList = accept(
            ImportCoordinator.planImportForPlatform(
                noteId, "a.pdf", "application/pdf", 10, "", ts, taken,
            ),
        )
        val viaLambda = accept(
            ImportCoordinator.planImport(
                noteId, "a.pdf", "application/pdf", 10, "", ts, exists = { it in taken },
            ),
        )
        assertEquals(viaLambda.relativePath, viaList.relativePath)
        assertEquals(viaLambda.contentType, viaList.contentType)
        assertEquals(viaLambda.mimeType, viaList.mimeType)
    }

    @Test
    fun the_platform_overload_rejects_and_skips_like_the_lambda_form() {
        assertIs<ImportDecision.NotAFile>(
            ImportCoordinator.planImportForPlatform(noteId, "p.html", "text/html", 10, "", ts, emptyList()),
        )
        assertIs<ImportDecision.Rejected>(
            ImportCoordinator.planImportForPlatform(noteId, "", null, 10, "", ts, emptyList()),
        )
    }

    // ---- batch planning ----

    @Test
    fun a_batch_of_distinct_names_all_get_planned() {
        val batch = ImportCoordinator.planImports(
            noteId,
            listOf(ImportRequest("a.pdf"), ImportRequest("b.png"), ImportRequest("c.md")),
            ts,
        )
        assertEquals(3, batch.accepted.size)
        assertTrue(batch.skipped.isEmpty())
        assertTrue(batch.hasWork)
    }

    @Test
    fun two_files_with_the_same_name_in_one_batch_cannot_collide() {
        // Nothing is on disk yet, so only in-batch reservation can prevent this.
        val batch = ImportCoordinator.planImports(
            noteId, listOf(ImportRequest("a.pdf"), ImportRequest("a.pdf")), ts,
        )
        assertEquals(2, batch.accepted.size)
        assertEquals(
            batch.accepted.map { it.relativePath }.toSet().size,
            batch.accepted.size,
            "two plans resolved to the same path",
        )
    }

    @Test
    fun a_batch_keeps_the_good_files_and_reports_the_bad_ones() {
        val batch = ImportCoordinator.planImports(
            noteId,
            listOf(
                ImportRequest("good.pdf"),
                ImportRequest("page.html", "text/html"),
                ImportRequest("", null),
            ),
            ts,
        )
        assertEquals(1, batch.accepted.size)
        assertEquals(2, batch.skipped.size)
        assertIs<ImportDecision.NotAFile>(batch.skipped[0])
        assertIs<ImportDecision.Rejected>(batch.skipped[1])
    }

    @Test
    fun an_empty_batch_reports_no_work() {
        val batch = ImportCoordinator.planImports(noteId, emptyList(), ts)
        assertTrue(batch.accepted.isEmpty())
        assertTrue(!batch.hasWork)
    }

    @Test
    fun batch_planning_also_respects_files_already_on_disk() {
        val onDisk = setOf("imported/note-42/a.pdf")
        val batch = ImportCoordinator.planImports(
            noteId, listOf(ImportRequest("a.pdf")), ts, exists = { it in onDisk },
        )
        assertEquals("imported/note-42/${ts}_a.pdf", batch.accepted.single().relativePath)
    }
}
