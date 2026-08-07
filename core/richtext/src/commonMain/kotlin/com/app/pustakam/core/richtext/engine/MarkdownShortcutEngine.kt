package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.ListMarker
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.TextFormat

/** What the widget must do after a markdown trigger was recognised. */
sealed class MarkdownOutcome {
    data class Rewrite(val block: RichBlock.Text, val caret: Int) : MarkdownOutcome()
    data class ReplaceWithDivider(val blockId: String) : MarkdownOutcome()
    data class ReplaceWithCode(val blockId: String) : MarkdownOutcome()
}

/** Converts markdown typed at the start of a block, and inline pairs as they are closed. */
object MarkdownShortcutEngine {

    fun detect(block: RichBlock.Text, caret: Int): MarkdownOutcome? =
        detectBlockPrefix(block, caret) ?: detectInlinePair(block, caret)

    private fun detectBlockPrefix(block: RichBlock.Text, caret: Int): MarkdownOutcome? {
        val prefix = block.text.take(caret)
        if (!prefix.endsWith(" ")) return null
        val token = prefix.trimEnd()
        if (token.isEmpty() || token.length > 7) return null
        if (prefix.length != token.length + 1) return null

        val rest = block.text.substring(caret)
        val restSpans = SpanEngine.slice(block.spans, caret, block.text.length)

        val hashes = token.takeWhile { it == '#' }.length
        if (hashes in 1..6 && token.length == hashes) {
            return MarkdownOutcome.Rewrite(
                block.copy(
                    text = rest,
                    spans = restSpans,
                    style = ParagraphStyle.forMarkdownHashes(hashes),
                    list = null
                ),
                caret = 0
            )
        }

        val listStyle = when {
            token == "-" || token == "*" || token == "+" -> ListStyle.BULLET
            token == "[]" || token == "[ ]" || token == "-[]" -> ListStyle.CHECKLIST
            token.endsWith(".") && token.dropLast(1).all { it.isDigit() } -> ListStyle.NUMBERED
            token.endsWith(")") && token.dropLast(1).all { it.isDigit() } -> ListStyle.NUMBERED
            else -> null
        }
        if (listStyle != null) {
            val startNumber = token.dropLast(1).toIntOrNull() ?: 1
            return MarkdownOutcome.Rewrite(
                block.copy(
                    text = rest,
                    spans = restSpans,
                    style = ParagraphStyle.PARAGRAPH,
                    list = ListMarker(
                        style = listStyle,
                        level = block.list?.level ?: 0,
                        restart = listStyle == ListStyle.NUMBERED && startNumber != 1,
                        startNumber = startNumber
                    )
                ),
                caret = 0
            )
        }

        if (token == ">") {
            return MarkdownOutcome.Rewrite(
                block.copy(
                    text = rest,
                    spans = restSpans,
                    style = ParagraphStyle.QUOTE,
                    list = null
                ),
                caret = 0
            )
        }
        return null
    }

    /** `---` and ``` fire without a trailing space, so they are checked on every keystroke. */
    fun detectStandalone(block: RichBlock.Text): MarkdownOutcome? = when (block.text) {
        "---", "***", "___" -> MarkdownOutcome.ReplaceWithDivider(block.id)
        "```" -> MarkdownOutcome.ReplaceWithCode(block.id)
        else -> null
    }

    // hand-rolled instead of regex lookaround, which Kotlin/Native does not support
    private fun detectInlinePair(block: RichBlock.Text, caret: Int): MarkdownOutcome? {
        val text = block.text.take(caret)
        if (text.endsWith("**")) {
            val open = text.dropLast(2).lastIndexOf("**")
            if (open >= 0 && caret - 2 > open + 2) {
                return rewriteInline(block, open, caret, 2, TextFormat.BOLD)
            }
            return null
        }
        if (text.endsWith("*")) {
            val open = text.dropLast(1).lastIndexOf('*')
            if (open >= 0 && caret - 1 > open + 1) {
                return rewriteInline(block, open, caret, 1, TextFormat.ITALIC)
            }
            return null
        }
        if (text.endsWith("`")) {
            val open = text.dropLast(1).lastIndexOf('`')
            if (open >= 0 && caret - 1 > open + 1) {
                return rewriteInline(block, open, caret, 1, TextFormat.CODE)
            }
        }
        return null
    }

    private fun rewriteInline(
        block: RichBlock.Text,
        openAt: Int,
        caret: Int,
        markerLength: Int,
        format: TextFormat
    ): MarkdownOutcome {
        val innerStart = openAt + markerLength
        val innerEnd = caret - markerLength
        if (innerEnd <= innerStart) return MarkdownOutcome.Rewrite(block, caret)

        val newText = block.text.substring(0, openAt) +
                block.text.substring(innerStart, innerEnd) +
                block.text.substring(caret)

        val afterClose = SpanEngine.afterDelete(block.spans, innerEnd, caret)
        val afterOpen = SpanEngine.afterDelete(afterClose, openAt, innerStart)
        val styled = SpanEngine.setFormat(
            spans = afterOpen,
            start = openAt,
            end = openAt + (innerEnd - innerStart),
            textLength = newText.length,
            format = format,
            enabled = true
        )
        return MarkdownOutcome.Rewrite(
            block.copy(text = newText, spans = styled),
            caret = openAt + (innerEnd - innerStart)
        )
    }
}
