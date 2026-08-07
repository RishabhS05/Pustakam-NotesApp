package com.app.pustakam.core.richtext.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One editable unit of a document. Blocks are the reason paragraph styles, lists and indentation
 * behave identically on Compose and UIKit: each platform renders one native editor per block.
 */
@Serializable
sealed class RichBlock {
    abstract val id: String

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String,
        val text: String = "",
        val spans: List<RichSpan> = emptyList(),
        val style: ParagraphStyle = ParagraphStyle.PARAGRAPH,
        val align: TextAlign = TextAlign.START,
        val list: ListMarker? = null,
        val indent: Int = 0,
        val firstLineIndent: Boolean = false,
        val lineHeight: Float = DEFAULT_LINE_HEIGHT,
        val paragraphSpacing: Float = DEFAULT_PARAGRAPH_SPACING,
        val checked: Boolean = false
    ) : RichBlock() {

        val isEmpty: Boolean get() = text.isEmpty()

        val isList: Boolean get() = list != null

        val listLevel: Int get() = list?.level ?: 0

        fun withText(newText: String, newSpans: List<RichSpan>): Text =
            copy(text = newText, spans = newSpans)

        fun withStyle(newStyle: ParagraphStyle): Text = copy(style = newStyle)

        fun withList(newList: ListMarker?): Text = copy(list = newList)
    }

    @Serializable
    @SerialName("divider")
    data class Divider(override val id: String) : RichBlock()

    @Serializable
    @SerialName("code")
    data class Code(
        override val id: String,
        val code: String = "",
        val language: String? = null
    ) : RichBlock()

    @Serializable
    @SerialName("table")
    data class Table(
        override val id: String,
        val data: TableData = TableData.empty(2, 2)
    ) : RichBlock()

    val plainText: String
        get() = when (this) {
            is Text -> text
            is Code -> code
            is Divider -> ""
            is Table -> data.rows.joinToString("\n") { row ->
                row.cells.joinToString("\t") { it.text }
            }
        }

    val isEditableText: Boolean get() = this is Text || this is Code

    companion object {
        const val DEFAULT_LINE_HEIGHT = 1.45f
        const val DEFAULT_PARAGRAPH_SPACING = 8f
    }
}
