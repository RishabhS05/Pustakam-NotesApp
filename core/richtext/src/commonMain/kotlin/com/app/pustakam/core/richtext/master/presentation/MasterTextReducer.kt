package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.engine.AutoFormatEngine
import com.app.pustakam.core.richtext.engine.ListEngine
import com.app.pustakam.core.richtext.engine.SegmentEditor
import com.app.pustakam.core.richtext.engine.SpanEngine
import com.app.pustakam.core.richtext.model.EditableSegment
import com.app.pustakam.core.richtext.model.ParagraphSpan
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.presentation.ToolbarState

object MasterTextReducer {

    fun reduce(state: MasterTextState, intent: MasterTextIntent): MasterTextState = when (intent) {

        is MasterTextIntent.Edit -> onEdit(state, intent)

        is MasterTextIntent.SelectionChanged -> withRefreshedToolbar(
            state.copy(
                selection = MasterTextSelection(intent.start, intent.end)
                    .coercedIn(state.text.length),
                pendingStyle = null,
                toolbarDismissed = false
            )
        )

        MasterTextIntent.SelectAll -> withRefreshedToolbar(
            state.copy(selection = MasterTextSelection(0, state.text.length))
        )

        is MasterTextIntent.SelectWord -> onSelectWord(state, intent.offset)

        is MasterTextIntent.SelectParagraph -> onSelectParagraph(state, intent.offset)

        is MasterTextIntent.ToggleFormat -> styled(
            state,
            arm = { it.copy(formats = it.formats.toggle(intent.format)) },
            apply = { block, start, end ->
                val enable = !SpanEngine.isActive(block.spans, start, end, intent.format)
                block.copy(
                    spans = SpanEngine.setFormat(
                        block.spans, start, end, block.text.length, intent.format, enable
                    )
                )
            }
        )

        is MasterTextIntent.SetTextColor -> styled(
            state,
            arm = { it.copy(textColor = intent.color) },
            apply = { block, start, end ->
                block.copy(
                    spans = SpanEngine.setTextColor(
                        block.spans, start, end, block.text.length, intent.color
                    )
                )
            }
        )

        is MasterTextIntent.SetBackgroundColor -> styled(
            state,
            arm = { it.copy(backgroundColor = intent.color) },
            apply = { block, start, end ->
                block.copy(
                    spans = SpanEngine.setBackgroundColor(
                        block.spans, start, end, block.text.length, intent.color
                    )
                )
            }
        )

        is MasterTextIntent.SetFontSize -> styled(
            state,
            arm = { it.copy(fontSize = intent.size) },
            apply = { block, start, end ->
                block.copy(
                    spans = SpanEngine.setFontSize(
                        block.spans, start, end, block.text.length, intent.size
                    )
                )
            }
        )

        is MasterTextIntent.SetFontWeight -> styled(
            state,
            arm = { it.copy(fontWeight = intent.weight) },
            apply = { block, start, end ->
                block.copy(
                    spans = SpanEngine.setFontWeight(
                        block.spans, start, end, block.text.length, intent.weight
                    )
                )
            }
        )

        is MasterTextIntent.SetLink -> styled(
            state,
            arm = { it.copy(link = intent.url) },
            apply = { block, start, end ->
                block.copy(
                    spans = SpanEngine.setLink(
                        block.spans, start, end, block.text.length, intent.url
                    )
                )
            }
        )

        MasterTextIntent.ClearFormatting -> recorded(state) { current ->
            mapSelectedRanges(current) { block, start, end ->
                block.copy(
                    spans = SpanEngine.clearFormatting(block.spans, start, end, block.text.length),
                    style = ParagraphStyle.PARAGRAPH,
                    align = TextAlign.START,
                    list = null,
                    indent = 0
                )
            }
        }

        is MasterTextIntent.SetParagraphStyle -> recorded(state) { current ->
            mapSelectedParagraphs(current) { it.copy(style = intent.style, list = null) }
        }

        is MasterTextIntent.SetAlignment -> recorded(state) { current ->
            mapSelectedParagraphs(current) { it.copy(align = intent.align) }
        }

        is MasterTextIntent.ToggleList -> recorded(state) { current ->
            mapSelectedParagraphs(current) { ListEngine.toggle(it, intent.style) }
        }

        MasterTextIntent.Indent -> recorded(state) { current ->
            mapSelectedParagraphs(current) { ListEngine.indent(it) }
        }

        MasterTextIntent.Outdent -> recorded(state) { current ->
            mapSelectedParagraphs(current) { ListEngine.outdent(it) }
        }

        MasterTextIntent.ToggleQuote -> recorded(state) { current ->
            mapSelectedParagraphs(current) {
                it.copy(
                    style = if (it.style == ParagraphStyle.QUOTE) ParagraphStyle.PARAGRAPH
                    else ParagraphStyle.QUOTE
                )
            }
        }

        is MasterTextIntent.ToggleChecked -> recorded(state) { current ->
            val paragraph = current.segment.paragraphAt(intent.offset)
                ?: return@recorded current
            val block = current.document.textBlockById(paragraph.blockId)
                ?: return@recorded current
            current.copy(
                document = current.document.replacingBlock(ListEngine.toggleChecked(block))
            )
        }

        MasterTextIntent.Undo -> onUndo(state)

        MasterTextIntent.Redo -> onRedo(state)

        is MasterTextIntent.SetToolbarExpanded -> state.copy(isToolbarExpanded = intent.expanded)

        MasterTextIntent.DismissToolbar -> state.copy(
            toolbarDismissed = true,
            showSelectionToolbar = false,
            isToolbarExpanded = false
        )
    }

    private fun onEdit(state: MasterTextState, intent: MasterTextIntent.Edit): MasterTextState {
        val segment = state.segment
        if (segment.paragraphs.isEmpty()) return state
        if (segment.text == intent.text) {
            return withRefreshedToolbar(
                state.copy(
                    selection = MasterTextSelection(intent.selectionStart, intent.selectionEnd)
                        .coercedIn(intent.text.length)
                )
            )
        }

        val result = SegmentEditor.applyEdit(
            document = state.document,
            segment = segment,
            newText = intent.text,
            caret = intent.selectionEnd,
            pendingStyle = state.pendingStyle
        )

        val linkified = linkifyChangedParagraphs(result.document, segment, intent.text)

        return withRefreshedToolbar(
            state.copy(
                document = linkified,
                selection = MasterTextSelection.caret(result.caret),
                pendingStyle = null,
                history = state.history.record(
                    MasterTextHistoryEntry(state.document, state.selection),
                    coalesce = true
                )
            )
        )
    }

    private fun linkifyChangedParagraphs(
        document: RichDocument,
        previous: EditableSegment,
        newText: String
    ): RichDocument {
        if (!newText.contains(' ') && !newText.contains('\n')) return document
        val previousIds = previous.paragraphs.map { it.blockId }.toSet()
        var result = document
        document.blocks.forEach { block ->
            if (block is RichBlock.Text && block.id in previousIds) {
                val linked = AutoFormatEngine.linkify(block)
                if (linked != block) result = result.replacingBlock(linked)
            }
        }
        return result
    }

    private fun styled(
        state: MasterTextState,
        arm: (RichSpan) -> RichSpan,
        apply: (RichBlock.Text, Int, Int) -> RichBlock.Text
    ): MasterTextState = if (state.selection.isCollapsed) {
        withRefreshedToolbar(state.copy(pendingStyle = arm(state.pendingStyle ?: caretStyle(state))))
    } else {
        recorded(state) { current -> mapSelectedRanges(current, apply) }
    }

    private fun caretStyle(state: MasterTextState): RichSpan {
        val segment = state.segment
        val paragraph = segment.paragraphAt(state.selection.normalizedStart) ?: return RichSpan(0, 0)
        val block = state.document.textBlockById(paragraph.blockId) ?: return RichSpan(0, 0)
        val local = state.selection.normalizedStart - paragraph.start
        return SpanEngine.styleForCaret(block.spans, local)?.styleOnly() ?: RichSpan(0, 0)
    }

    private fun selectedParagraphs(state: MasterTextState): List<ParagraphSpan> =
        state.segment.paragraphsIn(state.selection.normalizedStart, state.selection.normalizedEnd)

    private fun mapSelectedParagraphs(
        state: MasterTextState,
        transform: (RichBlock.Text) -> RichBlock.Text
    ): MasterTextState {
        val ids = selectedParagraphs(state).map { it.blockId }.toSet()
        if (ids.isEmpty()) return state
        return state.copy(
            document = state.document.withBlocks(
                state.document.blocks.map { block ->
                    if (block is RichBlock.Text && block.id in ids) transform(block) else block
                }
            )
        )
    }

    private fun mapSelectedRanges(
        state: MasterTextState,
        transform: (RichBlock.Text, Int, Int) -> RichBlock.Text
    ): MasterTextState {
        val segment = state.segment
        val paragraphs = selectedParagraphs(state)
        if (paragraphs.isEmpty()) return state
        val from = state.selection.normalizedStart
        val to = state.selection.normalizedEnd

        var document = state.document
        paragraphs.forEach { paragraph ->
            val block = document.textBlockById(paragraph.blockId) ?: return@forEach
            val local = segment.localRange(paragraph, from, to) ?: return@forEach
            val start = local.first.coerceIn(0, block.text.length)
            val end = local.last.coerceIn(start, block.text.length)
            if (end > start) document = document.replacingBlock(transform(block, start, end))
        }
        return state.copy(document = document)
    }

    private fun onSelectWord(state: MasterTextState, offset: Int): MasterTextState {
        val text = state.text
        if (text.isEmpty()) return state
        val safe = offset.coerceIn(0, text.length - 1)
        var start = safe
        var end = safe
        while (start > 0 && text[start - 1].isLetterOrDigit()) start--
        while (end < text.length && text[end].isLetterOrDigit()) end++
        return withRefreshedToolbar(state.copy(selection = MasterTextSelection(start, end)))
    }

    private fun onSelectParagraph(state: MasterTextState, offset: Int): MasterTextState {
        val paragraph = state.segment.paragraphAt(offset) ?: return state
        return withRefreshedToolbar(
            state.copy(selection = MasterTextSelection(paragraph.start, paragraph.end))
        )
    }

    private fun recorded(
        state: MasterTextState,
        transform: (MasterTextState) -> MasterTextState
    ): MasterTextState {
        val next = transform(state)
        if (next.document == state.document) return withRefreshedToolbar(next)
        return withRefreshedToolbar(
            next.copy(
                history = next.history.record(
                    MasterTextHistoryEntry(state.document, state.selection)
                )
            )
        )
    }

    private fun onUndo(state: MasterTextState): MasterTextState {
        val result = state.history.undo(
            MasterTextHistoryEntry(state.document, state.selection)
        ) ?: return state
        val (history, entry) = result
        return withRefreshedToolbar(
            state.copy(document = entry.document, selection = entry.selection, history = history)
        )
    }

    private fun onRedo(state: MasterTextState): MasterTextState {
        val result = state.history.redo(
            MasterTextHistoryEntry(state.document, state.selection)
        ) ?: return state
        val (history, entry) = result
        return withRefreshedToolbar(
            state.copy(document = entry.document, selection = entry.selection, history = history)
        )
    }

    fun withRefreshedToolbar(state: MasterTextState): MasterTextState {
        val segment = state.segment
        val selection = state.selection.coercedIn(segment.text.length)
        val paragraphs = segment.paragraphsIn(selection.normalizedStart, selection.normalizedEnd)
        val first = paragraphs.firstOrNull()
        val block = first?.let { state.document.textBlockById(it.blockId) }
        val pending = state.pendingStyle

        if (block == null) {
            return state.copy(selection = selection, toolbar = ToolbarState())
        }

        val local = segment.localRange(first, selection.normalizedStart, selection.normalizedEnd)
        val start = (local?.first ?: 0).coerceIn(0, block.text.length)
        val end = (local?.last ?: 0).coerceIn(start, block.text.length)

        val formats = pending?.formats ?: paragraphs
            .mapNotNull { paragraph ->
                val paragraphBlock = state.document.textBlockById(paragraph.blockId)
                    ?: return@mapNotNull null
                val range = segment.localRange(
                    paragraph, selection.normalizedStart, selection.normalizedEnd
                ) ?: return@mapNotNull null
                SpanEngine.commonFormats(
                    paragraphBlock.spans,
                    range.first.coerceIn(0, paragraphBlock.text.length),
                    range.last.coerceIn(0, paragraphBlock.text.length)
                )
            }
            .reduceOrNull { acc, next -> acc.intersect(next) }
            ?: com.app.pustakam.core.richtext.model.FormatSet.EMPTY

        return state.copy(
            selection = selection,
            toolbar = ToolbarState(
                activeFormats = formats,
                paragraphStyle = block.style,
                align = block.align,
                listStyle = block.list?.style,
                canIndent = ListEngine.canIndent(block),
                canOutdent = ListEngine.canOutdent(block),
                isCodeBlock = false,
                isQuote = block.style == ParagraphStyle.QUOTE,
                link = pending?.link
                    ?: SpanEngine.commonAttribute(block.spans, start, end) { it.link },
                textColor = pending?.textColor
                    ?: SpanEngine.commonAttribute(block.spans, start, end) { it.textColor },
                backgroundColor = pending?.backgroundColor
                    ?: SpanEngine.commonAttribute(block.spans, start, end) { it.backgroundColor },
                fontSize = pending?.fontSize ?: SpanEngine.spanAt(block.spans, start)?.fontSize,
                fontWeight = pending?.fontWeight ?: SpanEngine.spanAt(block.spans, start)?.fontWeight
            ),
            showSelectionToolbar = !selection.isCollapsed && !state.toolbarDismissed
        )
    }
}
