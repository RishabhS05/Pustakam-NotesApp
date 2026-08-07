package com.app.pustakam.core.richtext.engine

import com.app.pustakam.core.richtext.model.RichDocument
import com.app.pustakam.core.richtext.presentation.DocumentSelection

data class HistoryEntry(val document: RichDocument, val selection: DocumentSelection)

/**
 * Bounded undo/redo. Immutable so it can live inside the MVI state and travel to Swift unchanged.
 * Consecutive plain typing coalesces into one entry so undo does not step one character at a time.
 */
data class HistoryStack(
    val past: List<HistoryEntry> = emptyList(),
    val future: List<HistoryEntry> = emptyList(),
    val limit: Int = DEFAULT_LIMIT
) {
    val canUndo: Boolean get() = past.isNotEmpty()

    val canRedo: Boolean get() = future.isNotEmpty()

    fun record(entry: HistoryEntry, coalesce: Boolean = false): HistoryStack {
        val last = past.lastOrNull()
        if (last != null && last.document == entry.document) return this
        if (coalesce && last != null && isTypingContinuation(last, entry)) {
            return copy(future = emptyList())
        }
        val trimmed = (past + entry).takeLast(limit)
        return copy(past = trimmed, future = emptyList())
    }

    fun undo(current: HistoryEntry): Pair<HistoryStack, HistoryEntry>? {
        val previous = past.lastOrNull() ?: return null
        return copy(past = past.dropLast(1), future = listOf(current) + future) to previous
    }

    fun redo(current: HistoryEntry): Pair<HistoryStack, HistoryEntry>? {
        val next = future.firstOrNull() ?: return null
        return copy(past = (past + current).takeLast(limit), future = future.drop(1)) to next
    }

    fun cleared(): HistoryStack = HistoryStack(limit = limit)

    // same block, same block count, small length delta = the user is still typing the same word
    private fun isTypingContinuation(last: HistoryEntry, entry: HistoryEntry): Boolean =
        last.selection.startBlockId == entry.selection.startBlockId &&
                last.document.blocks.size == entry.document.blocks.size &&
                kotlin.math.abs(last.document.plainText.length - entry.document.plainText.length) <= COALESCE_DELTA

    companion object {
        const val DEFAULT_LIMIT = 100
        // one undo step per ~20 typed characters instead of one per keystroke
        private const val COALESCE_DELTA = 20
    }
}
