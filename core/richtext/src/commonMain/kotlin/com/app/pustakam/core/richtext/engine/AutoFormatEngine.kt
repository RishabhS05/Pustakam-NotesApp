package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichSpan

/** A detected clickable run of text. */
data class DetectedLink(val start: Int, val end: Int, val url: String)

/** URL / e-mail / phone detection plus the typographic niceties applied while typing. */
object AutoFormatEngine {

    fun detectLinks(text: String): List<DetectedLink> {
        val found = mutableListOf<DetectedLink>()
        urlPattern.findAll(text).forEach { match ->
            val raw = match.value.trimEnd('.', ',', ')', ']', ';', ':')
            val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
            found.add(DetectedLink(match.range.first, match.range.first + raw.length, url))
        }
        emailPattern.findAll(text).forEach { match ->
            if (found.none { it.start <= match.range.first && match.range.first < it.end }) {
                found.add(DetectedLink(match.range.first, match.range.last + 1, "mailto:${match.value}"))
            }
        }
        phonePattern.findAll(text).forEach { match ->
            val value = match.value.trim()
            val start = match.range.first + (match.value.length - match.value.trimStart().length)
            if (found.none { it.start <= start && start < it.end }) {
                found.add(DetectedLink(start, start + value.length, "tel:${value.filter { it.isDigit() || it == '+' }}"))
            }
        }
        return found.sortedBy { it.start }
    }

    /** Applies detected links without touching links the user set by hand. */
    fun linkify(block: RichBlock.Text): RichBlock.Text {
        val detected = detectLinks(block.text)
        if (detected.isEmpty()) return block
        var spans: List<RichSpan> = block.spans
        detected.forEach { link ->
            val alreadyLinked = spans.any { it.link != null && it.intersects(link.start, link.end) }
            if (!alreadyLinked) {
                spans = SpanEngine.setLink(spans, link.start, link.end, block.text.length, link.url)
            }
        }
        return block.copy(spans = spans)
    }

    /** Straight quotes become typographic ones based on the character to their left. */
    fun smartQuote(text: String, insertedAt: Int, inserted: Char): Char? {
        val previous = text.getOrNull(insertedAt - 1)
        val opening = previous == null || previous.isWhitespace() || previous in "([{“‘"
        return when (inserted) {
            '"' -> if (opening) '“' else '”'
            '\'' -> if (opening) '‘' else '’'
            else -> null
        }
    }

    /** Pairs that auto-close as the opener is typed. */
    fun closingFor(opener: Char): Char? = when (opener) {
        '(' -> ')'
        '[' -> ']'
        '{' -> '}'
        '`' -> '`'
        else -> null
    }

    private val urlPattern = Regex(
        """(?:https?://|www\.)[\w\-]+(?:\.[\w\-]+)+(?:[/#?][^\s]*)?""",
        RegexOption.IGNORE_CASE
    )
    private val emailPattern = Regex("""[\w.+\-]+@[\w\-]+(?:\.[\w\-]+)+""")
    private val phonePattern = Regex("""(?:\+\d{1,3}[ \-]?)?(?:\d[ \-]?){7,13}\d""")
}
