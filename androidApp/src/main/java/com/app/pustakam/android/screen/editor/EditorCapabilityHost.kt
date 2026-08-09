package com.app.pustakam.android.screen.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.app.pustakam.android.permission.AskPermissions
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.services.locationService.LocationService
import com.app.pustakam.android.extension.startServiceWrapper
import com.app.pustakam.android.widgets.alert.DeleteNoteAlert
import com.app.pustakam.android.widgets.audio.AudioRecording
import com.app.pustakam.android.widgets.importsheet.ImportFilesSheet
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.feature.notes.domain.editor.CaptureKind
import com.app.pustakam.feature.notes.domain.editor.EditorCapabilityReducer
import com.app.pustakam.feature.notes.domain.editor.EditorCapabilityState

data class EditorCapabilityCallbacks(
    val onState: (EditorCapabilityState) -> Unit,
    val onOpenCamera: (ContentType) -> Unit,
    val onAudioSaved: (NoteContentModel.MediaContent) -> Unit,
    val onFilesPicked: (Context, List<Uri>) -> Unit,
    val onImportLink: (Context, String) -> Unit,
    val onDeleteContent: (String) -> Unit,
    val onDeleteNote: () -> Unit
)

@Composable
fun EditorCapabilityHost(
    state: EditorCapabilityState,
    noteTitle: String,
    permissions: List<NeededPermission>,
    audioDraft: (Context) -> NoteContentModel.MediaContent?,
    callbacks: EditorCapabilityCallbacks
) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) callbacks.onFilesPicked(context, uris)
        callbacks.onState(EditorCapabilityReducer.captureFinished(state))
    }

    if (state.isPermissionPromptVisible) {
        AskPermissions(
            permissionsRequired = permissions,
            onDismiss = { callbacks.onState(EditorCapabilityReducer.denied(state)) },
            onGrantPermission = {
                val granted = EditorCapabilityReducer.granted(state)
                callbacks.onState(granted)
                state.pendingCapture
                    ?.takeIf { it.opensCamera }
                    ?.let { callbacks.onOpenCamera(it.contentType) }
            }
        )
    }

    if (state.isTrackingLocation) {
        LaunchedEffect(Unit) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            (context as Activity).startServiceWrapper(intent = intent)
            callbacks.onState(EditorCapabilityReducer.stopLocation(state))
        }
    }

    if (state.isRecordingAudio) {
        val draft = remember(state.isRecordingAudio) { audioDraft(context) }
        if (draft != null) {
            AudioRecording(
                noteContentModel = draft,
                onStop = {
                    callbacks.onAudioSaved(it)
                    callbacks.onState(EditorCapabilityReducer.stopAudio(state))
                },
                onDelete = { callbacks.onState(EditorCapabilityReducer.stopAudio(state)) }
            )
        }
    }

    if (state.isImportSheetVisible) {
        ImportFilesSheet(
            onDismiss = {
                callbacks.onState(EditorCapabilityReducer.setImportSheet(state, false))
            },
            onPickFromDevice = { filePicker.launch(arrayOf("*/*")) },
            onImportFromLink = { link ->
                callbacks.onImportLink(context, link)
                callbacks.onState(EditorCapabilityReducer.setImportSheet(state, false))
            }
        )
    }

    state.pendingDeleteContentId?.let { contentId ->
        DeleteNoteAlert(
            noteTitle = "this block",
            onConfirm = {
                callbacks.onDeleteContent(contentId)
                callbacks.onState(EditorCapabilityReducer.dismissDelete(state))
            },
            onDismiss = { callbacks.onState(EditorCapabilityReducer.dismissDelete(state)) }
        )
    }

    if (state.isDeleteNotePromptVisible) {
        DeleteNoteAlert(
            noteTitle = noteTitle,
            onConfirm = {
                callbacks.onDeleteNote()
                callbacks.onState(EditorCapabilityReducer.dismissDelete(state))
            },
            onDismiss = { callbacks.onState(EditorCapabilityReducer.dismissDelete(state)) }
        )
    }
}

fun permissionsFor(kind: CaptureKind?): List<NeededPermission> = when (kind) {
    CaptureKind.VIDEO -> listOf(NeededPermission.CAMERA, NeededPermission.RECORD_AUDIO)
    CaptureKind.AUDIO -> listOf(NeededPermission.RECORD_AUDIO)
    CaptureKind.IMAGE -> listOf(NeededPermission.CAMERA)
    CaptureKind.LOCATION -> listOf(
        NeededPermission.COARSE_LOCATION,
        NeededPermission.FINE_LOCATION,
        NeededPermission.BACKGROUND_LOCATION
    )

    else -> listOf(NeededPermission.POST_NOTIFICATIONS)
}
