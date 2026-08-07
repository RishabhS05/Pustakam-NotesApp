package com.app.pustakam.feature.notes.domain.history

import com.app.pustakam.core.model.models.response.notes.Note

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

data class NoteHistoryStep(val history: NoteHistory, val note: Note)

data class NoteHistory(
    val past: List<Note> = emptyList(),
    val future: List<Note> = emptyList(),
    val limit: Int = DEFAULT_LIMIT
) {
    val canUndo: Boolean get() = past.isNotEmpty()

    val canRedo: Boolean get() = future.isNotEmpty()

    fun record(previous: Note, kind: NoteEditKind): NoteHistory {
        if (!isUndoable(kind)) return this
        if (past.lastOrNull()?.let { sameContent(it, previous) } == true) return this
        return copy(past = (past + previous).takeLast(limit), future = emptyList())
    }

    fun recordText(previous: Note): NoteHistory = record(previous, NoteEditKind.TEXT)

    fun recordFormatting(previous: Note): NoteHistory = record(previous, NoteEditKind.FORMATTING)

    fun recordTitle(previous: Note): NoteHistory = record(previous, NoteEditKind.TITLE)

    fun recordAddText(previous: Note): NoteHistory = record(previous, NoteEditKind.ADD_TEXT)

    fun recordDeleteContent(previous: Note): NoteHistory =
        record(previous, NoteEditKind.DELETE_CONTENT)

    fun recordAddMedia(previous: Note): NoteHistory = record(previous, NoteEditKind.ADD_MEDIA)

    fun recordAddDocument(previous: Note): NoteHistory =
        record(previous, NoteEditKind.ADD_DOCUMENT)

    fun undoStep(current: Note): NoteHistoryStep? {
        val previous = past.lastOrNull() ?: return null
        return NoteHistoryStep(
            history = copy(past = past.dropLast(1), future = listOf(current) + future),
            note = previous
        )
    }

    fun redoStep(current: Note): NoteHistoryStep? {
        val next = future.firstOrNull() ?: return null
        return NoteHistoryStep(
            history = copy(past = (past + current).takeLast(limit), future = future.drop(1)),
            note = next
        )
    }

    fun cleared(): NoteHistory = NoteHistory(limit = limit)

    companion object {
        const val DEFAULT_LIMIT = 50

        fun isUndoable(kind: NoteEditKind): Boolean =
            kind != NoteEditKind.ADD_MEDIA && kind != NoteEditKind.ADD_DOCUMENT

        private fun sameContent(a: Note, b: Note): Boolean =
            a.title == b.title && a.contents == b.contents
    }
}
