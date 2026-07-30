package com.app.pustakam.core.filesys.export

// 🔧 30-Jul-2026 02:10 Phase 2 — export LAYOUT, shared. Decides which block lands on which page and
//   at what offset. Renders nothing: it consumes heights that a platform measured and produces
//   coordinates a platform will draw at.
//   Constants and the pagination rule are lifted VERBATIM from androidApp NoteExporter (iOS
//   NoteExporter.swift carries the identical numbers), so both platforms keep their current output.

/** A block whose height a platform has already measured, at a given content width. */
data class MeasuredBlock(
    val block: ExportBlock,
    val height: Float,
)

/** Where a block ends up once laid out. */
data class PlacedBlock(
    val block: ExportBlock,
    /** 0-based page. Always 0 for a continuous (single tall image) layout. */
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val height: Float,
)

/** The finished layout: how many pages, how big, and where everything goes. */
data class ExportLayout(
    val pageCount: Int,
    val pageWidth: Float,
    val pageHeight: Float,
    val contentWidth: Float,
    val placements: List<PlacedBlock>,
)

object ExportLayoutBuilder {

    // A4 @ 72dpi, the long-image width, and the shared margins.
    const val PDF_WIDTH = 595f
    const val PDF_HEIGHT = 842f
    const val IMAGE_WIDTH = 1080f
    const val MARGIN = 40f
    const val BLOCK_GAP = 14f

    /** Drawable width inside the margins. */
    fun contentWidth(pageWidth: Float, margin: Float = MARGIN): Float = pageWidth - 2 * margin

    /**
     * Paginated layout (PDF).
     * Rule, unchanged: place blocks top to bottom; when the next block would cross the bottom
     * margin AND we are not already at the top of a page, start a new page. A block taller than a
     * whole page therefore begins on a fresh page and is allowed to overflow — same as today.
     */
    fun layoutPaged(
        measured: List<MeasuredBlock>,
        pageWidth: Float = PDF_WIDTH,
        pageHeight: Float = PDF_HEIGHT,
        margin: Float = MARGIN,
        gap: Float = BLOCK_GAP,
    ): ExportLayout {
        val placements = mutableListOf<PlacedBlock>()
        val bottom = pageHeight - margin
        var pageIndex = 0
        var y = margin

        measured.forEach { item ->
            if (y + item.height > bottom && y > margin) {
                pageIndex++
                y = margin
            }
            placements += PlacedBlock(item.block, pageIndex, margin, y, item.height)
            y += item.height + gap
        }

        return ExportLayout(
            pageCount = if (measured.isEmpty()) 1 else pageIndex + 1,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            contentWidth = contentWidth(pageWidth, margin),
            placements = placements,
        )
    }

    /**
     * Continuous layout (one tall PNG). Total height is margins + every block + a gap after each,
     * floored at the image width — matching the existing `coerceAtLeast(IMAGE_WIDTH)`.
     */
    fun layoutContinuous(
        measured: List<MeasuredBlock>,
        width: Float = IMAGE_WIDTH,
        margin: Float = MARGIN,
        gap: Float = BLOCK_GAP,
    ): ExportLayout {
        val placements = mutableListOf<PlacedBlock>()
        var y = margin
        measured.forEach { item ->
            placements += PlacedBlock(item.block, 0, margin, y, item.height)
            y += item.height + gap
        }
        val total = (margin + measured.sumOf { (it.height + gap).toDouble() }).toFloat()
        return ExportLayout(
            pageCount = 1,
            pageWidth = width,
            pageHeight = maxOf(total, width),
            contentWidth = contentWidth(width, margin),
            placements = placements,
        )
    }

    /** Blocks that ended up on [pageIndex], in draw order. */
    fun blocksOnPage(layout: ExportLayout, pageIndex: Int): List<PlacedBlock> =
        layout.placements.filter { it.pageIndex == pageIndex }
}
