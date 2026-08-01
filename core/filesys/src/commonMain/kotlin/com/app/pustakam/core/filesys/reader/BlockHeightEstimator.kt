package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import kotlin.math.ceil
import kotlin.math.max

// 📖 01-Aug-2026 Step 3 — every block reports its height in policy units, which ARE screen units.
//   Grids show EVERY item (no "+N"): a grid too tall for one page is split into consecutive blocks
//   by the builder, so it flows onto the next page instead of overflowing.
object BlockHeightEstimator {

    fun estimate(block: ReaderBlock, policy: PageLayoutPolicy): Float = when (block) {
        is ReaderBlock.Title ->
            policy.titleHeight + if (block.subtitle != null) policy.captionHeight else 0f

        is ReaderBlock.Paragraph -> textHeight(block.text, policy)

        // a lone image is aspect-aware and full width; two or more use uniform 4:3 grid cells
        is ReaderBlock.ImageGrid ->
            if (block.items.size == 1) singleImageHeight(block.items[0], policy)
            else gridHeight(block.items.size, policy.imageCellHeight, policy)

        // a lone video is full width at 16:9; grid cells keep the same ratio
        is ReaderBlock.VideoGrid ->
            if (block.items.size == 1) policy.usableWidth * PageLayoutPolicy.VIDEO_ASPECT
            else gridHeight(block.items.size, policy.videoCellHeight, policy)

        is ReaderBlock.Audio -> policy.audioHeight
        // a document owns its page: title + inline pdf pages + Load more
        is ReaderBlock.Document -> policy.usableHeight
        is ReaderBlock.Link -> policy.linkHeight
        is ReaderBlock.Location -> policy.locationHeight
    }

    /** Lines × line height. A hard newline and a wrapped line each consume one line. */
    fun textHeight(text: String, policy: PageLayoutPolicy): Float {
        if (text.isEmpty()) return 0f
        val lines = text.split('\n').sumOf { line ->
            if (line.isEmpty()) 1 else ceil(line.length.toDouble() / policy.charsPerLine).toInt()
        }
        return max(1, lines) * policy.bodyLineHeight
    }

    /** Rows needed for [total] items in the 2-column grid — every item is shown. */
    fun rowsFor(total: Int, policy: PageLayoutPolicy): Int =
        ceil(total.toDouble() / policy.gridColumns).toInt().coerceAtLeast(1)

    /** Items that still fit on one page for a grid of [cellHeight] cells — used to split a grid. */
    fun maxItemsPerPage(cellHeight: Float, policy: PageLayoutPolicy): Int {
        val rowPitch = cellHeight + policy.gridSpacing
        if (rowPitch <= 0f) return policy.gridColumns
        val rows = (policy.usableHeight / rowPitch).toInt().coerceAtLeast(1)
        return rows * policy.gridColumns
    }

    fun gridHeight(total: Int, cellHeight: Float, policy: PageLayoutPolicy): Float {
        val rows = rowsFor(total, policy)
        return rows * cellHeight + (rows - 1) * policy.gridSpacing
    }

    /** Aspect-aware height for a single image; falls back to the max when the size is unknown. */
    fun singleImageHeight(media: NoteContentModel.MediaContent, policy: PageLayoutPolicy): Float {
        if (media.width <= 0 || media.height <= 0) return policy.imageMaxHeight
        // toFloat() first — integer division here silently collapsed every portrait image
        val scaled = policy.usableWidth * (media.height.toFloat() / media.width.toFloat())
        return scaled.coerceIn(policy.imageMinHeight, policy.imageMaxHeight)
    }

    /** Swift-facing accessor — a `const val` inside an object has no guaranteed export shape. */
    fun videoAspect(): Float = PageLayoutPolicy.VIDEO_ASPECT
}
