package com.app.pustakam.core.richtext.presentation

import com.app.pustakam.core.richtext.engine.HistoryStack
import com.app.pustakam.core.richtext.engine.SearchMatch
import com.app.pustakam.core.richtext.model.FormatSet
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextAlign
import kotlinx.serialization.Serializable

/** Caret or selection expressed in document coordinates, so it survives block splits and merges. */
@Serializable
data class DocumentSelection(
    val startBlockId: String,
    val startOffset: Int = 0,
    val endBlockId: String = startBlockId,
    val endOffset: Int = startOffset
) {
    val isCollapsed: Boolean
        get() = startBlockId == endBlockId && startOffset == endOffset

    val isSingleBlock: Boolean get() = startBlockId == endBlockId

    val normalizedStart: Int get() = if (isSingleBlock) minOf(startOffset, endOffset) else startOffset

    val normalizedEnd: Int get() = if (isSingleBlock) maxOf(startOffset, endOffset) else endOffset

    fun collapsedTo(blockId: String, offset: Int): DocumentSelection =
        DocumentSelection(blockId, offset, blockId, offset)

    companion object {
        fun caret(blockId: String, offset: Int = 0): DocumentSelection =
            DocumentSelection(blockId, offset, blockId, offset)
    }
}

/** Everything a toolbar button needs to render its active state — computed, never stored. */
data class ToolbarState(
    val activeFormats: FormatSet = FormatSet.EMPTY,
    val paragraphStyle: ParagraphStyle = ParagraphStyle.PARAGRAPH,
    val align: TextAlign = TextAlign.START,
    val listStyle: ListStyle? = null,
    val canIndent: Boolean = false,
    val canOutdent: Boolean = false,
    val isCodeBlock: Boolean = false,
    val isQuote: Boolean = false,
    val link: String? = null,
    val textColor: String? = null,
    val backgroundColor: String? = null,
    val fontSize: Float? = null,
    val fontWeight: Int? = null
)

data class SearchState(
    val query: String = "",
    val replacement: String = "",
    val matchCase: Boolean = false,
    val matches: List<SearchMatch> = emptyList(),
    val currentIndex: Int = -1,
    val isActive: Boolean = false
) {
    val current: SearchMatch? get() = matches.getOrNull(currentIndex)

    val hasMatches: Boolean get() = matches.isNotEmpty()
}

/** The single immutable state both platforms render. */
data class SmartTextState(
    val document: RichDocument = RichDocument.empty(),
    // defaults off `document`, never off a second RichDocument.empty() — the ids would not match
    val selection: DocumentSelection = DocumentSelection.caret(
        document.blocks.firstOrNull()?.id.orEmpty()
    ),
    val toolbar: ToolbarState = ToolbarState(),
    val history: HistoryStack = HistoryStack(),
    val search: SearchState = SearchState(),
    val isToolbarExpanded: Boolean = false,
    val showSelectionToolbar: Boolean = false,
    val clipboard: ClipboardPayload? = null,
    // styling armed by the toolbar with no selection — applied to the next characters typed
    val pendingStyle: RichSpan? = null
) {
    val canUndo: Boolean get() = history.canUndo

    val canRedo: Boolean get() = history.canRedo

    val plainText: String get() = document.plainText

    val focusedBlockId: String get() = selection.startBlockId

    companion object {
        fun of(document: RichDocument): SmartTextState {
            val blocks = document.blocks.ifEmpty { RichDocument.empty().blocks }
            val safe = document.withBlocks(blocks)
            return SmartTextState(
                document = safe,
                selection = DocumentSelection.caret(safe.blocks.first().id)
            ).let { SmartTextReducer.withRefreshedToolbar(it) }
        }
    }
}

/** Cut / copy payload — carries spans so paste keeps its formatting. */
@Serializable
data class ClipboardPayload(
    val text: String,
    val document: RichDocument? = null
)
