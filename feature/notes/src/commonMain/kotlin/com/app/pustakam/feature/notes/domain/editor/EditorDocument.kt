package com.app.pustakam.feature.notes.domain.editor

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.feature.notes.domain.history.NoteEditKind
import com.app.pustakam.feature.notes.domain.history.NoteHistory

data class EditorDocument(
    val note: Note? = null,
    val contents: List<NoteContentModel> = emptyList(),
    val title: String = "",
    val dirtyContentIds: Set<String> = emptySet(),
    val history: NoteHistory = NoteHistory(),
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
) {
    val noteId: String? get() = note?.id

    val isReady: Boolean get() = note != null

    val hasUnsavedChanges: Boolean get() = dirtyContentIds.isNotEmpty()

    val canUndo: Boolean get() = history.canUndo

    val canRedo: Boolean get() = history.canRedo

    val textContents: List<NoteContentModel.TextContent>
        get() = contents.filterIsInstance<NoteContentModel.TextContent>()

    fun contentById(contentId: String?): NoteContentModel? =
        contentId?.let { id -> contents.firstOrNull { it.id == id } }

    fun textContentById(contentId: String?): NoteContentModel.TextContent? =
        contentById(contentId) as? NoteContentModel.TextContent

    fun indexOf(contentId: String): Int = contents.indexOfFirst { it.id == contentId }

    fun snapshot(): Note? = note?.withTitleAndContents(title.ifBlank { note.title }, contents)

    val isValid: Boolean
        get() = note != null && (title.isNotBlank() || contents.isNotEmpty())
}

object EditorDocumentReducer {

    fun loading(state: EditorDocument): EditorDocument =
        state.copy(isLoading = true, errorMessage = null)

    fun loaded(state: EditorDocument, note: Note): EditorDocument {
        if (state.hasUnsavedChanges) {
            return state.copy(note = note, isLoading = false, errorMessage = null)
        }
        return state.copy(
            note = note,
            contents = note.contents,
            title = note.title.orEmpty(),
            isLoading = false,
            errorMessage = null
        )
    }

    fun failed(state: EditorDocument, message: String?): EditorDocument =
        state.copy(isLoading = false, errorMessage = message)

    fun clearError(state: EditorDocument): EditorDocument = state.copy(errorMessage = null)

    fun titled(state: EditorDocument, title: String): EditorDocument =
        state.copy(title = title, history = recordOn(state, NoteEditKind.TITLE))

    fun addContent(
        state: EditorDocument,
        content: NoteContentModel,
        kind: NoteEditKind = NoteEditKind.ADD_TEXT
    ): EditorDocument {
        if (state.indexOf(content.id) >= 0) return updateContent(state, content, kind)
        return state.copy(
            contents = state.contents + content,
            dirtyContentIds = state.dirtyContentIds + content.id,
            history = recordOn(state, kind)
        )
    }

    fun updateContent(
        state: EditorDocument,
        content: NoteContentModel,
        kind: NoteEditKind = NoteEditKind.TEXT
    ): EditorDocument {
        val index = state.indexOf(content.id)
        if (index < 0) return addContent(state, content, kind)
        val updated = state.contents.toMutableList().apply { set(index, content) }
        return state.copy(
            contents = updated,
            dirtyContentIds = state.dirtyContentIds + content.id,
            history = recordOn(state, kind)
        )
    }

    fun removeContent(state: EditorDocument, contentId: String): EditorDocument {
        if (state.indexOf(contentId) < 0) return state
        return state.copy(
            contents = state.contents.filterNot { it.id == contentId },
            dirtyContentIds = state.dirtyContentIds + contentId,
            history = recordOn(state, NoteEditKind.DELETE_CONTENT)
        )
    }

    fun replaceContents(
        state: EditorDocument,
        contents: List<NoteContentModel>,
        kind: NoteEditKind = NoteEditKind.REORDER
    ): EditorDocument = state.copy(
        contents = contents,
        dirtyContentIds = state.dirtyContentIds + contents.map { it.id },
        history = recordOn(state, kind)
    )

    fun undo(state: EditorDocument): EditorDocument {
        val current = state.snapshot() ?: return state
        val step = state.history.undoStep(current) ?: return state
        return restored(state, step.note).copy(history = step.history)
    }

    fun redo(state: EditorDocument): EditorDocument {
        val current = state.snapshot() ?: return state
        val step = state.history.redoStep(current) ?: return state
        return restored(state, step.note).copy(history = step.history)
    }

    fun markSaved(state: EditorDocument, savedIds: Set<String>): EditorDocument =
        state.copy(dirtyContentIds = state.dirtyContentIds - savedIds)

    fun markAllDirty(state: EditorDocument): EditorDocument =
        state.copy(dirtyContentIds = state.contents.map { it.id }.toSet())

    fun deleted(state: EditorDocument): EditorDocument =
        state.copy(isDeleted = true, dirtyContentIds = emptySet())

    private fun restored(state: EditorDocument, note: Note): EditorDocument = state.copy(
        note = note,
        contents = note.contents,
        title = note.title.orEmpty(),
        dirtyContentIds = state.dirtyContentIds + note.contents.map { it.id }
    )

    private fun recordOn(state: EditorDocument, kind: NoteEditKind): NoteHistory =
        state.snapshot()?.let { state.history.record(it, kind) } ?: state.history
}

object EditorCommands {

    fun empty(): EditorDocument = EditorDocument()

    fun of(note: Note): EditorDocument = EditorDocumentReducer.loaded(EditorDocument(), note)

    fun textKind(): NoteEditKind = NoteEditKind.TEXT

    fun formattingKind(): NoteEditKind = NoteEditKind.FORMATTING

    fun addTextKind(): NoteEditKind = NoteEditKind.ADD_TEXT

    fun addMediaKind(): NoteEditKind = NoteEditKind.ADD_MEDIA

    fun addDocumentKind(): NoteEditKind = NoteEditKind.ADD_DOCUMENT

    fun deleteKind(): NoteEditKind = NoteEditKind.DELETE_CONTENT

    fun reorderKind(): NoteEditKind = NoteEditKind.REORDER

    fun dirtyIds(state: EditorDocument): List<String> = state.dirtyContentIds.toList()

    fun contentsOf(state: EditorDocument): List<NoteContentModel> = state.contents

    fun textContentsOf(state: EditorDocument): List<NoteContentModel.TextContent> =
        state.textContents
}
