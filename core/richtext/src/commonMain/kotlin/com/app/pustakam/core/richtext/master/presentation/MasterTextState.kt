package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.model.EditableSegment
import com.app.pustakam.core.richtext.model.FormatSet
import com.app.pustakam.core.richtext.model.ListStyle
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.model.RichSpan
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.model.segments
import com.app.pustakam.core.richtext.presentation.ToolbarState
import kotlinx.serialization.Serializable

@Serializable
data class MasterTextSelection(val start: Int = 0, val end: Int = start) {

    val isCollapsed: Boolean get() = start == end

    val normalizedStart: Int get() = minOf(start, end)

    val normalizedEnd: Int get() = maxOf(start, end)

    val length: Int get() = normalizedEnd - normalizedStart

    fun coercedIn(textLength: Int): MasterTextSelection = MasterTextSelection(
        start = start.coerceIn(0, textLength),
        end = end.coerceIn(0, textLength)
    )

    companion object {
        fun caret(offset: Int): MasterTextSelection = MasterTextSelection(offset, offset)
    }
}

data class MasterTextHistoryEntry(
    val document: RichDocument,
    val selection: MasterTextSelection
)

data class MasterTextHistory(
    val past: List<MasterTextHistoryEntry> = emptyList(),
    val future: List<MasterTextHistoryEntry> = emptyList(),
    val limit: Int = DEFAULT_LIMIT
) {
    val canUndo: Boolean get() = past.isNotEmpty()

    val canRedo: Boolean get() = future.isNotEmpty()

    fun record(entry: MasterTextHistoryEntry, coalesce: Boolean = false): MasterTextHistory {
        val last = past.lastOrNull()
        if (last != null && last.document == entry.document) return this
        if (coalesce && last != null && isTypingRun(last, entry)) return copy(future = emptyList())
        return copy(past = (past + entry).takeLast(limit), future = emptyList())
    }

    fun undo(current: MasterTextHistoryEntry): Pair<MasterTextHistory, MasterTextHistoryEntry>? {
        val previous = past.lastOrNull() ?: return null
        return copy(past = past.dropLast(1), future = listOf(current) + future) to previous
    }

    fun redo(current: MasterTextHistoryEntry): Pair<MasterTextHistory, MasterTextHistoryEntry>? {
        val next = future.firstOrNull() ?: return null
        return copy(past = (past + current).takeLast(limit), future = future.drop(1)) to next
    }

    private fun isTypingRun(
        last: MasterTextHistoryEntry,
        entry: MasterTextHistoryEntry
    ): Boolean = kotlin.math.abs(
        last.document.plainText.length - entry.document.plainText.length
    ) <= COALESCE_DELTA

    companion object {
        const val DEFAULT_LIMIT = 120
        private const val COALESCE_DELTA = 20
    }
}

data class MasterTextState(
    val document: RichDocument = RichDocument.empty(),
    val selection: MasterTextSelection = MasterTextSelection(),
    val toolbar: ToolbarState = ToolbarState(),
    val history: MasterTextHistory = MasterTextHistory(),
    val pendingStyle: RichSpan? = null,
    val isToolbarExpanded: Boolean = false,
    val showSelectionToolbar: Boolean = false,
    val toolbarDismissed: Boolean = false
) {
    val segment: EditableSegment
        get() = document.segments().firstOrNull()
            ?: EditableSegment(id = "", text = "", paragraphs = emptyList())

    val text: String get() = segment.text

    val canUndo: Boolean get() = history.canUndo

    val canRedo: Boolean get() = history.canRedo

    val activeFormats: FormatSet get() = toolbar.activeFormats

    val paragraphStyle: ParagraphStyle get() = toolbar.paragraphStyle

    val align: TextAlign get() = toolbar.align

    val listStyle: ListStyle? get() = toolbar.listStyle

    companion object {
        fun of(document: RichDocument): MasterTextState =
            MasterTextReducer.withRefreshedToolbar(MasterTextState(document = document))
    }
}
