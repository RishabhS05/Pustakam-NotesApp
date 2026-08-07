package com.app.pustakam.core.richtext.model

/** Where one paragraph sits inside a segment's flat text. */
data class ParagraphSpan(
    val blockId: String,
    val blockIndex: Int,
    val start: Int,
    val end: Int
) {
    val length: Int get() = end - start

    fun contains(offset: Int): Boolean = offset in start..end

    fun intersects(from: Int, to: Int): Boolean = start <= to && from <= end
}

/**
 * A run of consecutive text paragraphs edited as ONE native text field — this is what makes
 * selection span lines and Enter stay inside the editor instead of spawning another field.
 * Media, dividers, tables and code blocks are not text, so they end a run.
 */
data class EditableSegment(
    val id: String,
    val text: String,
    val paragraphs: List<ParagraphSpan>
) {
    val blockIds: List<String> get() = paragraphs.map { it.blockId }

    val isEmpty: Boolean get() = text.isEmpty()

    /** The paragraph a caret sits in; ties at a boundary resolve to the earlier paragraph. */
    fun paragraphAt(offset: Int): ParagraphSpan? =
        paragraphs.lastOrNull { it.start <= offset && offset <= it.end }
            ?: paragraphs.firstOrNull()

    fun paragraphsIn(from: Int, to: Int): List<ParagraphSpan> {
        if (paragraphs.isEmpty()) return emptyList()
        val start = minOf(from, to)
        val end = maxOf(from, to)
        val touched = paragraphs.filter { it.intersects(start, end) }
        return touched.ifEmpty { listOfNotNull(paragraphAt(start)) }
    }

    /** Clips a flat selection onto one paragraph, in that paragraph's own coordinates. */
    fun localRange(paragraph: ParagraphSpan, from: Int, to: Int): IntRange? {
        val start = maxOf(minOf(from, to), paragraph.start) - paragraph.start
        val end = minOf(maxOf(from, to), paragraph.end) - paragraph.start
        return if (end >= start) start..end else null
    }

    fun offsetOf(blockId: String, localOffset: Int): Int? =
        paragraphs.firstOrNull { it.blockId == blockId }?.let { it.start + localOffset }

    companion object {
        const val PARAGRAPH_SEPARATOR = '\n'
    }
}

/** One row of the editor: either a multi-paragraph text field, or a non-text block. */
sealed class SmartTextRow {
    data class Segment(val segment: EditableSegment) : SmartTextRow()
    data class Block(val block: RichBlock) : SmartTextRow()

    val key: String
        get() = when (this) {
            is Segment -> segment.id
            is Block -> block.id
        }
}

/** Maximal runs of consecutive text blocks, each becoming one editable field. */
fun RichDocument.segments(): List<EditableSegment> =
    rows().filterIsInstance<SmartTextRow.Segment>().map { it.segment }

fun RichDocument.rows(): List<SmartTextRow> {
    val rows = mutableListOf<SmartTextRow>()
    var run = mutableListOf<Pair<Int, RichBlock.Text>>()

    fun flushRun() {
        if (run.isEmpty()) return
        rows.add(SmartTextRow.Segment(buildSegment(run)))
        run = mutableListOf()
    }

    blocks.forEachIndexed { index, block ->
        if (block is RichBlock.Text) {
            run.add(index to block)
        } else {
            flushRun()
            rows.add(SmartTextRow.Block(block))
        }
    }
    flushRun()
    return rows
}

fun RichDocument.segmentContaining(blockId: String): EditableSegment? =
    segments().firstOrNull { segment -> segment.paragraphs.any { it.blockId == blockId } }

fun RichDocument.segmentById(segmentId: String): EditableSegment? =
    segments().firstOrNull { it.id == segmentId }

private fun buildSegment(run: List<Pair<Int, RichBlock.Text>>): EditableSegment {
    val builder = StringBuilder()
    val paragraphs = mutableListOf<ParagraphSpan>()
    run.forEachIndexed { position, (blockIndex, block) ->
        if (position > 0) builder.append(EditableSegment.PARAGRAPH_SEPARATOR)
        val start = builder.length
        builder.append(block.text)
        paragraphs.add(ParagraphSpan(block.id, blockIndex, start, builder.length))
    }
    return EditableSegment(
        id = run.first().second.id,
        text = builder.toString(),
        paragraphs = paragraphs
    )
}
