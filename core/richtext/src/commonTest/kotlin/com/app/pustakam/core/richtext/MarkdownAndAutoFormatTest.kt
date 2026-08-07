package com.app.pustakam.core.richtext

import com.app.pustakam.core.richtext.engine.AutoFormatEngine
import com.app.pustakam.core.richtext.engine.MarkdownOutcome
import com.app.pustakam.core.richtext.engine.MarkdownShortcutEngine
import com.app.pustakam.core.richtext.engine.SpanEngine
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownAndAutoFormatTest {

    private fun rewrite(text: String): MarkdownOutcome.Rewrite? {
        val block = RichDocument.newTextBlock(text)
        return MarkdownShortcutEngine.detect(block, text.length) as? MarkdownOutcome.Rewrite
    }

    @Test
    fun hashesBecomeHeadings() {
        assertEquals(ParagraphStyle.HEADING_1, rewrite("# ")?.block?.style)
        assertEquals(ParagraphStyle.HEADING_2, rewrite("## ")?.block?.style)
        assertEquals(ParagraphStyle.HEADING_3, rewrite("### ")?.block?.style)
        assertEquals(ParagraphStyle.HEADING_6, rewrite("###### ")?.block?.style)
    }

    @Test
    fun headingTriggerRemovesTheMarkerAndKeepsTheRest() {
        val block = RichDocument.newTextBlock("# Title")
        val outcome = MarkdownShortcutEngine.detect(block, 2) as? MarkdownOutcome.Rewrite

        assertEquals("Title", outcome?.block?.text)
        assertEquals(0, outcome?.caret)
    }

    @Test
    fun dashBecomesBulletList() {
        assertEquals(ListStyle.BULLET, rewrite("- ")?.block?.list?.style)
    }

    @Test
    fun numberBecomesNumberedList() {
        val outcome = rewrite("1. ")
        assertEquals(ListStyle.NUMBERED, outcome?.block?.list?.style)
        assertEquals(1, outcome?.block?.list?.startNumber)
    }

    @Test
    fun bracketsBecomeChecklist() {
        assertEquals(ListStyle.CHECKLIST, rewrite("[] ")?.block?.list?.style)
    }

    @Test
    fun angleBecomesQuote() {
        assertEquals(ParagraphStyle.QUOTE, rewrite("> ")?.block?.style)
    }

    @Test
    fun tripleDashBecomesADivider() {
        val outcome = MarkdownShortcutEngine.detectStandalone(RichDocument.newTextBlock("---"))
        assertTrue(outcome is MarkdownOutcome.ReplaceWithDivider)
    }

    @Test
    fun tripleBacktickBecomesACodeBlock() {
        val outcome = MarkdownShortcutEngine.detectStandalone(RichDocument.newTextBlock("```"))
        assertTrue(outcome is MarkdownOutcome.ReplaceWithCode)
    }

    @Test
    fun doubleStarsBecomeBoldAndTheMarkersDisappear() {
        val outcome = rewrite("**bold**")

        assertEquals("bold", outcome?.block?.text)
        assertTrue(SpanEngine.isActive(outcome!!.block.spans, 0, 4, TextFormat.BOLD))
    }

    @Test
    fun singleStarsBecomeItalic() {
        val outcome = rewrite("*soft*")

        assertEquals("soft", outcome?.block?.text)
        assertTrue(SpanEngine.isActive(outcome!!.block.spans, 0, 4, TextFormat.ITALIC))
    }

    @Test
    fun backticksBecomeInlineCode() {
        val outcome = rewrite("`code`")

        assertEquals("code", outcome?.block?.text)
        assertTrue(SpanEngine.isActive(outcome!!.block.spans, 0, 4, TextFormat.CODE))
    }

    @Test
    fun anOpeningMarkerAloneDoesNothing() {
        assertNull(rewrite("**"))
        assertNull(rewrite("hello"))
    }

    @Test
    fun urlsAreDetected() {
        val links = AutoFormatEngine.detectLinks("see https://pustakam.app/docs for more")

        assertEquals(1, links.size)
        assertEquals("https://pustakam.app/docs", links.first().url)
    }

    @Test
    fun bareWwwGetsAScheme() {
        val links = AutoFormatEngine.detectLinks("visit www.example.com today")
        assertEquals("https://www.example.com", links.first().url)
    }

    @Test
    fun emailsBecomeMailtoLinks() {
        val links = AutoFormatEngine.detectLinks("write to hello@pustakam.app please")
        assertEquals("mailto:hello@pustakam.app", links.first().url)
    }

    @Test
    fun linkifyAppliesTheSpan() {
        val block = AutoFormatEngine.linkify(RichDocument.newTextBlock("go to https://a.com now"))
        assertEquals("https://a.com", SpanEngine.linkAt(block.spans, 8)?.link)
    }

    @Test
    fun smartQuotesOpenAndClose() {
        assertEquals('“', AutoFormatEngine.smartQuote("", 0, '"'))
        assertEquals('”', AutoFormatEngine.smartQuote("word", 4, '"'))
    }
}
