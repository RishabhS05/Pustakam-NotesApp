package com.app.pustakam.feature.notes.domain.editor

import com.app.pustakam.core.common.util.ContentType

data class EditorCapabilityState(
    val pendingCapture: ContentType? = null,
    val isPermissionPromptVisible: Boolean = false,
    val isRecordingAudio: Boolean = false,
    val isTrackingLocation: Boolean = false,
    val isImportSheetVisible: Boolean = false,
    val isAttachSheetVisible: Boolean = false,
    val isExporting: Boolean = false,
    val pendingDeleteContentId: String? = null,
    val isDeleteNotePromptVisible: Boolean = false
) {
    val isDeletePromptVisible: Boolean
        get() = isDeleteNotePromptVisible || pendingDeleteContentId != null

    val isBusy: Boolean get() = isExporting || isRecordingAudio
}

object EditorCapabilityReducer {

    fun needsPermission(type: ContentType): Boolean = when (type) {
        ContentType.IMAGE, ContentType.VIDEO, ContentType.AUDIO, ContentType.LOCATION -> true
        else -> false
    }

    fun opensCamera(type: ContentType): Boolean =
        type == ContentType.IMAGE || type == ContentType.VIDEO

    fun requestCapture(state: EditorCapabilityState, type: ContentType): EditorCapabilityState =
        if (needsPermission(type)) {
            state.copy(pendingCapture = type, isPermissionPromptVisible = true)
        } else {
            granted(state.copy(pendingCapture = type))
        }

    fun granted(state: EditorCapabilityState): EditorCapabilityState {
        val type = state.pendingCapture ?: return state.copy(isPermissionPromptVisible = false)
        val cleared = state.copy(isPermissionPromptVisible = false)
        return when (type) {
            ContentType.AUDIO -> cleared.copy(isRecordingAudio = true)
            ContentType.LOCATION -> cleared.copy(isTrackingLocation = true)
            else -> cleared
        }
    }

    fun denied(state: EditorCapabilityState): EditorCapabilityState =
        state.copy(pendingCapture = null, isPermissionPromptVisible = false)

    fun captureFinished(state: EditorCapabilityState): EditorCapabilityState = state.copy(
        pendingCapture = null,
        isRecordingAudio = false,
        isTrackingLocation = false,
        isImportSheetVisible = false,
        isAttachSheetVisible = false
    )

    fun stopAudio(state: EditorCapabilityState): EditorCapabilityState =
        state.copy(isRecordingAudio = false, pendingCapture = null)

    fun stopLocation(state: EditorCapabilityState): EditorCapabilityState =
        state.copy(isTrackingLocation = false, pendingCapture = null)

    fun setAttachSheet(state: EditorCapabilityState, visible: Boolean): EditorCapabilityState =
        state.copy(isAttachSheetVisible = visible)

    fun setImportSheet(state: EditorCapabilityState, visible: Boolean): EditorCapabilityState =
        state.copy(isImportSheetVisible = visible, isAttachSheetVisible = false)

    fun setExporting(state: EditorCapabilityState, exporting: Boolean): EditorCapabilityState =
        state.copy(isExporting = exporting)

    fun askDeleteContent(state: EditorCapabilityState, contentId: String): EditorCapabilityState =
        state.copy(pendingDeleteContentId = contentId)

    fun askDeleteNote(state: EditorCapabilityState): EditorCapabilityState =
        state.copy(isDeleteNotePromptVisible = true)

    fun dismissDelete(state: EditorCapabilityState): EditorCapabilityState =
        state.copy(pendingDeleteContentId = null, isDeleteNotePromptVisible = false)
}

object EditorCapabilityCommands {

    fun empty(): EditorCapabilityState = EditorCapabilityState()
}
