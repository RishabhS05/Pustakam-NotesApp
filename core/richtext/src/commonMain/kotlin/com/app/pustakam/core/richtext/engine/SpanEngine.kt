package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.FormatSet
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextFormat

/**
 * Span algebra. Every public function returns a normalised list: sorted, non-overlapping,
 * adjacent equal styles merged, unstyled and empty spans dropped.
 */
object SpanEngine {

    fun normalize(spans: List<RichSpan>, textLength: Int): List<RichSpan> {
        if (spans.isEmpty()) return emptyList()
        val clipped = spans
            .mapNotNull { it.clipped(0, textLength) }
            .filter { it.isStyled && !it.isEmpty }
            .sortedWith(compareBy({ it.start }, { it.end }))
        if (clipped.isEmpty()) return emptyList()

        val merged = mutableListOf<RichSpan>()
        clipped.forEach { span ->
            val last = merged.lastOrNull()
            if (last != null && last.end == span.start && last.sameStyleAs(span)) {
                merged[merged.lastIndex] = last.copy(end = span.end)
            } else {
                merged.add(span)
            }
        }
        return merged
    }

    /** The span covering [offset], or null when that character is unstyled. */
    fun spanAt(spans: List<RichSpan>, offset: Int): RichSpan? =
        spans.firstOrNull { it.contains(offset) }

    /** Style a newly typed character inherits — the character to its left. */
    fun styleForCaret(spans: List<RichSpan>, caret: Int): RichSpan? =
        spanAt(spans, (caret - 1).coerceAtLeast(0)) ?: spanAt(spans, caret)

    /** Formats shared by every character in the range; a collapsed range reads the caret style. */
    fun commonFormats(spans: List<RichSpan>, start: Int, end: Int): FormatSet {
        if (end <= start) return styleForCaret(spans, start)?.formats ?: FormatSet.EMPTY
        val ordered = spans.sortedBy { it.start }
        var covered = start
        var accumulated: FormatSet? = null
        for (span in ordered) {
            if (span.end <= start || span.start >= end) continue
            if (span.start > covered) return FormatSet.EMPTY
            accumulated = accumulated?.intersect(span.formats) ?: span.formats
            covered = maxOf(covered, span.end)
            if (covered >= end) break
        }
        return if (covered < end) FormatSet.EMPTY else accumulated ?: FormatSet.EMPTY
    }

    fun isActive(spans: List<RichSpan>, start: Int, end: Int, format: TextFormat): Boolean =
        commonFormats(spans, start, end).has(format)

    /** Attribute shared by the whole range, or null when it varies. */
    fun commonAttribute(
        spans: List<RichSpan>,
        start: Int,
        end: Int,
        selector: (RichSpan) -> String?
    ): String? {
        if (end <= start) return styleForCaret(spans, start)?.let(selector)
        val touching = spans.filter { it.intersects(start, end) }
        if (touching.isEmpty()) return null
        val covered = touching.sumOf { minOf(it.end, end) - maxOf(it.start, start) }
        if (covered < end - start) return null
        val first = selector(touching.first())
        return if (touching.all { selector(it) == first }) first else null
    }

    fun apply(
        spans: List<RichSpan>,
        start: Int,
        end: Int,
        textLength: Int,
        transform: (RichSpan) -> RichSpan
    ): List<RichSpan> {
        if (end <= start) return normalize(spans, textLength)
        val pieces = splitAt(splitAt(spans, start), end).sortedBy { it.start }
        val result = mutableListOf<RichSpan>()
        var cursor = start
        pieces.forEach { piece ->
            if (piece.end <= start || piece.start >= end) {
                result.add(piece)
                return@forEach
            }
            if (piece.start > cursor) result.add(transform(RichSpan(cursor, piece.start)))
            result.add(transform(piece))
            cursor = piece.end
        }
        if (cursor < end) result.add(transform(RichSpan(cursor, end)))
        return normalize(result, textLength)
    }

    fun setFormat(
        spans: List<RichSpan>,
        start: Int,
        end: Int,
        textLength: Int,
        format: TextFormat,
        enabled: Boolean
    ): List<RichSpan> = apply(spans, start, end, textLength) { span ->
        val exclusive = FormatSet.exclusiveOf(format)
        val base = if (enabled && exclusive != null) span.formats.minus(exclusive) else span.formats
        span.copy(formats = if (enabled) base.plus(format) else base.minus(format))
    }

    fun toggleFormat(
        spans: List<RichSpan>,
        start: Int,
        end: Int,
        textLength: Int,
        format: TextFormat
    ): List<RichSpan> =
        setFormat(spans, start, end, textLength, format, !isActive(spans, start, end, format))

    fun setTextColor(spans: List<RichSpan>, start: Int, end: Int, textLength: Int, color: String?) =
        apply(spans, start, end, textLength) { it.copy(textColor = color) }

    fun setBackgroundColor(spans: List<RichSpan>, start: Int, end: Int, textLength: Int, color: String?) =
        apply(spans, start, end, textLength) { it.copy(backgroundColor = color) }

    fun setFontSize(spans: List<RichSpan>, start: Int, end: Int, textLength: Int, size: Float?) =
        apply(spans, start, end, textLength) { it.copy(fontSize = size) }

    fun setFontWeight(spans: List<RichSpan>, start: Int, end: Int, textLength: Int, weight: Int?) =
        apply(spans, start, end, textLength) { it.copy(fontWeight = weight) }

    fun setFontFamily(spans: List<RichSpan>, start: Int, end: Int, textLength: Int, family: String?) =
        apply(spans, start, end, textLength) { it.copy(fontFamily = family) }

    fun setLink(spans: List<RichSpan>, start: Int, end: Int, textLength: Int, url: String?) =
        apply(spans, start, end, textLength) { it.copy(link = url) }

    fun clearFormatting(spans: List<RichSpan>, start: Int, end: Int, textLength: Int): List<RichSpan> =
        normalize(spans.flatMap { span ->
            if (!span.intersects(start, end)) listOf(span)
            else listOfNotNull(span.clipped(span.start, start), span.clipped(end, span.end))
        }, textLength)

    /** Link covering the caret — used by "Edit link" and "Open link". */
    fun linkAt(spans: List<RichSpan>, offset: Int): RichSpan? =
        spans.firstOrNull { it.link != null && it.contains(offset) }

    fun afterInsert(spans: List<RichSpan>, at: Int, length: Int): List<RichSpan> =
        spans.map { span ->
            when {
                span.end < at -> span
                span.start >= at -> span.shifted(length)
                else -> span.copy(end = span.end + length)
            }
        }

    fun afterDelete(spans: List<RichSpan>, from: Int, to: Int): List<RichSpan> {
        val removed = to - from
        if (removed <= 0) return spans
        return spans.mapNotNull { span ->
            val newStart = when {
                span.start >= to -> span.start - removed
                span.start > from -> from
                else -> span.start
            }
            val newEnd = when {
                span.end >= to -> span.end - removed
                span.end > from -> from
                else -> span.end
            }
            if (newEnd > newStart) span.copy(start = newStart, end = newEnd) else null
        }
    }

    /** Spans of a sub-range, re-based to 0 — used when a block is split or copied. */
    fun slice(spans: List<RichSpan>, from: Int, to: Int): List<RichSpan> =
        normalize(spans.mapNotNull { it.clipped(from, to)?.shifted(-from) }, to - from)

    /** Concatenates the spans of two blocks being merged. */
    fun concat(
        first: List<RichSpan>,
        firstLength: Int,
        second: List<RichSpan>
    ): List<RichSpan> = first + second.map { it.shifted(firstLength) }

    private fun splitAt(spans: List<RichSpan>, at: Int): List<RichSpan> =
        spans.flatMap { span ->
            if (at > span.start && at < span.end) {
                listOf(span.copy(end = at), span.copy(start = at))
            } else {
                listOf(span)
            }
        }
}
