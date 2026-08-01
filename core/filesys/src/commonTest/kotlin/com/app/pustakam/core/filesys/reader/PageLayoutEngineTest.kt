package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PageLayoutEngineTest {

    private val policy = PageLayoutPolicy.standard()
    private var seq = 0.0

    private fun text(id: String, body: String = "a short line") = NoteContentModel.TextContent(
        text = body, updatedAt = null, createdAt = null, id = id, noteId = "n", position = seq++,
    )

    private fun media(id: String, type: ContentType) = NoteContentModel.MediaContent(
        position = seq++, noteId = "n", type = type, updatedAt = null, createdAt = null,
        id = id, url = "u/$id", title = id,
    )

    private fun link(id: String) = NoteContentModel.Link(
        url = "https://example.com/$id", updatedAt = null, createdAt = null,
        id = id, position = seq++, noteId = "n",
    )

    private fun location(id: String) = NoteContentModel.Location(
        latitude = 19.0, longitude = 72.0, address = "Mumbai", updatedAt = null, createdAt = null,
        id = id, noteId = "n", position = seq++,
    )

    private fun note(vararg contents: NoteContentModel) = Note(
        id = "n", title = "Field Notes", updatedAt = null, createdAt = null,
        contents = contents.toList(),
    )

    // ---- §2 reading order ----

    @Test
    fun reading_order_is_never_changed() {
        val contents = listOf(
            text("t1"), media("i1", ContentType.IMAGE), media("a1", ContentType.AUDIO),
            text("t2"), media("v1", ContentType.VIDEO), link("l1"), location("loc1"),
        )
        val flattened = ReaderBlockBuilder.buildBlocks(contents, policy).flatMap { it.sourceContentIds }
        assertEquals(listOf("t1", "i1", "a1", "t2", "v1", "l1", "loc1"), flattened)
    }

    @Test
    fun reading_order_survives_pagination() {
        val contents = (1..40).map { i ->
            when (i % 4) {
                0 -> media("m$i", ContentType.IMAGE)
                1 -> text("t$i", "paragraph number $i ".repeat(20))
                2 -> media("a$i", ContentType.AUDIO)
                else -> media("v$i", ContentType.VIDEO)
            }
        }
        val expected = contents.map { it.id }
        val actual = PageLayoutEngine
            .paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy)
            .flatMap { it.sourceContentIds }
        assertEquals(expected, actual)
    }

    // ---- §3 consecutive-only grouping ----

    @Test
    fun consecutive_images_become_one_grid() {
        val blocks = ReaderBlockBuilder.buildBlocks(
            listOf(
                media("i1", ContentType.IMAGE),
                media("i2", ContentType.IMAGE),
                media("i3", ContentType.IMAGE),
            ),
            policy,
        )
        assertEquals(1, blocks.size)
        assertEquals(3, assertIs<ReaderBlock.ImageGrid>(blocks[0]).items.size)
    }

    @Test
    fun a_content_of_another_type_breaks_the_run() {
        // Image Image Text Image -> ImageGrid(2), Paragraph, ImageGrid(1)
        val blocks = ReaderBlockBuilder.buildBlocks(
            listOf(
                media("i1", ContentType.IMAGE), media("i2", ContentType.IMAGE),
                text("t1"), media("i3", ContentType.IMAGE),
            ),
            policy,
        )
        assertEquals(3, blocks.size)
        assertEquals(2, assertIs<ReaderBlock.ImageGrid>(blocks[0]).items.size)
        assertIs<ReaderBlock.Paragraph>(blocks[1])
        assertEquals(1, assertIs<ReaderBlock.ImageGrid>(blocks[2]).items.size)
    }

    @Test
    fun videos_and_images_never_share_a_grid() {
        val blocks = ReaderBlockBuilder.buildBlocks(
            listOf(media("i1", ContentType.IMAGE), media("v1", ContentType.VIDEO)),
            policy,
        )
        assertEquals(2, blocks.size)
        assertIs<ReaderBlock.ImageGrid>(blocks[0])
        assertIs<ReaderBlock.VideoGrid>(blocks[1])
    }

    @Test
    fun audio_is_never_grouped() {
        val blocks = ReaderBlockBuilder.buildBlocks(
            listOf(
                media("a1", ContentType.AUDIO),
                media("a2", ContentType.AUDIO),
                media("a3", ContentType.AUDIO),
            ),
            policy,
        )
        assertEquals(3, blocks.size)
        blocks.forEach { assertIs<ReaderBlock.Audio>(it) }
    }

    @Test
    fun consecutive_paragraphs_merge_into_one_block() {
        val blocks = ReaderBlockBuilder.buildBlocks(
            listOf(text("t1", "first"), text("t2", "second")),
            policy,
        )
        assertEquals(1, blocks.size)
        val paragraph = assertIs<ReaderBlock.Paragraph>(blocks[0])
        assertEquals("first\n\nsecond", paragraph.text)
        assertEquals(listOf("t1", "t2"), paragraph.sourceContentIds)
    }

    // ---- §7 pagination ----

    @Test
    fun a_page_holds_several_widgets_instead_of_one() {
        // the exact case from the brief: 3 audio + text + image grid + video must share a page
        val contents = listOf(
            media("a1", ContentType.AUDIO), media("a2", ContentType.AUDIO),
            media("a3", ContentType.AUDIO), text("t1", "two short lines of copy"),
            media("i1", ContentType.IMAGE), media("i2", ContentType.IMAGE),
            media("v1", ContentType.VIDEO),
        )
        val pages = PageLayoutEngine.paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy)
        assertEquals(1, pages.size, "everything fits one A4 page")
        assertEquals(6, pages[0].blocks.size)
    }

    @Test
    fun a_block_is_never_split_across_pages() {
        val contents = (1..30).map { media("a$it", ContentType.AUDIO) }
        val pages = PageLayoutEngine.paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy)
        // every audio block appears exactly once, on exactly one page
        val ids = pages.flatMap { it.sourceContentIds }
        assertEquals(30, ids.size)
        assertEquals(30, ids.toSet().size)
    }

    @Test
    fun no_page_exceeds_the_usable_height_unless_one_block_alone_does() {
        val contents = (1..25).map { i ->
            if (i % 3 == 0) text("t$i", "body ".repeat(40)) else media("i$i", ContentType.IMAGE)
        }
        val pages = PageLayoutEngine.paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy)
        pages.forEach { page ->
            if (page.blocks.size > 1) {
                assertTrue(
                    page.usedHeight <= policy.usableHeight,
                    "page ${page.index} used ${page.usedHeight} > ${policy.usableHeight}",
                )
            }
        }
    }

    @Test
    fun pages_are_indexed_in_order() {
        val contents = (1..40).map { media("a$it", ContentType.AUDIO) }
        val pages = PageLayoutEngine.paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy)
        pages.forEachIndexed { index, page -> assertEquals(index, page.index) }
    }

    // ---- text splitting ----

    @Test
    fun a_paragraph_taller_than_a_page_is_split_at_a_word_boundary() {
        val long = "word ".repeat(policy.maxCharsPerParagraph)
        val blocks = ReaderBlockBuilder.buildBlocks(listOf(text("t1", long)), policy)
        assertTrue(blocks.size > 1)
        blocks.forEach {
            val paragraph = assertIs<ReaderBlock.Paragraph>(it)
            assertTrue(paragraph.text.length <= policy.maxCharsPerParagraph)
            assertEquals(blocks.size, paragraph.chunkCount)
        }
        // no characters invented or lost beyond the whitespace at each cut
        val rejoined = blocks.joinToString(" ") { (it as ReaderBlock.Paragraph).text.trim() }
        assertEquals(long.trim().replace(Regex("\\s+"), " "), rejoined.replace(Regex("\\s+"), " "))
    }

    // ---- §10 one source of truth ----

    @Test
    fun both_reading_modes_receive_the_identical_page_list() {
        val n = note(text("t1"), media("i1", ContentType.IMAGE), media("a1", ContentType.AUDIO))
        val first = PageLayoutEngine.buildPages(n, policy)
        val second = PageLayoutEngine.buildPages(n, policy)
        // page mode and scroll mode both read this list; generating twice must be identical
        assertEquals(first, second)
    }

    @Test
    fun a_note_starts_with_its_title_block() {
        val pages = PageLayoutEngine.buildPages(note(text("t1")), policy)
        assertIs<ReaderBlock.Title>(pages.first().blocks.first())
    }

    @Test
    fun jump_target_resolves_to_the_page_holding_the_content() {
        val contents = (1..30).map { media("a$it", ContentType.AUDIO) }
        val pages = PageLayoutEngine.paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy)
        val index = PageLayoutEngine.pageIndexOf(pages, "a25")
        assertTrue(index >= 0)
        assertTrue(pages[index].contains("a25"))
    }

    // ---- grid rules ----

    @Test
    fun a_grid_shows_at_most_four_cells_and_reports_the_overflow() {
        assertEquals(4, BlockHeightEstimator.visibleCells(9, policy))
        assertEquals(5, BlockHeightEstimator.overflowCount(9, policy))
        assertEquals(0, BlockHeightEstimator.overflowCount(3, policy))
    }

    @Test
    fun the_grid_is_two_columns() {
        assertEquals(2, policy.gridColumns)
    }

    // ---- Stage 2: measured size, not just height ----

    @Test
    fun a_measured_block_reports_width_and_height() {
        val block = ReaderBlock.Audio(media("a1", ContentType.AUDIO), listOf("a1"))
        val measured = BlockHeightEstimator.measure(block, policy)
        assertEquals(policy.usableWidth, measured.width)
        assertEquals(policy.audioHeight, measured.height)
    }

    @Test
    fun only_text_is_breakable() {
        val paragraph = ReaderBlock.Paragraph("body", 1, 1, listOf("t1"))
        val audio = ReaderBlock.Audio(media("a1", ContentType.AUDIO), listOf("a1"))
        assertTrue(BlockHeightEstimator.measure(paragraph, policy).breakable)
        assertFalse(BlockHeightEstimator.measure(audio, policy).breakable)
    }

    // ---- Stage 3: the budget container ----

    @Test
    fun an_empty_budget_offers_the_whole_page_and_charges_no_gap() {
        val budget = PageBudget.of(policy)
        assertTrue(budget.isEmpty)
        assertEquals(0f, budget.nextGap)
        assertEquals(policy.usableHeight, budget.remainingHeight)
    }

    @Test
    fun placing_a_block_reduces_the_room_left_for_the_next_one() {
        val audio = BlockHeightEstimator.measure(
            ReaderBlock.Audio(media("a1", ContentType.AUDIO), listOf("a1")), policy,
        )
        val after = PageBudget.of(policy).place(audio)
        assertEquals(policy.audioHeight, after.occupied.height)
        assertEquals(policy.usableWidth, after.occupied.width)
        // the SECOND block pays a gap; the first did not
        assertEquals(policy.blockGap, after.nextGap)
        assertEquals(policy.usableHeight - policy.audioHeight, after.remainingHeight)
    }

    @Test
    fun a_block_that_would_overflow_does_not_fit() {
        var budget = PageBudget.of(policy)
        val audio = BlockHeightEstimator.measure(
            ReaderBlock.Audio(media("a1", ContentType.AUDIO), listOf("a1")), policy,
        )
        var placed = 0
        while (budget.fits(audio)) { budget = budget.place(audio); placed++ }
        assertTrue(placed > 0)
        // the page is now genuinely full — one more would exceed the usable height
        assertTrue(budget.occupied.height + budget.nextGap + audio.height > policy.usableHeight)
    }

    @Test
    fun a_pages_occupied_size_matches_what_the_budget_accumulated() {
        val contents = listOf(
            media("a1", ContentType.AUDIO), media("a2", ContentType.AUDIO), text("t1", "one line"),
        )
        val page = PageLayoutEngine
            .paginate(ReaderBlockBuilder.buildBlocks(contents, policy), policy).first()
        val expected = page.blocks.foldIndexed(0f) { index, total, block ->
            total + (if (index == 0) 0f else policy.blockGap) +
                BlockHeightEstimator.estimate(block, policy)
        }
        assertEquals(expected, page.occupied.height)
        assertEquals(policy.usableWidth, page.occupied.width)
    }

    @Test
    fun empty_content_produces_no_pages() {
        assertEquals(0, PageLayoutEngine.paginate(emptyList(), policy).size)
    }
}
