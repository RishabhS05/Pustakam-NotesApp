package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.model.models.RichTextMetadata
import com.app.pustakam.core.model.models.TextSpan
import com.app.pustakam.core.model.models.TextStyleType
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.richtext.model.ListMarker
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RichTextPaginationTest {

    private val policy = PageLayoutPolicy.standard()
    private var seq = 0.0

    private fun text(id: String, body: String, metadata: RichTextMetadata? = null) =
        NoteContentModel.TextContent(
            text = body, updatedAt = null, createdAt = null, id = id, noteId = "n",
            position = seq++, metadata = metadata,
        )

    private fun note(vararg contents: NoteContentModel) = Note(
        id = "n", title = "Field Notes", updatedAt = null, createdAt = null,
        contents = contents.toList(),
    )

    private fun bold(end: Int) = RichTextMetadata(
        spans = listOf(TextSpan(start = 0, end = end, style = TextStyleType.BOLD)),
    )

    private val longBody =
        "A paragraph that keeps going and going so the reader has to break it across pages. ".repeat(200)

    private fun richBlock(body: String, list: ListMarker? = null): RichBlock.Text =
        RichDocument.newTextBlock(
            text = body,
            spans = listOf(RichSpan(start = 0, end = body.length, fontWeight = 700)),
            list = list,
        )

    private fun height(block: RichBlock.Text) = BlockHeightEstimator.richHeight(block, policy)

    // ---- the plain path must not move ----

    @Test
    fun a_note_without_formatting_is_built_exactly_as_before() {
        val blocks = ReaderBlockBuilder.buildBlocks(listOf(text("t1", "one"), text("t2", "two")), policy)
        assertTrue(blocks.none { it is ReaderBlock.RichParagraph })
        assertEquals("one\n\ntwo", blocks.filterIsInstance<ReaderBlock.Paragraph>().single().text)
    }

    // ---- formatting reaches the reader ----

    @Test
    fun a_formatted_content_becomes_one_block_per_paragraph() {
        val blocks = ReaderBlockBuilder.buildBlocks(listOf(text("t1", "first\nsecond", bold(5))), policy)
        val rich = blocks.filterIsInstance<ReaderBlock.RichParagraph>()
        assertEquals(listOf("first", "second"), rich.map { it.text })
        assertTrue(blocks.none { it is ReaderBlock.Paragraph })
        assertEquals(listOf("t1"), rich.first().sourceContentIds)
    }

    // ---- long text is cut, never clipped ----

    @Test
    fun a_long_paragraph_is_cut_until_every_chunk_fits_one_page() {
        val chunks = RichTextSplitter.split(richBlock(longBody), policy)
        assertTrue(chunks.size > 1, "expected the body to need several pages")
        chunks.forEach { chunk ->
            assertTrue(
                height(chunk) <= policy.usableHeight,
                "chunk of ${chunk.text.length} chars measured ${height(chunk)}",
            )
        }
    }

    @Test
    fun the_cut_keeps_every_character() {
        val block = richBlock(longBody)
        assertEquals(block.text, RichTextSplitter.split(block, policy).joinToString("") { it.text })
    }

    @Test
    fun a_cut_lands_after_a_word_boundary() {
        val chunks = RichTextSplitter.split(richBlock(longBody), policy)
        chunks.dropLast(1).forEach { chunk ->
            val last = chunk.text.last()
            assertTrue(last == ' ' || last == '\n', "chunk ended mid-word on '$last'")
        }
    }

    @Test
    fun spans_survive_the_cut_with_local_offsets() {
        val chunks = RichTextSplitter.split(richBlock(longBody), policy)
        chunks.forEach { chunk ->
            val span = chunk.spans.single()
            assertEquals(0, span.start)
            assertEquals(chunk.text.length, span.end)
            assertEquals(700, span.fontWeight)
        }
    }

    @Test
    fun a_list_marker_never_repeats_on_a_continuation_page() {
        val chunks = RichTextSplitter.split(richBlock(longBody, ListMarker(style = ListStyle.BULLET)), policy)
        assertTrue(chunks.size > 1)
        assertNotNull(chunks.first().list)
        chunks.drop(1).forEach { assertNull(it.list) }
    }

    @Test
    fun a_short_paragraph_is_never_cut() {
        val block = richBlock("one short line")
        val chunks = RichTextSplitter.split(block, policy)
        assertEquals(1, chunks.size)
        assertEquals(block, chunks.single())
    }

    // ---- end to end ----

    @Test
    fun no_page_overflows_once_a_formatted_note_is_paginated() {
        val pages = PageLayoutEngine.buildPages(note(text("t1", longBody, bold(40))), policy)
        assertTrue(pages.size > 1, "a long formatted note must span pages")
        pages.forEach { page ->
            val gaps = policy.blockGap * (page.blocks.size - 1).coerceAtLeast(0)
            val used = page.blocks.sumOf { BlockHeightEstimator.estimate(it, policy).toDouble() } + gaps
            assertTrue(used <= policy.usableHeight + 0.5, "page ${page.index} measured $used")
        }
    }

    @Test
    fun a_formatted_note_keeps_its_reading_order() {
        val pages = PageLayoutEngine.buildPages(
            note(text("t1", "alpha\nbeta", bold(5)), text("t2", "gamma", bold(5))), policy,
        )
        val texts = pages.flatMap { it.blocks }
            .filterIsInstance<ReaderBlock.RichParagraph>()
            .map { it.text }
        assertEquals(listOf("alpha", "beta", "gamma"), texts)
    }
}
