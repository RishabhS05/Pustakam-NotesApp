package com.app.pustakam.feature.notes.domain.editor

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

sealed interface EditorEffect {

    data class ReadNote(val noteId: String?) : EditorEffect

    data class SaveNote(val note: Note, val dirtyContentIds: Set<String>) : EditorEffect

    data class DeleteContentRow(val contentId: String) : EditorEffect

    data class DeleteFiles(val paths: List<String>) : EditorEffect

    data class MakeThumbnail(val content: NoteContentModel.MediaContent) : EditorEffect

    data class PublishMedia(val content: NoteContentModel.MediaContent) : EditorEffect

    data class OpenCamera(val noteId: String, val type: ContentType) : EditorEffect

    data object StartRecorder : EditorEffect

    data object OpenImportPicker : EditorEffect
}
