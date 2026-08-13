package com.app.pustakam.feature.notes.domain.editor

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

sealed interface EditorIntent {

    data class Load(val noteId: String?) : EditorIntent

    data object Refresh : EditorIntent

    data object LoadStarted : EditorIntent

    data class NoteLoaded(val note: Note) : EditorIntent

    data class LoadFailed(val message: String) : EditorIntent

    data class AddContent(val content: NoteContentModel) : EditorIntent

    data class AddContents(val contents: List<NoteContentModel>) : EditorIntent

    data class UpdateContent(val content: NoteContentModel) : EditorIntent

    data class RemoveContent(val contentId: String) : EditorIntent

    data class ThumbnailReady(val contentId: String, val thumbnailPath: String) : EditorIntent

    data object SaveRequested : EditorIntent

    data class Saved(val note: Note, val savedContentIds: Set<String>) : EditorIntent

    data class CaptureRequested(val type: ContentType) : EditorIntent

    data object PermissionGranted : EditorIntent

    data class PermissionDenied(val type: ContentType) : EditorIntent

    data object CaptureFinished : EditorIntent

    data class ShowImportSheet(val visible: Boolean) : EditorIntent

    data class ShowAttachSheet(val visible: Boolean) : EditorIntent

    data class AskDeleteContent(val contentId: String) : EditorIntent

    data object AskDeleteNote : EditorIntent

    data object DismissDelete : EditorIntent

    data class ExternalContentsChanged(val contents: List<NoteContentModel>) : EditorIntent

    data object ClearError : EditorIntent
}
