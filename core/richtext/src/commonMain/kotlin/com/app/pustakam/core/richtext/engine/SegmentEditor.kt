package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.common.util.UniqueIdGenerator
import com.app.pustakam.core.richtext.model.EditableSegment
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan

data class SegmentEditResult(val document: RichDocument, val caret: Int)

/**
 * Turns a whole-segment text edit back into blocks.
 *
 * The editor hands over the segment's entire flat text on every keystroke. This re-splits it on
 * newlines, keeps the identity and paragraph styling of every paragraph the edit did not touch,
 * and rebases character spans across the change. Pressing Enter therefore stays inside one text
 * field while still producing a real new paragraph underneath.
 */
object SegmentEditor {

    fun applyEdit(
        document: RichDocument,
        segment: EditableSegment,
        newText: String,
        caret: Int,
        pendingStyle: RichSpan? = null
    ): SegmentEditResult {
        val oldText = segment.text
        if (oldText == newText) return SegmentEditResult(document, caret)

        val oldBlocks = segment.paragraphs.map { document.textBlockById(it.blockId) ?: return SegmentEditResult(document, caret) }

        val prefix = commonPrefixLength(oldText, newText)
        val suffix = commonSuffixLength(oldText, newText, prefix)
        val removedTo = oldText.length - suffix
        val insertedLength = newText.length - suffix - prefix

        // Enter on an empty list item drops the decoration instead of adding another row
        val newlineOnly = insertedLength == 1 && newText.getOrNull(prefix) == EditableSegment.PARAGRAPH_SEPARATOR
        if (newlineOnly && removedTo == prefix) {
            val index = paragraphIndexAt(segment, prefix)
            val block = oldBlocks[index]
            if (ListEngine.shouldExitList(block)) {
                return SegmentEditResult(
                    document.replacingBlock(ListEngine.outdent(block)),
                    caret = prefix
                )
            }
        }

        var flat = flatten(segment, oldBlocks)
        val caretSource = pendingStyle
            ?: SpanEngine.styleForCaret(flat, prefix)?.takeIf { it.link == null }

        flat = SpanEngine.afterDelete(flat, prefix, removedTo)
        if (insertedLength > 0) {
            flat = SpanEngine.afterInsert(flat, prefix, insertedLength)
            if (caretSource != null) {
                flat = SpanEngine.apply(flat, prefix, prefix + insertedLength, newText.length) { span ->
                    span.copy(
                        formats = caretSource.formats,
                        textColor = caretSource.textColor,
                        backgroundColor = caretSource.backgroundColor,
                        fontSize = caretSource.fontSize,
                        fontWeight = caretSource.fontWeight,
                        fontFamily = caretSource.fontFamily
                    )
                }
            }
        }
        flat = SpanEngine.normalize(flat, newText.length)

        val paragraphTexts = newText.split(EditableSegment.PARAGRAPH_SEPARATOR)
        val oldCount = oldBlocks.size
        val newCount = paragraphTexts.size
        val maxKeep = minOf(oldCount, newCount)
        val head = paragraphIndexAt(segment, prefix).coerceIn(0, maxKeep)
        val tail = (oldCount - 1 - paragraphIndexAt(segment, removedTo)).coerceIn(0, maxKeep - head)

        val rebuilt = mutableListOf<RichBlock.Text>()
        var offset = 0
        paragraphTexts.forEachIndexed { index, paragraphText ->
            val start = offset
            val end = offset + paragraphText.length
            offset = end + 1

            val fromHead = index < head
            val fromTail = index >= newCount - tail
            val template = when {
                fromHead -> oldBlocks[index]
                fromTail -> oldBlocks[oldCount - (newCount - index)]
                index == head -> oldBlocks[head.coerceAtMost(oldCount - 1)]
                else -> siblingOf(oldBlocks[head.coerceAtMost(oldCount - 1)])
            }
            val keepsIdentity = fromHead || fromTail || index == head
            rebuilt.add(
                template.copy(
                    id = if (keepsIdentity) template.id else UniqueIdGenerator.generateUniqueId(),
                    text = paragraphText,
                    spans = SpanEngine.slice(flat, start, end)
                )
            )
        }

        val blocks = document.blocks.toMutableList()
        val firstIndex = segment.paragraphs.first().blockIndex
        repeat(oldCount) { blocks.removeAt(firstIndex) }
        blocks.addAll(firstIndex, rebuilt)

        return SegmentEditResult(
            document = document.withBlocks(blocks),
            caret = (prefix + insertedLength).coerceIn(0, newText.length)
        )
    }

    /** Spans of every paragraph in one coordinate space, the separator counted as one character. */
    fun flatten(segment: EditableSegment, blocks: List<RichBlock.Text>): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        segment.paragraphs.forEachIndexed { index, paragraph ->
            val block = blocks.getOrNull(index) ?: return@forEachIndexed
            result.addAll(block.spans.map { it.shifted(paragraph.start) })
        }
        return SpanEngine.normalize(result, segment.text.length)
    }

    fun flatten(document: RichDocument, segment: EditableSegment): List<RichSpan> =
        flatten(segment, segment.paragraphs.mapNotNull { document.textBlockById(it.blockId) })

    /** A paragraph created by pressing Enter: same list, never an inherited heading. */
    private fun siblingOf(block: RichBlock.Text): RichBlock.Text = block.copy(
        style = if (block.style == ParagraphStyle.PARAGRAPH || block.style == ParagraphStyle.QUOTE) {
            block.style
        } else {
            ParagraphStyle.PARAGRAPH
        },
        list = ListEngine.markerForNewSibling(block),
        checked = false,
        spans = emptyList()
    )

    private fun paragraphIndexAt(segment: EditableSegment, offset: Int): Int {
        segment.paragraphs.forEachIndexed { index, paragraph ->
            if (offset <= paragraph.end) return index
        }
        return (segment.paragraphs.size - 1).coerceAtLeast(0)
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        var index = 0
        val max = minOf(a.length, b.length)
        while (index < max && a[index] == b[index]) index++
        return index
    }

    private fun commonSuffixLength(a: String, b: String, prefix: Int): Int {
        var index = 0
        val max = minOf(a.length - prefix, b.length - prefix)
        while (index < max && a[a.length - 1 - index] == b[b.length - 1 - index]) index++
        return index
    }
}
