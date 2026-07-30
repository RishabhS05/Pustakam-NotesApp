package com.app.pustakam.core.filesys.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 2 — pins export layout + block ordering.
//   Heights are supplied by the caller, so this suite is fully deterministic without a renderer.
class ExportLayoutBuilderTest {

    private fun para(text: String) = ExportBlock(ExportBlockKind.PARAGRAPH, text = text)
    private fun measured(vararg heights: Float) =
        heights.mapIndexed { i, h -> MeasuredBlock(para("b$i"), h) }

    // ---- constants are part of the contract ----

    @Test
    fun page_geometry_matches_both_platforms() {
        assertEquals(595f, ExportLayoutBuilder.PDF_WIDTH)
        assertEquals(842f, ExportLayoutBuilder.PDF_HEIGHT)
        assertEquals(1080f, ExportLayoutBuilder.IMAGE_WIDTH)
        assertEquals(40f, ExportLayoutBuilder.MARGIN)
        assertEquals(14f, ExportLayoutBuilder.BLOCK_GAP)
    }

    @Test
    fun content_width_is_the_page_minus_both_margins() {
        assertEquals(515f, ExportLayoutBuilder.contentWidth(595f))
        assertEquals(1000f, ExportLayoutBuilder.contentWidth(1080f))
    }

    // ---- paged layout ----

    @Test
    fun blocks_that_fit_stay_on_one_page() {
        val layout = ExportLayoutBuilder.layoutPaged(measured(100f, 100f, 100f))
        assertEquals(1, layout.pageCount)
        assertTrue(layout.placements.all { it.pageIndex == 0 })
    }

    @Test
    fun the_first_block_starts_at_the_top_margin() {
        val layout = ExportLayoutBuilder.layoutPaged(measured(100f))
        assertEquals(40f, layout.placements[0].y)
        assertEquals(40f, layout.placements[0].x)
    }

    @Test
    fun each_block_is_offset_by_its_height_plus_the_gap() {
        val layout = ExportLayoutBuilder.layoutPaged(measured(100f, 50f))
        assertEquals(40f, layout.placements[0].y)
        assertEquals(40f + 100f + 14f, layout.placements[1].y)
    }

    @Test
    fun a_block_that_would_cross_the_bottom_margin_starts_a_new_page() {
        // usable height = 842 - 40 - 40 = 762
        val layout = ExportLayoutBuilder.layoutPaged(measured(700f, 200f))
        assertEquals(2, layout.pageCount)
        assertEquals(0, layout.placements[0].pageIndex)
        assertEquals(1, layout.placements[1].pageIndex)
        assertEquals(40f, layout.placements[1].y, "a new page restarts at the top margin")
    }

    @Test
    fun a_block_taller_than_a_whole_page_is_not_moved_off_page_zero() {
        // Matches the shipped rule: `y > MARGIN` guards against an infinite page-break loop.
        val layout = ExportLayoutBuilder.layoutPaged(measured(5000f))
        assertEquals(1, layout.pageCount)
        assertEquals(0, layout.placements[0].pageIndex)
    }

    @Test
    fun an_oversized_block_after_content_gets_its_own_fresh_page() {
        val layout = ExportLayoutBuilder.layoutPaged(measured(100f, 5000f))
        assertEquals(2, layout.pageCount)
        assertEquals(1, layout.placements[1].pageIndex)
        assertEquals(40f, layout.placements[1].y)
    }

    @Test
    fun an_empty_note_still_produces_one_page() {
        val layout = ExportLayoutBuilder.layoutPaged(emptyList())
        assertEquals(1, layout.pageCount)
        assertTrue(layout.placements.isEmpty())
    }

    @Test
    fun many_blocks_spill_across_several_pages_in_order() {
        val layout = ExportLayoutBuilder.layoutPaged(List(30) { MeasuredBlock(para("b$it"), 100f) })
        assertTrue(layout.pageCount > 3, "expected several pages, got ${layout.pageCount}")
        val pages = layout.placements.map { it.pageIndex }
        assertEquals(pages.sorted(), pages, "pages must be assigned monotonically")
    }

    // ---- ordering is never disturbed ----

    @Test
    fun block_order_is_preserved_exactly() {
        val blocks = List(25) { MeasuredBlock(para("block-$it"), 90f) }
        val layout = ExportLayoutBuilder.layoutPaged(blocks)
        assertEquals(
            blocks.map { it.block.text },
            layout.placements.map { it.block.text },
            "layout must never reorder blocks",
        )
    }

    @Test
    fun every_measured_block_is_placed_exactly_once() {
        val blocks = List(17) { MeasuredBlock(para("b$it"), 120f) }
        assertEquals(blocks.size, ExportLayoutBuilder.layoutPaged(blocks).placements.size)
    }

    @Test
    fun mixed_block_kinds_keep_their_relative_order() {
        val mixed = listOf(
            MeasuredBlock(ExportBlock(ExportBlockKind.TITLE, text = "T"), 60f),
            MeasuredBlock(ExportBlock(ExportBlockKind.PARAGRAPH, text = "P"), 40f),
            MeasuredBlock(ExportBlock(ExportBlockKind.IMAGE, path = "/i.png"), 300f),
            MeasuredBlock(ExportBlock(ExportBlockKind.FILE, name = "f.pdf"), 30f),
            MeasuredBlock(ExportBlock(ExportBlockKind.LINK, url = "https://x"), 30f),
            MeasuredBlock(ExportBlock(ExportBlockKind.LOCATION, label = "here"), 30f),
        )
        assertEquals(
            listOf(
                ExportBlockKind.TITLE, ExportBlockKind.PARAGRAPH, ExportBlockKind.IMAGE,
                ExportBlockKind.FILE, ExportBlockKind.LINK, ExportBlockKind.LOCATION,
            ),
            ExportLayoutBuilder.layoutPaged(mixed).placements.map { it.block.kind },
        )
    }

    // ---- continuous layout ----

    @Test
    fun continuous_layout_is_a_single_page() {
        val layout = ExportLayoutBuilder.layoutContinuous(measured(100f, 100f, 100f))
        assertEquals(1, layout.pageCount)
        assertTrue(layout.placements.all { it.pageIndex == 0 })
    }

    @Test
    fun continuous_height_is_floored_at_the_image_width() {
        val layout = ExportLayoutBuilder.layoutContinuous(measured(10f))
        assertEquals(1080f, layout.pageHeight)
    }

    @Test
    fun continuous_height_grows_with_content() {
        val layout = ExportLayoutBuilder.layoutContinuous(measured(900f, 900f))
        assertEquals(40f + (900f + 14f) * 2, layout.pageHeight)
    }

    // ---- helper ----

    @Test
    fun blocksOnPage_returns_only_that_page_in_draw_order() {
        val layout = ExportLayoutBuilder.layoutPaged(List(20) { MeasuredBlock(para("b$it"), 200f) })
        val page0 = ExportLayoutBuilder.blocksOnPage(layout, 0)
        assertTrue(page0.isNotEmpty())
        assertTrue(page0.all { it.pageIndex == 0 })
        assertEquals(page0.map { it.y }.sorted(), page0.map { it.y })
    }
}
