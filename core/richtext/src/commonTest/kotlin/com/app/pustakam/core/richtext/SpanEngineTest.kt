package com.app.pustakam.core.richtext

import com.app.pustakam.core.richtext.engine.SpanEngine
import com.app.pustakam.core.richtext.model.FormatSet
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpanEngineTest {

    private val text = "Hello brave new world"

    @Test
    fun overlappingFormatsCoexistOnOneSpan() {
        var spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.BOLD, true)
        spans = SpanEngine.setFormat(spans, 0, 5, text.length, TextFormat.ITALIC, true)
        spans = SpanEngine.setFormat(spans, 0, 5, text.length, TextFormat.HIGHLIGHT, true)

        val formats = SpanEngine.commonFormats(spans, 0, 5)
        assertTrue(formats.has(TextFormat.BOLD))
        assertTrue(formats.has(TextFormat.ITALIC))
        assertTrue(formats.has(TextFormat.HIGHLIGHT))
    }

    @Test
    fun toggleRemovesOnlyTheRequestedFormat() {
        var spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.BOLD, true)
        spans = SpanEngine.setFormat(spans, 0, 5, text.length, TextFormat.ITALIC, true)
        spans = SpanEngine.toggleFormat(spans, 0, 5, text.length, TextFormat.BOLD)

        assertFalse(SpanEngine.isActive(spans, 0, 5, TextFormat.BOLD))
        assertTrue(SpanEngine.isActive(spans, 0, 5, TextFormat.ITALIC))
    }

    @Test
    fun partialSelectionSplitsTheSpan() {
        val bold = SpanEngine.setFormat(emptyList(), 0, 11, text.length, TextFormat.BOLD, true)
        val partial = SpanEngine.setFormat(bold, 6, 11, text.length, TextFormat.BOLD, false)

        assertTrue(SpanEngine.isActive(partial, 0, 5, TextFormat.BOLD))
        assertFalse(SpanEngine.isActive(partial, 6, 11, TextFormat.BOLD))
    }

    @Test
    fun commonFormatsIsEmptyWhenTheRangeIsOnlyPartlyStyled() {
        val spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.BOLD, true)
        assertEquals(FormatSet.EMPTY, SpanEngine.commonFormats(spans, 0, 11))
    }

    @Test
    fun adjacentEqualSpansMerge() {
        val spans = SpanEngine.normalize(
            listOf(
                RichSpan(0, 5, FormatSet.of(TextFormat.BOLD)),
                RichSpan(5, 11, FormatSet.of(TextFormat.BOLD))
            ),
            text.length
        )
        assertEquals(1, spans.size)
        assertEquals(0, spans.first().start)
        assertEquals(11, spans.first().end)
    }

    @Test
    fun unstyledSpansAreDropped() {
        val spans = SpanEngine.normalize(listOf(RichSpan(0, 5), RichSpan(6, 6)), text.length)
        assertTrue(spans.isEmpty())
    }

    @Test
    fun superscriptAndSubscriptAreMutuallyExclusive() {
        var spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.SUPERSCRIPT, true)
        spans = SpanEngine.setFormat(spans, 0, 5, text.length, TextFormat.SUBSCRIPT, true)

        assertFalse(SpanEngine.isActive(spans, 0, 5, TextFormat.SUPERSCRIPT))
        assertTrue(SpanEngine.isActive(spans, 0, 5, TextFormat.SUBSCRIPT))
    }

    @Test
    fun insertInsideAStyledRunExtendsIt() {
        val spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.BOLD, true)
        val shifted = SpanEngine.afterInsert(spans, 3, 4)

        assertEquals(0, shifted.first().start)
        assertEquals(9, shifted.first().end)
    }

    @Test
    fun insertAfterAStyledRunInheritsIt() {
        val spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.BOLD, true)
        val shifted = SpanEngine.afterInsert(spans, 5, 3)

        assertEquals(8, shifted.first().end)
    }

    @Test
    fun deleteShrinksAndDropsSpans() {
        var spans = SpanEngine.setFormat(emptyList(), 0, 5, text.length, TextFormat.BOLD, true)
        spans = SpanEngine.setFormat(spans, 6, 11, text.length, TextFormat.ITALIC, true)
        val afterDelete = SpanEngine.afterDelete(spans, 0, 6)

        assertEquals(1, afterDelete.size)
        assertEquals(0, afterDelete.first().start)
        assertEquals(5, afterDelete.first().end)
    }

    @Test
    fun clearFormattingKeepsStylingOutsideTheRange() {
        val spans = SpanEngine.setFormat(emptyList(), 0, 11, text.length, TextFormat.BOLD, true)
        val cleared = SpanEngine.clearFormatting(spans, 0, 5, text.length)

        assertFalse(SpanEngine.isActive(cleared, 0, 5, TextFormat.BOLD))
        assertTrue(SpanEngine.isActive(cleared, 5, 11, TextFormat.BOLD))
    }

    @Test
    fun sliceRebasesToZero() {
        val spans = SpanEngine.setFormat(emptyList(), 6, 11, text.length, TextFormat.BOLD, true)
        val sliced = SpanEngine.slice(spans, 6, 11)

        assertEquals(0, sliced.first().start)
        assertEquals(5, sliced.first().end)
    }

    @Test
    fun linkIsFoundAtItsOffsetOnly() {
        val spans = SpanEngine.setLink(emptyList(), 0, 5, text.length, "https://example.com")

        assertEquals("https://example.com", SpanEngine.linkAt(spans, 2)?.link)
        assertNull(SpanEngine.linkAt(spans, 9))
    }
}
