package com.app.pustakam.feature.notes.domain.editor

import com.app.pustakam.core.common.util.ContentType

enum class CaptureKind {
    IMAGE,
    VIDEO,
    AUDIO,
    LOCATION,
    FILE,
    LINK;

    val contentType: ContentType
        get() = when (this) {
            IMAGE -> ContentType.IMAGE
            VIDEO -> ContentType.VIDEO
            AUDIO -> ContentType.AUDIO
            LOCATION -> ContentType.LOCATION
            FILE -> ContentType.PDF
            LINK -> ContentType.LINK
        }

    val needsPermission: Boolean
        get() = this == IMAGE || this == VIDEO || this == AUDIO || this == LOCATION

    val opensCamera: Boolean get() = this == IMAGE || this == VIDEO
}

data class EditorCapabilityState(
    val pendingCapture: CaptureKind? = null,
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

    fun requestCapture(state: EditorCapabilityState, kind: CaptureKind): EditorCapabilityState =
        if (kind.needsPermission) {
            state.copy(pendingCapture = kind, isPermissionPromptVisible = true)
        } else {
            granted(state.copy(pendingCapture = kind))
        }

    fun granted(state: EditorCapabilityState): EditorCapabilityState {
        val kind = state.pendingCapture ?: return state.copy(isPermissionPromptVisible = false)
        val cleared = state.copy(isPermissionPromptVisible = false)
        return when (kind) {
            CaptureKind.AUDIO -> cleared.copy(isRecordingAudio = true)
            CaptureKind.LOCATION -> cleared.copy(isTrackingLocation = true)
            CaptureKind.FILE, CaptureKind.LINK -> cleared.copy(isImportSheetVisible = true)
            CaptureKind.IMAGE, CaptureKind.VIDEO -> cleared
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

    fun image(): CaptureKind = CaptureKind.IMAGE

    fun video(): CaptureKind = CaptureKind.VIDEO

    fun audio(): CaptureKind = CaptureKind.AUDIO

    fun location(): CaptureKind = CaptureKind.LOCATION

    fun file(): CaptureKind = CaptureKind.FILE

    fun link(): CaptureKind = CaptureKind.LINK

    fun opensCamera(state: EditorCapabilityState): Boolean =
        state.pendingCapture?.opensCamera == true

    fun pendingContentType(state: EditorCapabilityState): ContentType? =
        state.pendingCapture?.contentType
}
