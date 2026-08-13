package com.app.pustakam.feature.notes.domain.editor

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

data class EditorState(
    val note: Note? = null,
    val contents: List<NoteContentModel> = emptyList(),
    val dirtyContentIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val capabilities: EditorCapabilityState = EditorCapabilityState()
) {
    val noteId: String? get() = note?.id

    val hasUnsavedContent: Boolean get() = dirtyContentIds.isNotEmpty()

    val textContents: List<NoteContentModel.TextContent>
        get() = contents.filterIsInstance<NoteContentModel.TextContent>()

    val mediaContents: List<NoteContentModel.MediaContent>
        get() = contents.filterIsInstance<NoteContentModel.MediaContent>()

    val nextPosition: Double get() = contents.size.toDouble()

    fun contentById(contentId: String): NoteContentModel? =
        contents.firstOrNull { it.id == contentId }

    fun mediaById(contentId: String): NoteContentModel.MediaContent? =
        contentById(contentId) as? NoteContentModel.MediaContent

    fun noteWithContents(): Note? = note?.withContents(contents)
}
