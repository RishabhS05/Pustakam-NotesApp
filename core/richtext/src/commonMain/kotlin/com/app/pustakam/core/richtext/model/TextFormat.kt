package com.app.pustakam.core.richtext.model

import kotlinx.serialization.Serializable

/** Character formats are bit flags so Bold + Italic + Highlight can overlap on one span. */
@Serializable
enum class TextFormat(val bit: Int) {
    BOLD(1 shl 0),
    ITALIC(1 shl 1),
    UNDERLINE(1 shl 2),
    STRIKETHROUGH(1 shl 3),
    HIGHLIGHT(1 shl 4),
    CODE(1 shl 5),
    SUPERSCRIPT(1 shl 6),
    SUBSCRIPT(1 shl 7);

    companion object {
        val all: List<TextFormat> = entries
    }
}

/** Immutable set of [TextFormat] packed into an Int — Swift-friendly and cheap to compare. */
@Serializable
data class FormatSet(val mask: Int = 0) {

    fun has(format: TextFormat): Boolean = mask and format.bit != 0

    fun plus(format: TextFormat): FormatSet = FormatSet(mask or format.bit)

    fun minus(format: TextFormat): FormatSet = FormatSet(mask and format.bit.inv())

    fun toggle(format: TextFormat): FormatSet = if (has(format)) minus(format) else plus(format)

    fun union(other: FormatSet): FormatSet = FormatSet(mask or other.mask)

    fun intersect(other: FormatSet): FormatSet = FormatSet(mask and other.mask)

    fun isEmpty(): Boolean = mask == 0

    fun isNotEmpty(): Boolean = mask != 0

    fun toList(): List<TextFormat> = TextFormat.all.filter { has(it) }

    companion object {
        val EMPTY = FormatSet(0)

        // SUPERSCRIPT and SUBSCRIPT are mutually exclusive — applying one drops the other
        fun of(vararg formats: TextFormat): FormatSet =
            formats.fold(EMPTY) { acc, format -> acc.plus(format) }

        fun exclusiveOf(format: TextFormat): TextFormat? = when (format) {
            TextFormat.SUPERSCRIPT -> TextFormat.SUBSCRIPT
            TextFormat.SUBSCRIPT -> TextFormat.SUPERSCRIPT
            else -> null
        }
    }
}
