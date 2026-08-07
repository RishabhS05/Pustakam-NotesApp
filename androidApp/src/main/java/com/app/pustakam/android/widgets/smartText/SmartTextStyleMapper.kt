package com.app.pustakam.android.widgets.smartText

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.model.TextFormat

// turns the shared span model into Compose styling — the only place that mapping lives on Android
object SmartTextStyleMapper {

    fun paragraphTextStyle(
        block: RichBlock.Text,
        colors: SmartTextColors,
        baseSize: TextUnit
    ): TextStyle {
        val size = baseSize * block.style.relativeSize
        return TextStyle(
            color = if (block.style == ParagraphStyle.CAPTION) colors.onSurfaceMuted else colors.onSurface,
            fontSize = size,
            lineHeight = size * block.lineHeight,
            fontWeight = FontWeight(block.style.weight),
            fontStyle = if (block.style == ParagraphStyle.QUOTE) FontStyle.Italic else FontStyle.Normal,
            textAlign = alignOf(block.align),
            textIndent = if (block.firstLineIndent) TextIndent(firstLine = size) else null
        )
    }

    fun alignOf(align: TextAlign): ComposeTextAlign = when (align) {
        TextAlign.START -> ComposeTextAlign.Start
        TextAlign.CENTER -> ComposeTextAlign.Center
        TextAlign.END -> ComposeTextAlign.End
        TextAlign.JUSTIFY -> ComposeTextAlign.Justify
    }

    fun spanStyle(span: RichSpan, colors: SmartTextColors, baseSize: TextUnit): SpanStyle {
        val formats = span.formats
        val decorations = mutableListOf<TextDecoration>()
        if (formats.has(TextFormat.UNDERLINE)) decorations.add(TextDecoration.Underline)
        if (formats.has(TextFormat.STRIKETHROUGH)) decorations.add(TextDecoration.LineThrough)

        val baselineShift = when {
            formats.has(TextFormat.SUPERSCRIPT) -> BaselineShift.Superscript
            formats.has(TextFormat.SUBSCRIPT) -> BaselineShift.Subscript
            else -> BaselineShift.None
        }
        val scriptScale = if (baselineShift == BaselineShift.None) 1f else 0.75f

        return SpanStyle(
            color = span.textColor?.let(::parseColor)
                ?: if (span.link != null) colors.accent else Color.Unspecified,
            fontSize = span.fontSize?.sp?.times(scriptScale)
                ?: if (scriptScale != 1f) baseSize * scriptScale else TextUnit.Unspecified,
            fontWeight = span.fontWeight?.let { FontWeight(it) }
                ?: if (formats.has(TextFormat.BOLD)) FontWeight.Bold else null,
            fontStyle = if (formats.has(TextFormat.ITALIC)) FontStyle.Italic else null,
            fontFamily = if (formats.has(TextFormat.CODE)) FontFamily.Monospace else null,
            background = span.backgroundColor?.let(::parseColor)
                ?: when {
                    formats.has(TextFormat.HIGHLIGHT) -> colors.highlight
                    formats.has(TextFormat.CODE) -> colors.codeBackground
                    else -> Color.Unspecified
                },
            textDecoration = when {
                decorations.isEmpty() && span.link != null -> TextDecoration.Underline
                decorations.isEmpty() -> null
                else -> TextDecoration.combine(decorations)
            },
            baselineShift = baselineShift,
            letterSpacing = TextUnit.Unspecified
        )
    }

    fun annotate(
        text: String,
        spans: List<RichSpan>,
        colors: SmartTextColors,
        baseSize: TextUnit,
        highlightRanges: List<IntRange> = emptyList(),
        activeHighlight: IntRange? = null
    ): AnnotatedString = buildAnnotated(text) { builder ->
        spans.forEach { span ->
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (end > start) builder.addStyle(spanStyle(span, colors, baseSize), start, end)
        }
        highlightRanges.forEach { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            val background = if (range == activeHighlight) colors.searchHitActive else colors.searchHit
            if (end > start) builder.addStyle(SpanStyle(background = background), start, end)
        }
    }

    private inline fun buildAnnotated(
        text: String,
        block: (AnnotatedString.Builder) -> Unit
    ): AnnotatedString = AnnotatedString.Builder(text).apply(block).toAnnotatedString()

    fun parseColor(value: String): Color = runCatching {
        val hex = value.removePrefix("#")
        val long = hex.toLong(16)
        when (hex.length) {
            6 -> Color(0xFF000000 or long)
            8 -> Color(long)
            else -> Color.Unspecified
        }
    }.getOrDefault(Color.Unspecified)

    fun toHex(color: Color): String {
        val red = (color.red * 255).toInt()
        val green = (color.green * 255).toInt()
        val blue = (color.blue * 255).toInt()
        return "#%02X%02X%02X".format(red, green, blue)
    }
}
