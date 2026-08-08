package com.app.pustakam.android.widgets.masterEditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle as ComposeParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.widgets.smartText.SmartTextColors
import com.app.pustakam.android.widgets.smartText.SmartTextStyleMapper
import com.app.pustakam.core.richtext.master.presentation.MasterParagraphLayout
import com.app.pustakam.core.richtext.master.presentation.MasterTextLayout
import com.app.pustakam.core.richtext.master.presentation.MasterTextState
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle

object MasterTextRenderer {

    const val INDENT_STEP_SP = 20f
    const val MARKER_GUTTER_SP = 26f

    fun annotate(
        state: MasterTextState,
        colors: SmartTextColors,
        baseSize: TextUnit
    ): AnnotatedString {
        val text = state.text
        val builder = AnnotatedString.Builder(text)
        val paragraphs = MasterTextLayout.paragraphs(state)

        paragraphs.forEach { paragraph ->
            val start = paragraph.start.coerceIn(0, text.length)
            val end = paragraph.end.coerceIn(start, text.length)
            builder.addStyle(paragraphStyle(paragraph, baseSize), start, end)
            builder.addStyle(paragraphSpanStyle(paragraph, colors, baseSize), start, end)
        }

        MasterTextLayout.flatSpans(state).forEach { span ->
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (end > start) {
                builder.addStyle(SmartTextStyleMapper.spanStyle(span, colors, baseSize), start, end)
            }
        }
        return builder.toAnnotatedString()
    }

    private fun paragraphStyle(
        paragraph: MasterParagraphLayout,
        baseSize: TextUnit
    ): ComposeParagraphStyle {
        val indent = (paragraph.indentLevel * INDENT_STEP_SP) +
            if (paragraph.hasMarker) MARKER_GUTTER_SP else 0f
        val size = baseSize * paragraph.style.relativeSize
        return ComposeParagraphStyle(
            textAlign = SmartTextStyleMapper.alignOf(paragraph.align),
            lineHeight = size * paragraph.lineHeight,
            textIndent = TextIndent(
                firstLine = if (paragraph.firstLineIndent) (indent + 24f).sp else indent.sp,
                restLine = indent.sp
            )
        )
    }

    private fun paragraphSpanStyle(
        paragraph: MasterParagraphLayout,
        colors: SmartTextColors,
        baseSize: TextUnit
    ) = androidx.compose.ui.text.SpanStyle(
        color = if (paragraph.style == ParagraphStyle.CAPTION) colors.onSurfaceMuted else colors.onSurface,
        fontSize = baseSize * paragraph.style.relativeSize,
        fontWeight = FontWeight(paragraph.style.weight)
    )

    fun drawMarkers(
        scope: DrawScope,
        state: MasterTextState,
        layout: TextLayoutResult,
        measurer: TextMeasurer,
        colors: SmartTextColors,
        baseSize: TextUnit,
        density: Density
    ) {
        MasterTextLayout.paragraphs(state).forEach { paragraph ->
            if (!paragraph.hasMarker) return@forEach
            val offset = paragraph.start.coerceIn(0, layout.layoutInput.text.length)
            val line = runCatching { layout.getLineForOffset(offset) }.getOrNull() ?: return@forEach
            val top = layout.getLineTop(line)
            val indentPx = with(density) {
                (paragraph.indentLevel * INDENT_STEP_SP).sp.toPx()
            }

            if (paragraph.isChecklist) {
                drawCheckbox(scope, indentPx, top, paragraph.checked, colors, density)
                return@forEach
            }

            val glyph = measurer.measure(
                text = AnnotatedString(paragraph.marker),
                style = TextStyle(
                    color = colors.accent,
                    fontSize = baseSize * paragraph.style.relativeSize,
                    fontWeight = FontWeight.Medium
                )
            )
            scope.drawText(glyph, topLeft = Offset(indentPx, top))
        }
    }

    private fun drawCheckbox(
        scope: DrawScope,
        left: Float,
        top: Float,
        checked: Boolean,
        colors: SmartTextColors,
        density: Density
    ) {
        val size = with(density) { 16.dp.toPx() }
        val stroke = with(density) { 1.5.dp.toPx() }
        val corner = with(density) { 4.dp.toPx() }
        val offset = Offset(left, top + stroke)
        if (checked) {
            scope.drawRoundRect(
                color = colors.accent,
                topLeft = offset,
                size = androidx.compose.ui.geometry.Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
            )
        } else {
            scope.drawRoundRect(
                color = colors.onSurfaceMuted,
                topLeft = offset,
                size = androidx.compose.ui.geometry.Size(size, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
        }
    }

    fun markerHitOffset(
        state: MasterTextState,
        layout: TextLayoutResult,
        position: Offset,
        density: Density
    ): Int? {
        val gutter = with(density) { MARKER_GUTTER_SP.sp.toPx() }
        return MasterTextLayout.paragraphs(state).firstOrNull { paragraph ->
            if (paragraph.listStyle != ListStyle.CHECKLIST) return@firstOrNull false
            val offset = paragraph.start.coerceIn(0, layout.layoutInput.text.length)
            val line = runCatching { layout.getLineForOffset(offset) }.getOrNull()
                ?: return@firstOrNull false
            val indentPx = with(density) { (paragraph.indentLevel * INDENT_STEP_SP).sp.toPx() }
            position.y in layout.getLineTop(line)..layout.getLineBottom(line) &&
                position.x in indentPx..(indentPx + gutter)
        }?.start
    }
}
