package com.app.pustakam.core.richtext

import com.app.pustakam.core.model.models.RichTextMetadata
import com.app.pustakam.core.model.models.TextSpan
import com.app.pustakam.core.model.models.TextStyleType
import com.app.pustakam.core.richtext.codec.RichTextCodec
import com.app.pustakam.core.richtext.engine.SpanEngine
import com.app.pustakam.core.richtext.engine.TableEngine
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.TableData
import com.app.pustakam.core.richtext.model.TextFormat
import com.app.pustakam.core.richtext.presentation.SmartTextIntent
import com.app.pustakam.core.richtext.presentation.SmartTextReducer
import com.app.pustakam.core.richtext.presentation.SmartTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RichTextCodecTest {

    @Test
    fun roundTripPreservesEverything() {
        var state = SmartTextState.of(RichDocument.fromPlainText("Heading\nbody text"))
        state = SmartTextReducer.reduce(
            state, SmartTextIntent.SetParagraphStyle(ParagraphStyle.HEADING_1)
        )
        state = SmartTextReducer.reduce(state, SmartTextIntent.InsertTable(2, 2))

        val encoded = RichTextCodec.encode(state.document)
        val decoded = RichTextCodec.decode(encoded)

        assertEquals(state.document, decoded)
    }

    @Test
    fun overlappingFormatsSurviveTheRoundTrip() {
        val block = RichDocument.newTextBlock("styled")
        var spans = SpanEngine.setFormat(emptyList(), 0, 6, 6, TextFormat.BOLD, true)
        spans = SpanEngine.setFormat(spans, 0, 6, 6, TextFormat.ITALIC, true)
        spans = SpanEngine.setFormat(spans, 0, 6, 6, TextFormat.HIGHLIGHT, true)
        val document = RichDocument(listOf(block.copy(spans = spans)))

        val decoded = RichTextCodec.decode(RichTextCodec.encode(document))!!
        val restored = (decoded.blocks.first() as RichBlock.Text).spans

        assertTrue(SpanEngine.isActive(restored, 0, 6, TextFormat.BOLD))
        assertTrue(SpanEngine.isActive(restored, 0, 6, TextFormat.ITALIC))
        assertTrue(SpanEngine.isActive(restored, 0, 6, TextFormat.HIGHLIGHT))
    }

    @Test
    fun plainTextWithNoMetadataBecomesParagraphs() {
        val document = RichTextCodec.documentFrom("one\ntwo\nthree", null)

        assertEquals(3, document.blocks.size)
        assertEquals("one\ntwo\nthree", document.plainText)
    }

    @Test
    fun legacyMetadataStillOpens() {
        val legacy = RichTextMetadata(
            spans = listOf(TextSpan(start = 0, end = 5, style = TextStyleType.BOLD))
        )
        val document = RichTextCodec.documentFrom("Hello world", legacy)
        val spans = (document.blocks.first() as RichBlock.Text).spans

        assertTrue(SpanEngine.isActive(spans, 0, 5, TextFormat.BOLD))
    }

    @Test
    fun legacySpansAreWrittenBackForOldReaders() {
        val spans = SpanEngine.setFormat(emptyList(), 0, 5, 11, TextFormat.BOLD, true)
        val document = RichDocument(listOf(RichDocument.newTextBlock("Hello world", spans)))
        val metadata = RichTextCodec.metadataFrom(document)

        assertEquals(1, metadata.spans.size)
        assertEquals(TextStyleType.BOLD, metadata.spans.first().style)
        assertNotNull(metadata.document)
    }

    @Test
    fun theNewPayloadWinsOverLegacySpans() {
        val document = RichDocument(
            listOf(RichDocument.newTextBlock("Hello world", list = null, style = ParagraphStyle.HEADING_2))
        )
        val metadata = RichTextCodec.metadataFrom(document).copy(
            spans = listOf(TextSpan(start = 0, end = 5, style = TextStyleType.ITALIC))
        )
        val restored = RichTextCodec.documentFrom("Hello world", metadata)

        assertEquals(ParagraphStyle.HEADING_2, (restored.blocks.first() as RichBlock.Text).style)
    }

    @Test
    fun corruptPayloadFallsBackInsteadOfThrowing() {
        val document = RichTextCodec.documentFrom(
            "Hello", RichTextMetadata(document = "{ not json")
        )
        assertEquals("Hello", document.plainText)
        assertNull(RichTextCodec.decode("{ not json"))
    }

    @Test
    fun plainTextProjectionStaysInSync() {
        var state = SmartTextState.of(RichDocument.fromPlainText("shopping"))
        state = SmartTextReducer.reduce(state, SmartTextIntent.ToggleList(ListStyle.BULLET))

        assertEquals("shopping", state.document.plainText)
    }

    @Test
    fun tableStructureSurvivesEncoding() {
        var table = TableData.empty(2, 2)
        table = TableEngine.addColumn(table)
        table = TableEngine.updateCell(table, 0, 0) { it.copy(text = "header") }
        val document = RichDocument(listOf(RichBlock.Table("t1", table)))

        val decoded = RichTextCodec.decode(RichTextCodec.encode(document))!!
        val restored = (decoded.blocks.first() as RichBlock.Table).data

        assertEquals(3, restored.columnCount)
        assertEquals("header", restored.cellAt(0, 0)?.text)
    }
}
