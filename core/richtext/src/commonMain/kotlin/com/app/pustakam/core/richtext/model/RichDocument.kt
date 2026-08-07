package com.app.pustakam.core.richtext.model

import com.app.pustakam.core.common.util.UniqueIdGenerator
import kotlinx.serialization.Serializable

/** The whole rich-text value of one TextContent. Always holds at least one block. */
@Serializable
data class RichDocument(
    val blocks: List<RichBlock> = emptyList()
) {
    val isEmpty: Boolean
        get() = blocks.isEmpty() || (blocks.size == 1 && blocks.first().plainText.isEmpty())

    /** Plain-text projection — what the existing TextContent.text column keeps holding. */
    val plainText: String
        get() = blocks.joinToString("\n") { it.plainText }

    fun indexOf(blockId: String): Int = blocks.indexOfFirst { it.id == blockId }

    fun blockById(blockId: String): RichBlock? = blocks.firstOrNull { it.id == blockId }

    fun textBlockById(blockId: String): RichBlock.Text? = blockById(blockId) as? RichBlock.Text

    fun withBlocks(newBlocks: List<RichBlock>): RichDocument =
        copy(blocks = newBlocks.ifEmpty { listOf(newTextBlock()) })

    fun replacingBlock(block: RichBlock): RichDocument {
        val index = indexOf(block.id)
        if (index < 0) return this
        return withBlocks(blocks.toMutableList().apply { set(index, block) })
    }

    fun insertingBlock(block: RichBlock, at: Int): RichDocument =
        withBlocks(blocks.toMutableList().apply { add(at.coerceIn(0, blocks.size), block) })

    fun removingBlock(blockId: String): RichDocument {
        val index = indexOf(blockId)
        if (index < 0 || blocks.size <= 1) return this
        return withBlocks(blocks.toMutableList().apply { removeAt(index) })
    }

    companion object {
        fun newTextBlock(
            text: String = "",
            spans: List<RichSpan> = emptyList(),
            style: ParagraphStyle = ParagraphStyle.PARAGRAPH,
            list: ListMarker? = null,
            indent: Int = 0
        ): RichBlock.Text = RichBlock.Text(
            id = UniqueIdGenerator.generateUniqueId(),
            text = text,
            spans = spans,
            style = style,
            list = list,
            indent = indent
        )

        fun empty(): RichDocument = RichDocument(listOf(newTextBlock()))

        /** Every plain-text line becomes a paragraph — the migration path for existing notes. */
        fun fromPlainText(text: String): RichDocument {
            if (text.isEmpty()) return empty()
            return RichDocument(text.split("\n").map { newTextBlock(it) })
        }
    }
}
