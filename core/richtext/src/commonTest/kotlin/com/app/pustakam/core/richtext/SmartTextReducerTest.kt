package com.app.pustakam.core.richtext

import com.app.pustakam.core.richtext.engine.SpanEngine
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.TextFormat
import com.app.pustakam.core.richtext.presentation.DocumentSelection
import com.app.pustakam.core.richtext.presentation.SmartTextIntent
import com.app.pustakam.core.richtext.presentation.SmartTextReducer
import com.app.pustakam.core.richtext.presentation.SmartTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SmartTextReducerTest {

    private fun stateWith(text: String): SmartTextState {
        val document = RichDocument.fromPlainText(text)
        val first = document.blocks.first()
        return SmartTextReducer.withRefreshedToolbar(
            SmartTextState(document = document, selection = DocumentSelection.caret(first.id, 0))
        )
    }

    private fun SmartTextState.select(start: Int, end: Int): SmartTextState =
        SmartTextReducer.reduce(
            this,
            SmartTextIntent.SelectionChanged(
                DocumentSelection(focusedBlockId, start, focusedBlockId, end)
            )
        )

    private fun SmartTextState.firstText(): RichBlock.Text = document.blocks.first() as RichBlock.Text

    @Test
    fun boldAppliesToTheSelectionOnly() {
        val state = stateWith("Hello world")
            .select(0, 5)
            .let { SmartTextReducer.reduce(it, SmartTextIntent.ToggleFormat(TextFormat.BOLD)) }

        val spans = state.firstText().spans
        assertTrue(SpanEngine.isActive(spans, 0, 5, TextFormat.BOLD))
        assertFalse(SpanEngine.isActive(spans, 6, 11, TextFormat.BOLD))
    }

    @Test
    fun toolbarReportsTheActiveFormat() {
        val state = stateWith("Hello world")
            .select(0, 5)
            .let { SmartTextReducer.reduce(it, SmartTextIntent.ToggleFormat(TextFormat.BOLD)) }

        assertTrue(state.toolbar.activeFormats.has(TextFormat.BOLD))
    }

    @Test
    fun toggleWithNoSelectionArmsTheNextKeystroke() {
        var state = stateWith("")
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        assertTrue(state.toolbar.activeFormats.has(TextFormat.BOLD))

        state = SmartTextReducer.reduce(
            state, SmartTextIntent.TypeText(state.focusedBlockId, "hi", 2)
        )
        assertTrue(SpanEngine.isActive(state.firstText().spans, 0, 2, TextFormat.BOLD))
    }

    @Test
    fun colourPickedWithNoSelectionAppliesToTheNextKeystroke() {
        var state = stateWith("")
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetTextColor("#E9A33C"))
        assertEquals("#E9A33C", state.toolbar.textColor)

        state = SmartTextReducer.reduce(
            state, SmartTextIntent.TypeText(state.focusedBlockId, "hi", 2)
        )
        assertEquals("#E9A33C", state.firstText().spans.first().textColor)
    }

    @Test
    fun colourAppliesToTheSelectionWhenThereIsOne() {
        val state = stateWith("Hello world")
            .select(0, 5)
            .let { SmartTextReducer.reduce(it, SmartTextIntent.SetTextColor("#C0392B")) }

        val span = state.firstText().spans.first()
        assertEquals("#C0392B", span.textColor)
        assertEquals(0, span.start)
        assertEquals(5, span.end)
    }

    @Test
    fun fontSizePickedWithNoSelectionArmsTheNextKeystroke() {
        var state = stateWith("")
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetFontSize(24f))
        state = SmartTextReducer.reduce(
            state, SmartTextIntent.TypeText(state.focusedBlockId, "big", 3)
        )
        assertEquals(24f, state.firstText().spans.first().fontSize)
    }

    @Test
    fun movingTheCaretDisarmsAPendingToggle() {
        var state = stateWith("plain")
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        state = state.select(2, 2)

        assertFalse(state.toolbar.activeFormats.has(TextFormat.BOLD))
    }

    @Test
    fun paragraphStyleAppliesToTheFocusedBlock() {
        val state = SmartTextReducer.reduce(
            stateWith("Title"),
            SmartTextIntent.SetParagraphStyle(ParagraphStyle.HEADING_1)
        )
        assertEquals(ParagraphStyle.HEADING_1, state.firstText().style)
    }

    @Test
    fun typingRebasesExistingSpans() {
        var state = stateWith("Hello world").select(6, 11)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        state = SmartTextReducer.reduce(
            state,
            SmartTextIntent.TypeText(state.focusedBlockId, "Say Hello world", 4)
        )

        assertTrue(SpanEngine.isActive(state.firstText().spans, 10, 15, TextFormat.BOLD))
    }

    @Test
    fun enterSplitsTheBlockAndCarriesTheListMarker() {
        var state = stateWith("first")
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleList(ListStyle.BULLET))
        state = SmartTextReducer.reduce(state, SmartTextIntent.SplitBlock(state.focusedBlockId, 5))

        assertEquals(2, state.document.blocks.size)
        assertEquals(ListStyle.BULLET, (state.document.blocks[1] as RichBlock.Text).list?.style)
    }

    @Test
    fun enterOnAnEmptyListItemLeavesTheList() {
        var state = stateWith("")
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleList(ListStyle.BULLET))
        state = SmartTextReducer.reduce(state, SmartTextIntent.SplitBlock(state.focusedBlockId, 0))

        assertEquals(1, state.document.blocks.size)
        assertEquals(null, state.firstText().list)
    }

    @Test
    fun headingDoesNotLeakOntoTheNextParagraph() {
        var state = stateWith("Title")
        state = SmartTextReducer.reduce(
            state, SmartTextIntent.SetParagraphStyle(ParagraphStyle.HEADING_1)
        )
        state = SmartTextReducer.reduce(state, SmartTextIntent.SplitBlock(state.focusedBlockId, 5))

        assertEquals(ParagraphStyle.PARAGRAPH, (state.document.blocks[1] as RichBlock.Text).style)
    }

    @Test
    fun backspaceAtStartMergesWithThePreviousBlock() {
        var state = stateWith("one\ntwo")
        val second = state.document.blocks[1]
        state = SmartTextReducer.reduce(
            state, SmartTextIntent.SelectionChanged(DocumentSelection.caret(second.id, 0))
        )
        state = SmartTextReducer.reduce(state, SmartTextIntent.MergeWithPrevious(second.id))

        assertEquals(1, state.document.blocks.size)
        assertEquals("onetwo", state.firstText().text)
    }

    @Test
    fun undoRestoresTheDocumentAndRedoReappliesIt() {
        val original = stateWith("Hello world")
        val bolded = original
            .select(0, 5)
            .let { SmartTextReducer.reduce(it, SmartTextIntent.ToggleFormat(TextFormat.BOLD)) }
        assertNotEquals(original.document, bolded.document)

        val undone = SmartTextReducer.reduce(bolded, SmartTextIntent.Undo)
        assertEquals(original.document, undone.document)

        val redone = SmartTextReducer.reduce(undone, SmartTextIntent.Redo)
        assertEquals(bolded.document, redone.document)
    }

    @Test
    fun undoDoesNothingOnAFreshDocument() {
        val state = stateWith("Hello")
        assertFalse(state.canUndo)
        assertEquals(state.document, SmartTextReducer.reduce(state, SmartTextIntent.Undo).document)
    }

    @Test
    fun clearFormattingResetsSpansAndBlockStyle() {
        var state = stateWith("Hello world").select(0, 11)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetParagraphStyle(ParagraphStyle.HEADING_2))
        state = state.select(0, 11)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ClearFormatting)

        assertTrue(state.firstText().spans.isEmpty())
        assertEquals(ParagraphStyle.PARAGRAPH, state.firstText().style)
    }

    @Test
    fun dividerInsertsABlockAndAParagraphAfterIt() {
        val state = SmartTextReducer.reduce(stateWith("text"), SmartTextIntent.InsertDivider)

        assertTrue(state.document.blocks[1] is RichBlock.Divider)
        assertTrue(state.document.blocks[2] is RichBlock.Text)
    }

    @Test
    fun tableInsertsWithTheRequestedShape() {
        val state = SmartTextReducer.reduce(stateWith("text"), SmartTextIntent.InsertTable(3, 4))
        val table = state.document.blocks[1] as RichBlock.Table

        assertEquals(3, table.data.rowCount)
        assertEquals(4, table.data.columnCount)
    }

    @Test
    fun copyThenPasteKeepsFormatting() {
        var state = stateWith("Hello world").select(0, 5)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        state = state.select(0, 5)
        state = SmartTextReducer.reduce(state, SmartTextIntent.Copy)
        state = SmartTextReducer.reduce(
            state, SmartTextIntent.SelectionChanged(DocumentSelection.caret(state.focusedBlockId, 11))
        )
        state = SmartTextReducer.reduce(state, SmartTextIntent.Paste("Hello", keepFormatting = true))

        assertEquals("Hello worldHello", state.firstText().text)
        assertTrue(SpanEngine.isActive(state.firstText().spans, 11, 16, TextFormat.BOLD))
    }

    @Test
    fun pasteWithoutFormattingDropsTheSpans() {
        var state = stateWith("Hello world").select(0, 5)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        state = state.select(0, 5)
        state = SmartTextReducer.reduce(state, SmartTextIntent.Copy)
        state = SmartTextReducer.reduce(
            state, SmartTextIntent.SelectionChanged(DocumentSelection.caret(state.focusedBlockId, 11))
        )
        state = SmartTextReducer.reduce(state, SmartTextIntent.Paste("Hello", keepFormatting = false))

        assertFalse(SpanEngine.isActive(state.firstText().spans, 11, 16, TextFormat.BOLD))
    }

    @Test
    fun cutRemovesTheSelectedText() {
        var state = stateWith("Hello world").select(0, 6)
        state = SmartTextReducer.reduce(state, SmartTextIntent.Cut)

        assertEquals("world", state.firstText().text)
        assertEquals("Hello ", state.clipboard?.text)
    }

    @Test
    fun selectWordExpandsToWordBoundaries() {
        val state = SmartTextReducer.reduce(
            stateWith("Hello brave world"),
            SmartTextIntent.SelectWord(stateWith("Hello brave world").focusedBlockId, 8)
        )
        assertEquals(6, state.selection.normalizedStart)
        assertEquals(11, state.selection.normalizedEnd)
    }

    @Test
    fun selectAllSpansEveryBlock() {
        val state = SmartTextReducer.reduce(stateWith("one\ntwo\nthree"), SmartTextIntent.SelectAll)

        assertEquals(state.document.blocks.first().id, state.selection.startBlockId)
        assertEquals(state.document.blocks.last().id, state.selection.endBlockId)
        assertEquals(5, state.selection.endOffset)
    }

    @Test
    fun formattingSpansMultipleBlocks() {
        var state = stateWith("one\ntwo")
        state = SmartTextReducer.reduce(state, SmartTextIntent.SelectAll)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.ITALIC))

        val first = state.document.blocks[0] as RichBlock.Text
        val second = state.document.blocks[1] as RichBlock.Text
        assertTrue(SpanEngine.isActive(first.spans, 0, 3, TextFormat.ITALIC))
        assertTrue(SpanEngine.isActive(second.spans, 0, 3, TextFormat.ITALIC))
    }

    @Test
    fun findLocatesEveryOccurrence() {
        val state = SmartTextReducer.reduce(
            stateWith("cat\ncatalogue\ndog"),
            SmartTextIntent.SetSearchQuery("cat")
        )
        assertEquals(2, state.search.matches.size)
        assertEquals(0, state.search.currentIndex)
    }

    @Test
    fun replaceAllRewritesEveryMatch() {
        var state = SmartTextReducer.reduce(
            stateWith("cat and cat"), SmartTextIntent.SetSearchQuery("cat")
        )
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetReplacement("dog"))
        state = SmartTextReducer.reduce(state, SmartTextIntent.ReplaceAll)

        assertEquals("dog and dog", state.firstText().text)
    }

    @Test
    fun replaceKeepsSurroundingFormatting() {
        var state = stateWith("keep cat").select(0, 4)
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleFormat(TextFormat.BOLD))
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetSearchQuery("cat"))
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetReplacement("dog"))
        state = SmartTextReducer.reduce(state, SmartTextIntent.ReplaceAll)

        assertEquals("keep dog", state.firstText().text)
        assertTrue(SpanEngine.isActive(state.firstText().spans, 0, 4, TextFormat.BOLD))
    }

    @Test
    fun indentAndOutdentGoThroughTheReducer() {
        var state = stateWith("item")
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleList(ListStyle.BULLET))
        state = SmartTextReducer.reduce(state, SmartTextIntent.Indent)
        assertEquals(1, state.firstText().list?.level)

        state = SmartTextReducer.reduce(state, SmartTextIntent.Outdent)
        assertEquals(0, state.firstText().list?.level)
    }

    @Test
    fun quoteTogglesBothWays() {
        var state = SmartTextReducer.reduce(stateWith("quoted"), SmartTextIntent.ToggleQuote)
        assertEquals(ParagraphStyle.QUOTE, state.firstText().style)

        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleQuote)
        assertEquals(ParagraphStyle.PARAGRAPH, state.firstText().style)
    }

    @Test
    fun linkIsSetThenRemoved() {
        var state = stateWith("Pustakam site").select(0, 8)
        state = SmartTextReducer.reduce(state, SmartTextIntent.SetLink("https://pustakam.app"))
        assertEquals("https://pustakam.app", state.toolbar.link)

        state = SmartTextReducer.reduce(state, SmartTextIntent.RemoveLink)
        assertEquals(null, state.toolbar.link)
    }
}
