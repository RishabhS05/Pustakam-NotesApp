package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.engine.ListEngine
import com.app.pustakam.core.richtext.engine.SegmentEditor
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextAlign

data class MasterParagraphLayout(
    val blockId: String,
    val start: Int,
    val end: Int,
    val style: ParagraphStyle,
    val align: TextAlign,
    val indentLevel: Int,
    val listStyle: ListStyle?,
    val listNumber: Int,
    val checked: Boolean,
    val marker: String,
    val lineHeight: Float,
    val paragraphSpacing: Float,
    val firstLineIndent: Boolean
) {
    val hasMarker: Boolean get() = listStyle != null

    val isChecklist: Boolean get() = listStyle == ListStyle.CHECKLIST
}

object MasterTextLayout {

    fun paragraphs(state: MasterTextState): List<MasterParagraphLayout> {
        val segment = state.segment
        val numbering = ListEngine.numbering(state.document.blocks)
        return segment.paragraphs.mapNotNull { paragraph ->
            val block = state.document.textBlockById(paragraph.blockId) ?: return@mapNotNull null
            val marker = block.list
            val number = numbering[block.id] ?: 1
            MasterParagraphLayout(
                blockId = block.id,
                start = paragraph.start,
                end = paragraph.end,
                style = block.style,
                align = block.align,
                indentLevel = block.listLevel + block.indent,
                listStyle = marker?.style,
                listNumber = number,
                checked = block.checked,
                marker = markerText(marker?.style, marker?.level ?: 0, number),
                lineHeight = block.lineHeight,
                paragraphSpacing = block.paragraphSpacing,
                firstLineIndent = block.firstLineIndent
            )
        }
    }

    fun flatSpans(state: MasterTextState): List<RichSpan> =
        SegmentEditor.flatten(state.document, state.segment)

    fun paragraphAt(state: MasterTextState, offset: Int): MasterParagraphLayout? =
        paragraphs(state).lastOrNull { offset in it.start..it.end }

    fun markerText(style: ListStyle?, level: Int, number: Int): String = when (style) {
        ListStyle.BULLET -> ListEngine.bulletGlyph(level)
        ListStyle.NUMBERED -> "$number."
        ListStyle.CHECKLIST -> ""
        null -> ""
    }

    fun isEmptyDocument(state: MasterTextState): Boolean =
        state.document.blocks.all { it is RichBlock.Text && it.text.isEmpty() }
}
