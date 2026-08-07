package com.app.pustakam.core.richtext.model

import kotlinx.serialization.Serializable

/** Exactly one paragraph style per block. [relativeSize] drives Dynamic Type on both platforms. */
@Serializable
enum class ParagraphStyle(val relativeSize: Float, val weight: Int) {
    PARAGRAPH(1.0f, 400),
    TITLE(1.85f, 700),
    SUBTITLE(1.35f, 500),
    HEADING_1(1.6f, 700),
    HEADING_2(1.4f, 700),
    HEADING_3(1.25f, 600),
    HEADING_4(1.15f, 600),
    HEADING_5(1.05f, 600),
    HEADING_6(1.0f, 600),
    CAPTION(0.85f, 400),
    QUOTE(1.0f, 400);

    val isHeading: Boolean
        get() = this == HEADING_1 || this == HEADING_2 || this == HEADING_3 ||
                this == HEADING_4 || this == HEADING_5 || this == HEADING_6

    companion object {
        val all: List<ParagraphStyle> = entries

        fun forMarkdownHashes(count: Int): ParagraphStyle = when (count) {
            1 -> HEADING_1
            2 -> HEADING_2
            3 -> HEADING_3
            4 -> HEADING_4
            5 -> HEADING_5
            6 -> HEADING_6
            else -> PARAGRAPH
        }
    }
}

/** Horizontal alignment of a block. START/END keep RTL locales correct. */
@Serializable
enum class TextAlign {
    START,
    CENTER,
    END,
    JUSTIFY
}

/** List decoration for a text block. Unlimited nesting via [level]. */
@Serializable
enum class ListStyle {
    BULLET,
    NUMBERED,
    CHECKLIST
}

@Serializable
data class ListMarker(
    val style: ListStyle,
    val level: Int = 0,
    val restart: Boolean = false,
    val startNumber: Int = 1
) {
    fun withLevel(newLevel: Int): ListMarker = copy(level = newLevel.coerceAtLeast(0))
}
