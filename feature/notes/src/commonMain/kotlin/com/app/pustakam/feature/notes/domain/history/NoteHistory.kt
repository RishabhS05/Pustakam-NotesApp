package com.app.pustakam.feature.notes.domain.history

import com.app.pustakam.core.model.models.response.notes.Note

/** What kind of edit produced a snapshot — decides whether it can be undone at all. */
enum class NoteEditKind {
    TEXT,
    FORMATTING,
    TITLE,
    ADD_TEXT,
    DELETE_CONTENT,
    REORDER,
    ADD_MEDIA,
    ADD_DOCUMENT
}

/**
 * Note-wide undo/redo. Snapshots the whole [Note] so a single stack covers typing, formatting,
 * the title, adding a text block and deleting content.
 *
 * Capturing a photo or importing a document is deliberately NOT undoable: the file already exists
 * on disk and rolling the note back would orphan it, so those two kinds are dropped instead of
 * being recorded — undo then steps past them to the last text edit.
 */
data class NoteHistory(
    val past: List<Note> = emptyList(),
    val future: List<Note> = emptyList(),
    val limit: Int = DEFAULT_LIMIT
) {
    val canUndo: Boolean get() = past.isNotEmpty()

    val canRedo: Boolean get() = future.isNotEmpty()

    /** Call with the note as it was BEFORE the edit is applied. */
    fun record(previous: Note, kind: NoteEditKind): NoteHistory {
        if (!isUndoable(kind)) return this
        if (past.lastOrNull()?.let { sameContent(it, previous) } == true) return this
        return copy(past = (past + previous).takeLast(limit), future = emptyList())
    }

    fun undo(current: Note): Pair<NoteHistory, Note>? {
        val previous = past.lastOrNull() ?: return null
        return copy(past = past.dropLast(1), future = listOf(current) + future) to previous
    }

    fun redo(current: Note): Pair<NoteHistory, Note>? {
        val next = future.firstOrNull() ?: return null
        return copy(past = (past + current).takeLast(limit), future = future.drop(1)) to next
    }

    fun cleared(): NoteHistory = NoteHistory(limit = limit)

    companion object {
        const val DEFAULT_LIMIT = 50

        fun isUndoable(kind: NoteEditKind): Boolean =
            kind != NoteEditKind.ADD_MEDIA && kind != NoteEditKind.ADD_DOCUMENT

        /** updatedAt changes on every edit, so equality has to ignore it. */
        private fun sameContent(a: Note, b: Note): Boolean =
            a.title == b.title && a.contents == b.contents
    }
}
