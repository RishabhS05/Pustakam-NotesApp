package com.app.pustakam.core.richtext.codec

import com.app.pustakam.core.model.models.RichTextMetadata
import com.app.pustakam.core.model.models.TextColorType
import com.app.pustakam.core.model.models.TextSpan
import com.app.pustakam.core.model.models.TextStyleType
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.richtext.model.FormatSet
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bridges [RichDocument] and the persisted TextContent.
 *
 * Reading order: new document payload → legacy [RichTextMetadata.spans] → plain text.
 * Writing keeps `text` and the legacy span list populated so every existing reader
 * (export, search, the book reader) keeps working untouched.
 */
object RichTextCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        classDiscriminator = "kind"
    }

    fun encode(document: RichDocument): String = json.encodeToString(document)

    fun decode(payload: String?): RichDocument? {
        if (payload.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<RichDocument>(payload) }.getOrNull()
    }

    fun documentFrom(text: String, metadata: RichTextMetadata?): RichDocument {
        decode(metadata?.document)?.let { decoded ->
            return if (decoded.blocks.isEmpty()) RichDocument.fromPlainText(text) else decoded
        }
        val legacy = metadata?.spans.orEmpty()
        if (legacy.isEmpty()) return RichDocument.fromPlainText(text)
        return LegacyMetadataMapper.toDocument(text, legacy)
    }

    fun documentFrom(content: NoteContentModel.TextContent): RichDocument =
        documentFrom(content.text, content.metadata)

    fun metadataFrom(document: RichDocument): RichTextMetadata = RichTextMetadata(
        spans = LegacyMetadataMapper.toLegacySpans(document),
        document = encode(document)
    )

    /** The only write path — keeps text and metadata in sync on one call. */
    fun applyTo(
        content: NoteContentModel.TextContent,
        document: RichDocument
    ): NoteContentModel.TextContent =
        content.withText(document.plainText).copy(metadata = metadataFrom(document))
}

/** Reads and writes the pre-SmartText single-style span format. */
internal object LegacyMetadataMapper {

    fun toDocument(text: String, spans: List<TextSpan>): RichDocument {
        val document = RichDocument.fromPlainText(text)
        if (document.blocks.size != 1) return applyAcrossBlocks(document, spans)
        val block = document.blocks.first() as? RichBlock.Text ?: return document
        return document.replacingBlock(block.copy(spans = convert(spans, block.text.length)))
    }

    fun toLegacySpans(document: RichDocument): List<TextSpan> {
        var offset = 0
        val legacy = mutableListOf<TextSpan>()
        document.blocks.forEach { block ->
            if (block is RichBlock.Text) {
                block.spans.forEach { span ->
                    legacy.add(
                        TextSpan(
                            start = span.start + offset,
                            end = span.end + offset,
                            color = span.textColor.orEmpty(),
                            style = primaryStyle(span),
                            weight = span.fontWeight ?: DEFAULT_WEIGHT,
                            colorType = TextColorType.DEFAULT
                        )
                    )
                }
            }
            offset += block.plainText.length + 1
        }
        return legacy
    }

    private fun applyAcrossBlocks(document: RichDocument, spans: List<TextSpan>): RichDocument {
        var offset = 0
        var result = document
        document.blocks.forEach { block ->
            if (block is RichBlock.Text) {
                val length = block.text.length
                val local = spans.mapNotNull { span ->
                    val start = span.start - offset
                    val end = span.end - offset
                    if (end <= 0 || start >= length) null
                    else TextSpan(
                        start = start.coerceAtLeast(0),
                        end = end.coerceAtMost(length),
                        color = span.color,
                        style = span.style,
                        weight = span.weight,
                        colorType = span.colorType
                    )
                }
                if (local.isNotEmpty()) {
                    result = result.replacingBlock(block.copy(spans = convert(local, length)))
                }
                offset += length + 1
            } else {
                offset += block.plainText.length + 1
            }
        }
        return result
    }

    private fun convert(spans: List<TextSpan>, textLength: Int): List<RichSpan> = spans.mapNotNull {
        val format = format(it.style)
        RichSpan(
            start = it.start,
            end = it.end,
            formats = format?.let { found -> FormatSet.of(found) } ?: FormatSet.EMPTY,
            textColor = it.color?.takeIf { color -> color.isNotBlank() },
            fontWeight = it.weight.takeIf { weight -> weight != DEFAULT_WEIGHT }
        ).clipped(0, textLength)
    }

    private fun format(style: TextStyleType): TextFormat? = when (style) {
        TextStyleType.NORMAL -> null
        TextStyleType.BOLD -> TextFormat.BOLD
        TextStyleType.ITALIC -> TextFormat.ITALIC
        TextStyleType.UNDERLINE -> TextFormat.UNDERLINE
        TextStyleType.STRIKETHROUGH -> TextFormat.STRIKETHROUGH
        TextStyleType.HIGHLIGHT -> TextFormat.HIGHLIGHT
    }

    private fun primaryStyle(span: RichSpan): TextStyleType = when {
        span.formats.has(TextFormat.BOLD) -> TextStyleType.BOLD
        span.formats.has(TextFormat.ITALIC) -> TextStyleType.ITALIC
        span.formats.has(TextFormat.UNDERLINE) -> TextStyleType.UNDERLINE
        span.formats.has(TextFormat.STRIKETHROUGH) -> TextStyleType.STRIKETHROUGH
        span.formats.has(TextFormat.HIGHLIGHT) -> TextStyleType.HIGHLIGHT
        else -> TextStyleType.NORMAL
    }

    private const val DEFAULT_WEIGHT = 400
}
