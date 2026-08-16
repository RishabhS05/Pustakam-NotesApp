package com.app.pustakam.core.filesys.reader

import com.app.pustakam.core.richtext.model.RichBlock

// 📖 15-Aug-2026: a formatted paragraph taller than one page is CUT, never clipped. The cut keeps
//   every character — the boundary lands after a space or a newline, so the next page starts on a
//   word — and re-clips the spans so styling survives the split.
object RichTextSplitter {

    fun split(block: RichBlock.Text, policy: PageLayoutPolicy): List<RichBlock.Text> {
        val limit = maxCharsPerPage(block, policy)
        if (block.text.length <= limit) return listOf(block)

        val bounds = mutableListOf<Pair<Int, Int>>()
        var start = 0
        while (start < block.text.length) {
            val remaining = block.text.length - start
            if (remaining <= limit) {
                bounds.add(start to block.text.length)
                break
            }
            val cut = cutAt(block.text.substring(start, start + limit), limit)
            bounds.add(start to start + cut)
            start += cut
        }

        return bounds.mapIndexed { index, range ->
            val (from, to) = range
            block.copy(
                text = block.text.substring(from, to),
                spans = block.spans.mapNotNull { it.clipped(from, to)?.shifted(-from) },
                // a bullet or number belongs to the first chunk only — it must not repeat every page
                list = if (index == 0) block.list else null,
            )
        }
    }

    /** Characters this block can hold on one page, at its own font size and line height. */
    fun maxCharsPerPage(block: RichBlock.Text, policy: PageLayoutPolicy): Int {
        val size = policy.bodyFontSize * block.style.relativeSize
        val lineHeight = (size * block.lineHeight).coerceAtLeast(1f)
        val room = (policy.usableHeight - block.paragraphSpacing).coerceAtLeast(lineHeight)
        val lines = (room / (lineHeight * policy.overflowGuard)).toInt().coerceAtLeast(1)
        return (BlockHeightEstimator.charsPerLine(block, policy) * lines).coerceAtLeast(1)
    }

    /**
     * Where to cut a full-length window. Same preference as BookPaginator — last newline, else last
     * space, both only past the halfway mark — but the boundary sits AFTER the separator so no
     * character is ever dropped.
     */
    fun cutAt(window: String, limit: Int): Int {
        val half = limit / 2
        window.lastIndexOf('\n').takeIf { it > half }?.let { return it + 1 }
        window.lastIndexOf(' ').takeIf { it > half }?.let { return it + 1 }
        return limit
    }
}
