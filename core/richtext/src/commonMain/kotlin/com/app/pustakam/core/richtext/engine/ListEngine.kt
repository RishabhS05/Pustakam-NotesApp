package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.ListMarker
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock

/** List decoration, unlimited nesting, and the numbering pass both platforms render from. */
object ListEngine {

    const val MAX_LEVEL = 8

    fun toggle(block: RichBlock.Text, style: ListStyle): RichBlock.Text {
        val current = block.list
        return if (current?.style == style) {
            block.copy(list = null, indent = 0, checked = false)
        } else {
            block.copy(
                list = ListMarker(style = style, level = current?.level ?: 0),
                style = if (block.style.isHeading) block.style else ParagraphStyle.PARAGRAPH,
                checked = false
            )
        }
    }

    fun indent(block: RichBlock.Text): RichBlock.Text {
        val marker = block.list
            ?: return block.copy(indent = (block.indent + 1).coerceAtMost(MAX_LEVEL))
        return block.copy(list = marker.withLevel((marker.level + 1).coerceAtMost(MAX_LEVEL)))
    }

    fun outdent(block: RichBlock.Text): RichBlock.Text {
        val marker = block.list
            ?: return block.copy(indent = (block.indent - 1).coerceAtLeast(0))
        return if (marker.level == 0) block.copy(list = null, checked = false)
        else block.copy(list = marker.withLevel(marker.level - 1))
    }

    fun canIndent(block: RichBlock.Text): Boolean =
        (block.list?.level ?: block.indent) < MAX_LEVEL

    fun canOutdent(block: RichBlock.Text): Boolean =
        block.list != null || block.indent > 0

    fun restartNumbering(block: RichBlock.Text, startNumber: Int = 1): RichBlock.Text {
        val marker = block.list ?: return block
        return block.copy(list = marker.copy(restart = true, startNumber = startNumber))
    }

    fun continueNumbering(block: RichBlock.Text): RichBlock.Text {
        val marker = block.list ?: return block
        return block.copy(list = marker.copy(restart = false))
    }

    fun toggleChecked(block: RichBlock.Text): RichBlock.Text =
        if (block.list?.style == ListStyle.CHECKLIST) block.copy(checked = !block.checked) else block

    /**
     * Display number per numbered-list block id. Deeper levels reset when the list steps back out,
     * and a non-list block ends the sequence unless the next marker asks to continue.
     */
    fun numbering(blocks: List<RichBlock>): Map<String, Int> {
        val counters = mutableMapOf<Int, Int>()
        val numbers = mutableMapOf<String, Int>()
        var lastLevel = -1

        blocks.forEach { block ->
            val marker = (block as? RichBlock.Text)?.list
            if (marker == null) {
                counters.clear()
                lastLevel = -1
                return@forEach
            }
            if (marker.level < lastLevel) {
                counters.keys.filter { it > marker.level }.toList().forEach { counters.remove(it) }
            }
            if (marker.style != ListStyle.NUMBERED) {
                lastLevel = marker.level
                return@forEach
            }
            val next = if (marker.restart) {
                marker.startNumber
            } else {
                (counters[marker.level] ?: (marker.startNumber - 1)) + 1
            }
            counters[marker.level] = next
            numbers[block.id] = next
            lastLevel = marker.level
        }
        return numbers
    }

    /** Bullet glyph cycles by depth the way Word and Notion do. */
    fun bulletGlyph(level: Int): String = when (level % 3) {
        0 -> "•"
        1 -> "◦"
        else -> "▪"
    }

    /** Pressing Enter on an empty list item drops the decoration instead of adding another row. */
    fun shouldExitList(block: RichBlock.Text): Boolean = block.list != null && block.text.isEmpty()

    /** The marker a newly created sibling inherits. */
    fun markerForNewSibling(block: RichBlock.Text): ListMarker? =
        block.list?.copy(restart = false)
}
