package com.app.pustakam.core.richtext.presentation

import com.app.pustakam.core.richtext.engine.AutoFormatEngine
import com.app.pustakam.core.richtext.engine.FindReplaceEngine
import com.app.pustakam.core.richtext.engine.HistoryEntry
import com.app.pustakam.core.richtext.engine.ListEngine
import com.app.pustakam.core.richtext.engine.MarkdownOutcome
import com.app.pustakam.core.richtext.engine.MarkdownShortcutEngine
import com.app.pustakam.core.richtext.engine.SpanEngine
import com.app.pustakam.core.richtext.engine.TableCommandRunner
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TableData
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.common.util.UniqueIdGenerator

/**
 * The single source of editing behaviour. Pure: same state + same intent gives the same result on
 * Compose and on SwiftUI, which is what keeps the two widgets from drifting apart.
 */
object SmartTextReducer {

    fun reduce(state: SmartTextState, intent: SmartTextIntent): SmartTextState = when (intent) {

        is SmartTextIntent.TypeText -> onTypeText(state, intent)

        is SmartTextIntent.SelectionChanged -> withRefreshedToolbar(
            state.copy(
                selection = intent.selection,
                pendingStyle = null,
                toolbarDismissed = false
            )
        )

        SmartTextIntent.DismissToolbar -> state.copy(
            toolbarDismissed = true,
            showSelectionToolbar = false,
            isToolbarExpanded = false
        )

        is SmartTextIntent.SplitBlock -> onSplitBlock(state, intent.blockId, intent.caret)

        is SmartTextIntent.MergeWithPrevious -> onMergeWithPrevious(state, intent.blockId)

        is SmartTextIntent.ToggleFormat -> styled(
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

        is SmartTextIntent.SetParagraphStyle -> recorded(state) {
            mapSelectedBlocks(it) { block -> block.copy(style = intent.style, list = null) }
        }

        is SmartTextIntent.SetAlignment -> recorded(state) {
            mapSelectedBlocks(it) { block -> block.copy(align = intent.align) }
        }

        is SmartTextIntent.ToggleList -> recorded(state) {
            mapSelectedBlocks(it) { block -> ListEngine.toggle(block, intent.style) }
        }

        is SmartTextIntent.ToggleChecked -> recorded(state) { current ->
            val block = current.document.textBlockById(intent.blockId) ?: return@recorded current
            current.copy(document = current.document.replacingBlock(ListEngine.toggleChecked(block)))
        }

        SmartTextIntent.Indent -> recorded(state) {
            mapSelectedBlocks(it) { block -> ListEngine.indent(block) }
        }

        SmartTextIntent.Outdent -> recorded(state) {
            mapSelectedBlocks(it) { block -> ListEngine.outdent(block) }
        }

        is SmartTextIntent.SetFirstLineIndent -> recorded(state) {
            mapSelectedBlocks(it) { block -> block.copy(firstLineIndent = intent.enabled) }
        }

        is SmartTextIntent.SetLineHeight -> recorded(state) {
            mapSelectedBlocks(it) { block -> block.copy(lineHeight = intent.value) }
        }

        is SmartTextIntent.SetParagraphSpacing -> recorded(state) {
            mapSelectedBlocks(it) { block -> block.copy(paragraphSpacing = intent.value) }
        }

        is SmartTextIntent.SetTextColor -> styled(
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

        is SmartTextIntent.SetBackgroundColor -> styled(
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

        is SmartTextIntent.SetFontSize -> styled(
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

        is SmartTextIntent.SetFontWeight -> styled(
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

        is SmartTextIntent.SetFontFamily -> styled(
            state,
            arm = { it.copy(fontFamily = intent.family) },
            apply = { block, start, end ->
                block.copy(
                    spans = SpanEngine.setFontFamily(
                        block.spans, start, end, block.text.length, intent.family
                    )
                )
            }
        )

        SmartTextIntent.ToggleQuote -> recorded(state) {
            mapSelectedBlocks(it) { block ->
                block.copy(
                    style = if (block.style == ParagraphStyle.QUOTE) ParagraphStyle.PARAGRAPH
                    else ParagraphStyle.QUOTE
                )
            }
        }

        SmartTextIntent.InsertDivider ->
            recorded(state) { insertBlock(it, RichBlock.Divider(newId())) }

        SmartTextIntent.InsertCodeBlock ->
            recorded(state) { insertBlock(it, RichBlock.Code(newId())) }

        is SmartTextIntent.UpdateCodeBlock -> {
            val block = state.document.blockById(intent.blockId) as? RichBlock.Code
            if (block == null) state
            else state.copy(
                document = state.document.replacingBlock(block.copy(code = intent.code))
            )
        }

        is SmartTextIntent.InsertTable -> recorded(state) {
            insertBlock(it, RichBlock.Table(newId(), TableData.empty(intent.rows, intent.columns)))
        }

        is SmartTextIntent.UpdateTableCell -> {
            val block = state.document.blockById(intent.blockId) as? RichBlock.Table
            if (block == null) state else state.copy(
                document = state.document.replacingBlock(
                    block.copy(
                        data = TableCommandRunner.updateCellText(
                            block.data, intent.row, intent.column, intent.text
                        )
                    )
                )
            )
        }

        is SmartTextIntent.TableAction -> recorded(state) { current ->
            val block = current.document.blockById(intent.blockId) as? RichBlock.Table
                ?: return@recorded current
            current.copy(
                document = current.document.replacingBlock(
                    block.copy(data = TableCommandRunner.run(block.data, intent.action))
                )
            )
        }

        is SmartTextIntent.SetLink -> recorded(state) {
            mapSelectedRanges(it) { block, start, end ->
                block.copy(
                    spans = SpanEngine.setLink(block.spans, start, end, block.text.length, intent.url)
                )
            }
        }

        SmartTextIntent.RemoveLink -> recorded(state) {
            mapSelectedRanges(it) { block, start, end ->
                block.copy(
                    spans = SpanEngine.setLink(block.spans, start, end, block.text.length, null)
                )
            }
        }

        SmartTextIntent.ClearFormatting -> recorded(state) {
            mapSelectedRanges(it) { block, start, end ->
                block.copy(
                    spans = SpanEngine.clearFormatting(block.spans, start, end, block.text.length),
                    style = ParagraphStyle.PARAGRAPH,
                    align = TextAlign.START,
                    list = null,
                    indent = 0
                )
            }
        }

        SmartTextIntent.Undo -> onUndo(state)

        SmartTextIntent.Redo -> onRedo(state)

        SmartTextIntent.Copy -> state.copy(clipboard = clipboardFromSelection(state))

        SmartTextIntent.Cut -> recorded(state.copy(clipboard = clipboardFromSelection(state))) {
            deleteSelection(it)
        }

        is SmartTextIntent.Paste -> recorded(state) { onPaste(it, intent.text, intent.keepFormatting) }

        SmartTextIntent.SelectAll -> onSelectAll(state)

        SmartTextIntent.DuplicateSelection -> recorded(state) { onDuplicate(it) }

        is SmartTextIntent.SelectWord -> onSelectWord(state, intent.blockId, intent.offset)

        is SmartTextIntent.SelectParagraph -> onSelectParagraph(state, intent.blockId)

        is SmartTextIntent.SetSearchQuery -> onSearch(state, intent.query, intent.matchCase)

        is SmartTextIntent.SetReplacement ->
            state.copy(search = state.search.copy(replacement = intent.replacement))

        SmartTextIntent.FindNext -> state.copy(
            search = state.search.copy(
                currentIndex = FindReplaceEngine.next(state.search.matches, state.search.currentIndex)
            )
        )

        SmartTextIntent.FindPrevious -> state.copy(
            search = state.search.copy(
                currentIndex = FindReplaceEngine.previous(
                    state.search.matches, state.search.currentIndex
                )
            )
        )

        SmartTextIntent.ReplaceCurrent -> onReplaceCurrent(state)

        SmartTextIntent.ReplaceAll -> onReplaceAll(state)

        is SmartTextIntent.SetSearchActive -> state.copy(
            search = if (intent.active) state.search.copy(isActive = true)
            else SearchState()
        )

        is SmartTextIntent.SetToolbarExpanded -> state.copy(isToolbarExpanded = intent.expanded)

        is SmartTextIntent.ApplySpanStyle -> recorded(state) {
            mapSelectedRanges(it) { block, start, end ->
                block.copy(
                    spans = SpanEngine.apply(block.spans, start, end, block.text.length) { span ->
                        span.copy(
                            formats = intent.span.formats,
                            textColor = intent.span.textColor ?: span.textColor,
                            backgroundColor = intent.span.backgroundColor ?: span.backgroundColor,
                            fontSize = intent.span.fontSize ?: span.fontSize,
                            fontWeight = intent.span.fontWeight ?: span.fontWeight,
                            fontFamily = intent.span.fontFamily ?: span.fontFamily,
                            link = intent.span.link ?: span.link
                        )
                    }
                )
            }
        }

        SmartTextIntent.DeleteFocusedBlock -> recorded(state) { current ->
            val index = current.document.indexOf(current.focusedBlockId)
            val document = current.document.removingBlock(current.focusedBlockId)
            val target = document.blocks.getOrNull((index - 1).coerceAtLeast(0))
                ?: document.blocks.first()
            current.copy(
                document = document,
                selection = DocumentSelection.caret(target.id, target.plainText.length)
            )
        }
    }

    /**
     * Character-level styling has two modes: with a selection it rewrites spans, with a bare caret
     * it arms [SmartTextState.pendingStyle] so the next characters typed carry the style. Without
     * this a colour picked before typing would silently do nothing.
     */
    private fun styled(
        state: SmartTextState,
        arm: (RichSpan) -> RichSpan,
        apply: (RichBlock.Text, Int, Int) -> RichBlock.Text
    ): SmartTextState = if (state.selection.isCollapsed) {
        withRefreshedToolbar(state.copy(pendingStyle = arm(state.pendingStyle ?: caretStyle(state))))
    } else {
        recorded(state) { mapSelectedRanges(it, apply) }
    }

    private fun caretStyle(state: SmartTextState): RichSpan {
        val block = state.document.textBlockById(state.focusedBlockId) ?: return RichSpan(0, 0)
        return SpanEngine.styleForCaret(block.spans, state.selection.normalizedStart)?.styleOnly()
            ?: RichSpan(0, 0)
    }

    // region typing

    private fun onTypeText(state: SmartTextState, intent: SmartTextIntent.TypeText): SmartTextState {
        val block = state.document.textBlockById(intent.blockId) ?: return state
        if (block.text == intent.text) {
            return withRefreshedToolbar(
                state.copy(selection = DocumentSelection.caret(intent.blockId, intent.caret))
            )
        }

        val caret = intent.caret.coerceIn(0, intent.text.length)
        val edited = AutoFormatEngine.linkify(
            applyTextEdit(block, intent.text, caret, state.pendingStyle)
        )
        var next = state.copy(
            document = state.document.replacingBlock(edited),
            selection = DocumentSelection.caret(intent.blockId, caret),
            pendingStyle = null
        )

        next = applyMarkdown(next, edited, caret)
        return withRefreshedToolbar(
            next.copy(
                history = next.history.record(
                    HistoryEntry(state.document, state.selection),
                    coalesce = true
                )
            )
        )
    }

    private fun applyMarkdown(
        state: SmartTextState,
        block: RichBlock.Text,
        caret: Int
    ): SmartTextState {
        MarkdownShortcutEngine.detectStandalone(block)?.let { outcome ->
            return when (outcome) {
                is MarkdownOutcome.ReplaceWithDivider -> {
                    val index = state.document.indexOf(block.id)
                    val trailing = RichDocument.newTextBlock()
                    val blocks = state.document.blocks.toMutableList()
                    blocks[index] = RichBlock.Divider(newId())
                    blocks.add(index + 1, trailing)
                    state.copy(
                        document = state.document.withBlocks(blocks),
                        selection = DocumentSelection.caret(trailing.id, 0)
                    )
                }

                is MarkdownOutcome.ReplaceWithCode -> {
                    val code = RichBlock.Code(newId())
                    state.copy(
                        document = state.document.withBlocks(
                            state.document.blocks.toMutableList().apply {
                                set(state.document.indexOf(block.id), code)
                            }
                        ),
                        selection = DocumentSelection.caret(code.id, 0)
                    )
                }

                is MarkdownOutcome.Rewrite -> state
            }
        }

        val outcome = MarkdownShortcutEngine.detect(block, caret) as? MarkdownOutcome.Rewrite
            ?: return state
        return state.copy(
            document = state.document.replacingBlock(outcome.block),
            selection = DocumentSelection.caret(outcome.block.id, outcome.caret)
        )
    }

    /** Rebases spans over a text edit by diffing the common prefix and suffix. */
    private fun applyTextEdit(
        block: RichBlock.Text,
        newText: String,
        caret: Int,
        pendingStyle: RichSpan?
    ): RichBlock.Text {
        val old = block.text
        val prefix = commonPrefixLength(old, newText)
        val suffix = commonSuffixLength(old, newText, prefix)
        val removedTo = old.length - suffix
        val insertedLength = newText.length - suffix - prefix

        var spans = SpanEngine.afterDelete(block.spans, prefix, removedTo)
        if (insertedLength > 0) {
            spans = SpanEngine.afterInsert(spans, prefix, insertedLength)
            val inherited = SpanEngine.styleForCaret(block.spans, caret - insertedLength)
            // an armed toolbar pick wins; otherwise the new text inherits the character to its left
            val source = pendingStyle ?: inherited?.takeIf { it.link == null }
            if (source != null) {
                spans = SpanEngine.apply(
                    spans, prefix, prefix + insertedLength, newText.length
                ) { span ->
                    span.copy(
                        formats = source.formats,
                        textColor = source.textColor,
                        backgroundColor = source.backgroundColor,
                        fontSize = source.fontSize,
                        fontWeight = source.fontWeight,
                        fontFamily = source.fontFamily
                    )
                }
            }
        }
        return block.copy(text = newText, spans = SpanEngine.normalize(spans, newText.length))
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        var index = 0
        val max = minOf(a.length, b.length)
        while (index < max && a[index] == b[index]) index++
        return index
    }

    private fun commonSuffixLength(a: String, b: String, prefix: Int): Int {
        var index = 0
        val max = minOf(a.length - prefix, b.length - prefix)
        while (index < max && a[a.length - 1 - index] == b[b.length - 1 - index]) index++
        return index
    }

    // endregion

    // region structure

    private fun onSplitBlock(state: SmartTextState, blockId: String, caret: Int): SmartTextState {
        val block = state.document.textBlockById(blockId) ?: return state

        if (ListEngine.shouldExitList(block)) {
            return recorded(state) {
                it.copy(document = it.document.replacingBlock(ListEngine.outdent(block)))
            }
        }

        val head = block.copy(
            text = block.text.take(caret),
            spans = SpanEngine.slice(block.spans, 0, caret)
        )
        val tail = RichBlock.Text(
            id = newId(),
            text = block.text.drop(caret),
            spans = SpanEngine.slice(block.spans, caret, block.text.length),
            style = if (block.style.isHeading || block.style == ParagraphStyle.TITLE ||
                block.style == ParagraphStyle.SUBTITLE
            ) ParagraphStyle.PARAGRAPH else block.style,
            align = block.align,
            list = ListEngine.markerForNewSibling(block),
            indent = block.indent,
            lineHeight = block.lineHeight,
            paragraphSpacing = block.paragraphSpacing
        )

        return recorded(state) { current ->
            val index = current.document.indexOf(blockId)
            val blocks = current.document.blocks.toMutableList()
            blocks[index] = head
            blocks.add(index + 1, tail)
            current.copy(
                document = current.document.withBlocks(blocks),
                selection = DocumentSelection.caret(tail.id, 0)
            )
        }
    }

    private fun onMergeWithPrevious(state: SmartTextState, blockId: String): SmartTextState {
        val block = state.document.textBlockById(blockId) ?: return state

        if (ListEngine.canOutdent(block)) {
            return recorded(state) {
                it.copy(document = it.document.replacingBlock(ListEngine.outdent(block)))
            }
        }
        if (block.style != ParagraphStyle.PARAGRAPH) {
            return recorded(state) {
                it.copy(
                    document = it.document.replacingBlock(
                        block.copy(style = ParagraphStyle.PARAGRAPH)
                    )
                )
            }
        }

        val index = state.document.indexOf(blockId)
        if (index <= 0) return state
        val previous = state.document.blocks[index - 1]

        if (previous !is RichBlock.Text) {
            return recorded(state) { current ->
                current.copy(document = current.document.removingBlock(previous.id))
            }
        }

        val merged = previous.copy(
            text = previous.text + block.text,
            spans = SpanEngine.normalize(
                SpanEngine.concat(previous.spans, previous.text.length, block.spans),
                previous.text.length + block.text.length
            )
        )
        return recorded(state) { current ->
            val blocks = current.document.blocks.toMutableList()
            blocks[index - 1] = merged
            blocks.removeAt(index)
            current.copy(
                document = current.document.withBlocks(blocks),
                selection = DocumentSelection.caret(merged.id, previous.text.length)
            )
        }
    }

    private fun insertBlock(state: SmartTextState, block: RichBlock): SmartTextState {
        val index = state.document.indexOf(state.focusedBlockId)
        val trailing = RichDocument.newTextBlock()
        val blocks = state.document.blocks.toMutableList()
        val at = if (index < 0) blocks.size else index + 1
        blocks.add(at, block)
        blocks.add(at + 1, trailing)
        return state.copy(
            document = state.document.withBlocks(blocks),
            selection = DocumentSelection.caret(trailing.id, 0)
        )
    }

    // endregion

    // region selection helpers

    private fun selectedBlockIds(state: SmartTextState): List<String> {
        val startIndex = state.document.indexOf(state.selection.startBlockId)
        val endIndex = state.document.indexOf(state.selection.endBlockId)
        if (startIndex < 0) return emptyList()
        if (endIndex < 0) return listOf(state.selection.startBlockId)
        val from = minOf(startIndex, endIndex)
        val to = maxOf(startIndex, endIndex)
        return (from..to).map { state.document.blocks[it].id }
    }

    private fun mapSelectedBlocks(
        state: SmartTextState,
        transform: (RichBlock.Text) -> RichBlock.Text
    ): SmartTextState {
        val ids = selectedBlockIds(state).toSet()
        if (ids.isEmpty()) return state
        return state.copy(
            document = state.document.withBlocks(
                state.document.blocks.map { block ->
                    if (block.id in ids && block is RichBlock.Text) transform(block) else block
                }
            )
        )
    }

    /** Runs [transform] over each block's slice of the selection — the whole block when it is in the middle. */
    private fun mapSelectedRanges(
        state: SmartTextState,
        transform: (RichBlock.Text, Int, Int) -> RichBlock.Text
    ): SmartTextState {
        val ids = selectedBlockIds(state)
        if (ids.isEmpty()) return state
        val selection = state.selection
        val singleBlock = ids.size == 1

        return state.copy(
            document = state.document.withBlocks(
                state.document.blocks.map { block ->
                    if (block !is RichBlock.Text || block.id !in ids) return@map block
                    val start: Int
                    val end: Int
                    when {
                        singleBlock -> {
                            start = selection.normalizedStart.coerceIn(0, block.text.length)
                            end = selection.normalizedEnd.coerceIn(start, block.text.length)
                        }

                        block.id == ids.first() -> {
                            start = selection.startOffset.coerceIn(0, block.text.length)
                            end = block.text.length
                        }

                        block.id == ids.last() -> {
                            start = 0
                            end = selection.endOffset.coerceIn(0, block.text.length)
                        }

                        else -> {
                            start = 0
                            end = block.text.length
                        }
                    }
                    // a collapsed caret still styles the whole block so the next keystroke inherits it
                    if (start == end && block.text.isNotEmpty()) block
                    else transform(block, start, if (start == end) block.text.length else end)
                }
            )
        )
    }

    private fun onSelectAll(state: SmartTextState): SmartTextState {
        val first = state.document.blocks.first()
        val last = state.document.blocks.last()
        return withRefreshedToolbar(
            state.copy(
                selection = DocumentSelection(first.id, 0, last.id, last.plainText.length),
                showSelectionToolbar = true
            )
        )
    }

    private fun onSelectWord(state: SmartTextState, blockId: String, offset: Int): SmartTextState {
        val block = state.document.textBlockById(blockId) ?: return state
        if (block.text.isEmpty()) return state
        val safe = offset.coerceIn(0, block.text.length - 1)
        var start = safe
        var end = safe
        while (start > 0 && block.text[start - 1].isLetterOrDigit()) start--
        while (end < block.text.length && block.text[end].isLetterOrDigit()) end++
        return withRefreshedToolbar(
            state.copy(
                selection = DocumentSelection(blockId, start, blockId, end),
                showSelectionToolbar = end > start
            )
        )
    }

    private fun onSelectParagraph(state: SmartTextState, blockId: String): SmartTextState {
        val block = state.document.blockById(blockId) ?: return state
        return withRefreshedToolbar(
            state.copy(
                selection = DocumentSelection(blockId, 0, blockId, block.plainText.length),
                showSelectionToolbar = block.plainText.isNotEmpty()
            )
        )
    }

    // endregion

    // region clipboard

    private fun clipboardFromSelection(state: SmartTextState): ClipboardPayload? {
        val ids = selectedBlockIds(state)
        if (ids.isEmpty() || state.selection.isCollapsed) return null
        val blocks = mutableListOf<RichBlock>()
        ids.forEach { id ->
            val block = state.document.blockById(id) ?: return@forEach
            if (block !is RichBlock.Text) {
                blocks.add(block)
                return@forEach
            }
            val start = if (id == ids.first() && ids.size > 1) state.selection.startOffset
            else if (ids.size == 1) state.selection.normalizedStart else 0
            val end = if (id == ids.last() && ids.size > 1) state.selection.endOffset
            else if (ids.size == 1) state.selection.normalizedEnd else block.text.length
            val from = start.coerceIn(0, block.text.length)
            val to = end.coerceIn(from, block.text.length)
            blocks.add(
                block.copy(
                    id = newId(),
                    text = block.text.substring(from, to),
                    spans = SpanEngine.slice(block.spans, from, to)
                )
            )
        }
        val document = RichDocument(blocks)
        return ClipboardPayload(text = document.plainText, document = document)
    }

    private fun deleteSelection(state: SmartTextState): SmartTextState {
        if (state.selection.isCollapsed) return state
        val ids = selectedBlockIds(state)
        val first = state.document.textBlockById(ids.first()) ?: return state

        if (ids.size == 1) {
            val from = state.selection.normalizedStart.coerceIn(0, first.text.length)
            val to = state.selection.normalizedEnd.coerceIn(from, first.text.length)
            val text = first.text.removeRange(from, to)
            return state.copy(
                document = state.document.replacingBlock(
                    first.copy(
                        text = text,
                        spans = SpanEngine.normalize(
                            SpanEngine.afterDelete(first.spans, from, to), text.length
                        )
                    )
                ),
                selection = DocumentSelection.caret(first.id, from)
            )
        }

        val last = state.document.textBlockById(ids.last())
        val headEnd = state.selection.startOffset.coerceIn(0, first.text.length)
        val tailStart = (last?.let { state.selection.endOffset.coerceIn(0, it.text.length) }) ?: 0
        val mergedText = first.text.take(headEnd) + (last?.text?.drop(tailStart) ?: "")
        val mergedSpans = SpanEngine.normalize(
            SpanEngine.concat(
                SpanEngine.slice(first.spans, 0, headEnd),
                headEnd,
                last?.let { SpanEngine.slice(it.spans, tailStart, it.text.length) } ?: emptyList()
            ),
            mergedText.length
        )
        val merged = first.copy(text = mergedText, spans = mergedSpans)
        val remaining = state.document.blocks.filterNot { it.id in ids }
        val index = state.document.indexOf(ids.first())
        val blocks = remaining.toMutableList()
        blocks.add(index.coerceIn(0, blocks.size), merged)
        return state.copy(
            document = state.document.withBlocks(blocks),
            selection = DocumentSelection.caret(merged.id, headEnd)
        )
    }

    private fun onPaste(
        state: SmartTextState,
        text: String,
        keepFormatting: Boolean
    ): SmartTextState {
        val cleared = if (state.selection.isCollapsed) state else deleteSelection(state)
        val block = cleared.document.textBlockById(cleared.focusedBlockId) ?: return cleared
        val caret = cleared.selection.startOffset.coerceIn(0, block.text.length)
        val payload = cleared.clipboard?.takeIf { keepFormatting && it.text == text }?.document

        if (payload == null || payload.blocks.size <= 1) {
            val pastedSpans = (payload?.blocks?.firstOrNull() as? RichBlock.Text)?.spans.orEmpty()
            val newText = block.text.replaceRange(caret, caret, text)
            var spans = SpanEngine.afterInsert(block.spans, caret, text.length)
            if (keepFormatting && pastedSpans.isNotEmpty()) {
                spans = spans + pastedSpans.map { it.shifted(caret) }
            }
            return cleared.copy(
                document = cleared.document.replacingBlock(
                    block.copy(text = newText, spans = SpanEngine.normalize(spans, newText.length))
                ),
                selection = DocumentSelection.caret(block.id, caret + text.length)
            )
        }

        val head = block.copy(
            text = block.text.take(caret) + payload.blocks.first().plainText,
            spans = SpanEngine.normalize(
                SpanEngine.concat(
                    SpanEngine.slice(block.spans, 0, caret),
                    caret,
                    (payload.blocks.first() as? RichBlock.Text)?.spans.orEmpty()
                ),
                caret + payload.blocks.first().plainText.length
            )
        )
        val middle = payload.blocks.drop(1).map { copyWithNewId(it) }
        val tailText = block.text.drop(caret)
        val tail = RichBlock.Text(
            id = newId(),
            text = tailText,
            spans = SpanEngine.slice(block.spans, caret, block.text.length)
        )
        val blocks = cleared.document.blocks.toMutableList()
        val index = cleared.document.indexOf(block.id)
        blocks[index] = head
        blocks.addAll(index + 1, middle + tail)
        return cleared.copy(
            document = cleared.document.withBlocks(blocks),
            selection = DocumentSelection.caret(tail.id, 0)
        )
    }

    private fun onDuplicate(state: SmartTextState): SmartTextState {
        val payload = clipboardFromSelection(state) ?: return state
        return onPaste(
            state.copy(
                clipboard = payload,
                selection = DocumentSelection.caret(
                    state.selection.endBlockId,
                    state.selection.endOffset
                )
            ),
            payload.text,
            keepFormatting = true
        )
    }

    private fun copyWithNewId(block: RichBlock): RichBlock = when (block) {
        is RichBlock.Text -> block.copy(id = newId())
        is RichBlock.Code -> block.copy(id = newId())
        is RichBlock.Divider -> block.copy(id = newId())
        is RichBlock.Table -> block.copy(id = newId())
    }

    // endregion

    // region history

    private fun recorded(
        state: SmartTextState,
        transform: (SmartTextState) -> SmartTextState
    ): SmartTextState {
        val next = transform(state)
        if (next.document == state.document) return withRefreshedToolbar(next)
        return withRefreshedToolbar(
            next.copy(history = next.history.record(HistoryEntry(state.document, state.selection)))
        )
    }

    private fun onUndo(state: SmartTextState): SmartTextState {
        val result = state.history.undo(HistoryEntry(state.document, state.selection)) ?: return state
        val (history, entry) = result
        return withRefreshedToolbar(
            state.copy(document = entry.document, selection = entry.selection, history = history)
        )
    }

    private fun onRedo(state: SmartTextState): SmartTextState {
        val result = state.history.redo(HistoryEntry(state.document, state.selection)) ?: return state
        val (history, entry) = result
        return withRefreshedToolbar(
            state.copy(document = entry.document, selection = entry.selection, history = history)
        )
    }

    // endregion

    // region search

    private fun onSearch(state: SmartTextState, query: String, matchCase: Boolean): SmartTextState {
        val matches = FindReplaceEngine.find(state.document, query, matchCase)
        return state.copy(
            search = state.search.copy(
                query = query,
                matchCase = matchCase,
                matches = matches,
                currentIndex = if (matches.isEmpty()) -1 else 0,
                isActive = true
            )
        )
    }

    private fun onReplaceCurrent(state: SmartTextState): SmartTextState {
        val match = state.search.current ?: return state
        return recorded(state) { current ->
            val document = FindReplaceEngine.replaceOne(
                current.document, match, current.search.replacement
            )
            current.copy(document = document)
        }.let { onSearch(it, it.search.query, it.search.matchCase) }
    }

    private fun onReplaceAll(state: SmartTextState): SmartTextState {
        if (state.search.query.isEmpty()) return state
        return recorded(state) { current ->
            current.copy(
                document = FindReplaceEngine.replaceAll(
                    current.document,
                    current.search.query,
                    current.search.replacement,
                    current.search.matchCase
                )
            )
        }.let { onSearch(it, it.search.query, it.search.matchCase) }
    }

    // endregion

    /** Recomputes the toolbar from the current selection. Called after every state change. */
    fun withRefreshedToolbar(state: SmartTextState): SmartTextState {
        val block = state.document.blockById(state.selection.startBlockId)
        val text = block as? RichBlock.Text

        if (text == null) {
            return state.copy(
                toolbar = ToolbarState(isCodeBlock = block is RichBlock.Code),
                showSelectionToolbar = false
            )
        }

        val start = state.selection.normalizedStart.coerceIn(0, text.text.length)
        val end = state.selection.normalizedEnd.coerceIn(start, text.text.length)
        // an armed pick must read as active even though no character carries it yet
        val pending = state.pendingStyle

        return state.copy(
            toolbar = ToolbarState(
                activeFormats = pending?.formats ?: SpanEngine.commonFormats(text.spans, start, end),
                paragraphStyle = text.style,
                align = text.align,
                listStyle = text.list?.style,
                canIndent = ListEngine.canIndent(text),
                canOutdent = ListEngine.canOutdent(text),
                isCodeBlock = false,
                isQuote = text.style == ParagraphStyle.QUOTE,
                link = SpanEngine.commonAttribute(text.spans, start, end) { it.link },
                textColor = pending?.textColor
                    ?: SpanEngine.commonAttribute(text.spans, start, end) { it.textColor },
                backgroundColor = pending?.backgroundColor
                    ?: SpanEngine.commonAttribute(text.spans, start, end) { it.backgroundColor },
                fontSize = pending?.fontSize ?: SpanEngine.spanAt(text.spans, start)?.fontSize,
                fontWeight = pending?.fontWeight ?: SpanEngine.spanAt(text.spans, start)?.fontWeight
            ),
            showSelectionToolbar = !state.selection.isCollapsed && !state.toolbarDismissed
        )
    }

    private fun newId(): String = UniqueIdGenerator.generateUniqueId()
}
