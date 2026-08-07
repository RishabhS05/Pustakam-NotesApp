package com.app.pustakam.core.richtext.model

import kotlinx.serialization.Serializable

/**
 * A half-open character range [start, end) carrying character-level styling.
 * Spans never overlap after normalisation — [com.app.pustakam.core.richtext.engine.SpanEngine]
 * splits and merges them so a position maps to exactly one span.
 */
@Serializable
data class RichSpan(
    val start: Int,
    val end: Int,
    val formats: FormatSet = FormatSet.EMPTY,
    val textColor: String? = null,
    val backgroundColor: String? = null,
    val fontSize: Float? = null,
    val fontWeight: Int? = null,
    val fontFamily: String? = null,
    val link: String? = null
) {
    val length: Int get() = end - start

    val isEmpty: Boolean get() = end <= start

    val isStyled: Boolean
        get() = formats.isNotEmpty() || textColor != null || backgroundColor != null ||
                fontSize != null || fontWeight != null || fontFamily != null || link != null

    fun contains(offset: Int): Boolean = offset in start until end

    fun intersects(from: Int, to: Int): Boolean = start < to && from < end

    fun shifted(delta: Int): RichSpan = copy(start = start + delta, end = end + delta)

    fun clipped(from: Int, to: Int): RichSpan? {
        val newStart = maxOf(start, from)
        val newEnd = minOf(end, to)
        return if (newEnd > newStart) copy(start = newStart, end = newEnd) else null
    }

    /** Same styling, ignoring position — used to merge neighbours during normalisation. */
    fun sameStyleAs(other: RichSpan): Boolean =
        formats == other.formats &&
                textColor == other.textColor &&
                backgroundColor == other.backgroundColor &&
                fontSize == other.fontSize &&
                fontWeight == other.fontWeight &&
                fontFamily == other.fontFamily &&
                link == other.link

    fun styleOnly(): RichSpan = copy(start = 0, end = 0)
}
