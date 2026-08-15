package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReaderDocumentExpanderTest {

    private val policy = PageLayoutPolicy.standard()
    private var seq = 0.0

    private fun text(id: String, body: String = "a short line") = NoteContentModel.TextContent(
        text = body, updatedAt = null, createdAt = null, id = id, noteId = "n", position = seq++,
    )

    private fun doc(id: String) = NoteContentModel.MediaContent(
        position = seq++, noteId = "n", type = ContentType.PDF, updatedAt = null, createdAt = null,
        id = id, url = "u/$id", title = id,
    )

    private fun image(id: String) = NoteContentModel.MediaContent(
        position = seq++, noteId = "n", type = ContentType.IMAGE, updatedAt = null, createdAt = null,
        id = id, url = "u/$id", title = id,
    )

    private fun link(id: String) = NoteContentModel.Link(
        url = "https://example.com/$id", updatedAt = null, createdAt = null,
        id = id, position = seq++, noteId = "n",
    )

    private fun note(vararg contents: NoteContentModel) = Note(
        id = "n", title = "Field Notes", updatedAt = null, createdAt = null,
        contents = contents.toList(),
    )

    private fun basePages(): List<ReaderPage> = PageLayoutEngine.buildPages(
        note(text("t1"), doc("d1"), image("i1"), text("t2"), doc("d2"), link("l1")), policy,
    )

    private val sources = listOf(
        EmbeddedDocumentSource.pdf("d1", 25),
        EmbeddedDocumentSource.pdf("d2", 4),
    )

    private fun expandAll(base: List<ReaderPage>) = ReaderDocumentExpander.expand(
        base, sources, listOf(DocumentExpansion("d1", 10), DocumentExpansion("d2", 4)), policy,
    )

    private fun refsOf(pages: List<ReaderPage>, id: String) =
        pages.mapNotNull { it.embedded }.filter { it.contentId == id }

    // ---- windowing ----

    @Test
    fun first_window_never_exceeds_ten_and_short_documents_open_whole() {
        assertEquals(10, ReaderDocumentExpander.firstWindow(25))
        assertEquals(4, ReaderDocumentExpander.firstWindow(4))
        assertEquals(0, ReaderDocumentExpander.firstWindow(0))
    }

    @Test
    fun load_more_appends_ten_and_clamps_at_the_last_page() {
        assertEquals(20, ReaderDocumentExpander.nextWindow(10, 25))
        assertEquals(25, ReaderDocumentExpander.nextWindow(20, 25))
        assertEquals(25, ReaderDocumentExpander.nextWindow(25, 25))
    }

    @Test
    fun load_more_is_offered_only_on_the_last_loaded_sheet() {
        val out = ReaderDocumentExpander.expand(
            basePages(), sources, listOf(DocumentExpansion("d1", 10)), policy,
        ).pages
        val refs = refsOf(out, "d1")
        assertEquals(10, refs.size)
        assertEquals((1..10).toList(), refs.map { it.pageNumber })
        assertTrue(refs.last().showsLoadMore)
        assertTrue(refs.dropLast(1).none { it.showsLoadMore })
    }

    @Test
    fun a_fully_loaded_document_never_offers_load_more() {
        val out = ReaderDocumentExpander.expand(
            basePages(), sources, listOf(DocumentExpansion("d2", 4)), policy,
        ).pages
        val refs = refsOf(out, "d2")
        assertEquals(4, refs.size)
        assertTrue(refs.none { it.hasMore })
        assertTrue(refs.none { it.showsLoadMore })
    }

    // ---- sequence and identity ----

    @Test
    fun collapsed_reader_returns_the_engine_list_untouched() {
        val base = basePages()
        assertSame(base, ReaderDocumentExpander.expand(base, sources, emptyList(), policy).pages)
    }

    @Test
    fun every_block_keeps_its_reading_order_across_expansion() {
        val base = basePages()
        val out = expandAll(base).pages
        assertEquals(
            base.flatMap { it.blocks },
            out.filterNot { it.isDocumentSheet }.flatMap { it.blocks },
        )
    }

    @Test
    fun a_page_without_an_open_document_passes_through_by_reference() {
        val base = basePages()
        val out = expandAll(base).pages
        base.filterNot { page -> page.blocks.any { it is ReaderBlock.Document } }
            .forEach { page -> assertTrue(out.any { it === page }) }
    }

    // 📖 the page the card sits on carries the note content that FOLLOWS the document; it has to
    //   land after the sheets, otherwise the reader dead-ends on the last loaded sheet
    @Test
    fun note_content_after_the_card_becomes_the_page_right_after_the_sheets() {
        val base = PageLayoutEngine.buildPages(note(doc("d1"), text("t2")), policy)
        assertEquals(1, base.size)
        val blocks = base[0].blocks
        assertIs<ReaderBlock.Document>(blocks[1])

        val out = ReaderDocumentExpander.expand(
            base, listOf(EmbeddedDocumentSource.pdf("d1", 25)),
            listOf(DocumentExpansion("d1", 10)), policy,
        ).pages

        assertEquals(12, out.size)
        assertEquals(blocks.take(2), out.first().blocks)
        assertTrue(out.subList(1, 11).all { it.isDocumentSheet })
        val tail = out.last()
        assertFalse(tail.isDocumentSheet)
        assertEquals(blocks.drop(2), tail.blocks)
        assertEquals(base[0].index, tail.index)
        assertTrue(tail.continuation > 0)
        assertTrue(out.first().stableKey != tail.stableKey)
    }

    @Test
    fun sheets_follow_their_own_card_and_stay_contiguous() {
        val out = expandAll(basePages()).pages
        out.forEachIndexed { index, page ->
            val ref = page.embedded ?: return@forEachIndexed
            val card = out.take(index).last { !it.isDocumentSheet }
            assertTrue(card.blocks.any { it is ReaderBlock.Document && it.item.id == ref.contentId })
        }
        val order = out.mapNotNull { it.embedded?.contentId }
        assertEquals(List(10) { "d1" } + List(4) { "d2" }, order)
    }

    @Test
    fun collapsing_a_document_restores_the_base_list() {
        val base = basePages()
        val out = ReaderDocumentExpander.expand(
            base, sources, listOf(DocumentExpansion("d1", 0)), policy,
        ).pages
        assertEquals(base, out)
        base.forEachIndexed { index, page -> assertSame(page, out[index]) }
    }

    @Test
    fun an_unsupported_document_is_never_expanded() {
        val base = basePages()
        val out = ReaderDocumentExpander.expand(
            base, listOf(EmbeddedDocumentSource.unsupported("d1")),
            listOf(DocumentExpansion("d1", 10)), policy,
        ).pages
        assertEquals(base, out)
        assertTrue(out.none { it.isDocumentSheet })
    }

    @Test
    fun the_expander_never_repaginates() {
        val base = basePages()
        val out = expandAll(base)
        // a page may be cut in two around an open card, but no page index is added, dropped or reordered
        assertEquals(
            base.map { it.index },
            out.pages.filterNot { it.isDocumentSheet }.map { it.index }.distinct(),
        )
        assertFalse(out.pages.none { it.isDocumentSheet })
    }

    // ---- progress mapping ----

    @Test
    fun reading_progress_never_moves_backwards_and_a_sheet_reports_its_card() {
        val out = expandAll(basePages())
        var previous = -1
        out.pages.forEachIndexed { index, page ->
            val base = out.baseIndexOf(index)
            assertTrue(base >= previous)
            previous = base
            if (page.isDocumentSheet) {
                assertEquals(out.pages.take(index).last { !it.isDocumentSheet }.index, base)
            }
        }
    }

    @Test
    fun display_index_round_trips_for_every_engine_page() {
        val base = basePages()
        val out = expandAll(base)
        base.forEach { page ->
            assertEquals(page.index, out.baseIndexOf(out.displayIndexOf(page.index)))
        }
    }

    @Test
    fun a_text_document_carries_its_platform_paginated_chunk_in_order() {
        val chunks = listOf("one", "two", "three")
        val out = ReaderDocumentExpander.expand(
            basePages(), listOf(EmbeddedDocumentSource.text("d1", chunks)),
            listOf(DocumentExpansion("d1", 3)), policy,
        ).pages
        val sheets = out.mapNotNull { it.blocks.firstOrNull() as? ReaderBlock.DocumentPage }
        assertEquals(chunks, sheets.map { it.text })
        assertEquals(EmbeddedDocumentKind.TEXT, sheets.first().ref.kind)
    }

    @Test
    fun every_displayed_page_has_a_unique_list_key() {
        val out = expandAll(basePages()).pages
        assertEquals(out.size, out.map { it.stableKey }.toSet().size)
    }
}
