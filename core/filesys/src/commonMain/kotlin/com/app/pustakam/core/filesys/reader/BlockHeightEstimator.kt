package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


object BlockHeightEstimator {

    /** Stage 2 — full measurement. Media never breaks across pages; only text may. */
    fun measure(block: ReaderBlock, policy: PageLayoutPolicy): MeasuredBlock = MeasuredBlock(
        block = block,
        size = size(block, policy),
        breakable = block is ReaderBlock.Paragraph,
    )

    /** Width AND height a block occupies. Most blocks span the usable width; a lone image may not. */
    fun size(block: ReaderBlock, policy: PageLayoutPolicy): BlockSize {
        val height = estimate(block, policy)
        val width = when (block) {
            // a portrait image is height-clamped, so it renders narrower than the column
            is ReaderBlock.ImageGrid ->
                if (block.items.size == 1) singleImageWidth(block.items[0], height, policy)
                else policy.usableWidth

            else -> policy.usableWidth
        }
        return BlockSize(width, height)
    }

    fun estimate(block: ReaderBlock, policy: PageLayoutPolicy): Float = when (block) {
        is ReaderBlock.Title ->
            policy.titleHeight + if (block.subtitle != null) policy.captionHeight else 0f

        is ReaderBlock.Paragraph -> textHeight(block.text, policy)

        // a lone image is aspect-aware and full width; two or more use uniform 4:3 grid cells
        is ReaderBlock.ImageGrid ->
            if (block.items.size == 1) singleImageHeight(block.items[0], policy)
            else gridHeight(block.items.size, policy.gridCellWidth * policy.gridCellAspect, policy)

        // a lone video is full width at 16:9; grid cells keep the same ratio
        is ReaderBlock.VideoGrid ->
            if (block.items.size == 1) policy.usableWidth * VIDEO_ASPECT
            else gridHeight(block.items.size, policy.gridCellWidth * VIDEO_ASPECT, policy)

        is ReaderBlock.Audio -> policy.audioHeight
        is ReaderBlock.Document -> policy.documentHeight
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

    /** Cells actually drawn — 5+ collapses to gridMaxCells with a "+N" on the last one. */
    fun visibleCells(total: Int, policy: PageLayoutPolicy): Int =
        min(total, policy.gridMaxCells).coerceAtLeast(1)

    fun overflowCount(total: Int, policy: PageLayoutPolicy): Int =
        (total - policy.gridMaxCells).coerceAtLeast(0)

    /** Rows needed for [total] items in the 2-column grid, capped at gridMaxCells. */
    fun rowsFor(total: Int, policy: PageLayoutPolicy): Int =
        ceil(visibleCells(total, policy).toDouble() / policy.gridColumns).toInt().coerceAtLeast(1)

    private fun gridHeight(total: Int, cellHeight: Float, policy: PageLayoutPolicy): Float {
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

    /** Rendered width of a lone image once its height has been clamped. */
    fun singleImageWidth(
        media: NoteContentModel.MediaContent,
        height: Float,
        policy: PageLayoutPolicy,
    ): Float {
        if (media.width <= 0 || media.height <= 0) return policy.usableWidth
        val ratio = media.width.toFloat() / media.height.toFloat()
        return (height * ratio).coerceAtMost(policy.usableWidth)
    }

    const val VIDEO_ASPECT = 9f / 16f

    /** Swift-facing accessor — a `const val` inside an object has no guaranteed export shape. */
    fun videoAspect(): Float = VIDEO_ASPECT
}
