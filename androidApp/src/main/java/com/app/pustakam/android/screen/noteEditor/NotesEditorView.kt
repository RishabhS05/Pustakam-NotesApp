package com.app.pustakam.android.screen.noteEditor

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.permission.AskPermissions
import com.app.pustakam.android.screen.NoteContentUiState
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.alert.DeleteNoteAlert
import com.app.pustakam.android.widgets.audio.AudioPlayerUIState
import com.app.pustakam.android.widgets.audio.AudioRecording
import com.app.pustakam.android.widgets.fabWidget.OverLayEditorButtons
import com.app.pustakam.android.widgets.image.ImageCard
import com.app.pustakam.android.widgets.textField.NoteTextField
import com.app.pustakam.android.widgets.video.VideoCard
import com.app.pustakam.data.models.CameraData
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.getMediaUrl
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.extensions.toLocalFormat
import com.app.pustakam.util.ContentType
import kotlinx.coroutines.flow.MutableStateFlow

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    id: String? = null,
    noteEditorViewModel: NoteEditorViewModel = viewModel(),
    imageDataViewModel: ImageDataViewModel = viewModel(),
    onBack: () -> Unit = {},
    navigateTo: (Any) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                noteEditorViewModel.changeNoteStatus(null)
                noteEditorViewModel.readFromDataBase(id)
            }

            else -> {}
        }
    }
    BackHandler {
        /**  block direct exit as my scope is getting distroyed  */
        noteEditorViewModel.changeNoteStatus(NoteStatus.onBackPress)
    }

    val state = noteEditorViewModel.noteContentUiState.collectAsStateWithLifecycle()
    val stateEditor = noteEditorViewModel.noteUIState.collectAsStateWithLifecycle().value.apply {
        when {
            isLoading -> LoadingUI()
            error.isNotnull() -> SnackBarUi(error = error!!) {
                noteEditorViewModel.clearError()
            }

            showPermissionAlert == true -> {
                AskPermissions(permissionsRequired = permissions, onDismiss = {
                    noteEditorViewModel.showPermissionAlert(null)
                }, onGrantPermission = {
                    noteEditorViewModel.showPermissionAlert(null)
                    when (contentType) {
                        ContentType.IMAGE, ContentType.VIDEO -> navigateTo(CameraData(noteEditorViewModel.noteContentUiState.value.note?.id!!))
                        ContentType.AUDIO -> noteEditorViewModel.startStopAudioRecording()
                        else -> {}
                    }
                })
            }

            showDeleteAlert -> {
                if (deleteNoteContentId.isNotnull()) {
                    DeleteNoteAlert(noteTitle = "Recorded Note", onConfirm = {
                        noteEditorViewModel.removeContent(value = deleteNoteContentId!!)
                    }) {
                        noteEditorViewModel.showDeleteAlertBox(false, null)
                    }
                } else {
                    DeleteNoteAlert(noteTitle = if (!noteEditorViewModel.noteContentUiState.value.note?.title.isNullOrEmpty()) noteEditorViewModel.noteContentUiState.value.note?.title!! else "",
                        onConfirm = {
                            noteEditorViewModel.deleteNote(noteId = id!!)
                            noteEditorViewModel.showDeleteAlertBox(false)
                        }) {
                        noteEditorViewModel.showDeleteAlertBox(false)
                    }
                }
            }
        }
    }.also {
        when (it.noteStatus) {
            NoteStatus.onBackPress -> noteEditorViewModel.createOrUpdateNote()
            NoteStatus.onSaveCompletedExit, NoteStatus.exit -> onBack()
            else -> {}
        }
    }
    imageDataViewModel.paths.collectAsStateWithLifecycle().value.apply {
        noteEditorViewModel.getMediaData(this)
        imageDataViewModel.clearPaths()
    }
    NotesEditor(state = state, topBar = {
        TopAppBar(title = {
            state.value.note?.updatedAt?.let {
                Text(
                    it.toLocalFormat(), style = typography.titleMedium
                )
            }
        }, actions = {
            IconButton(onClick = noteEditorViewModel::createOrUpdateNote) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save",
                )
            }
            IconButton(onClick = noteEditorViewModel::createOrUpdateNote) {
                Icon(
                    imageVector = Icons.Default.SaveAs,
                    contentDescription = "Save As",
                )
            }
            IconButton(onClick = noteEditorViewModel::shareNote) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share Note",
                )
            }
            if (stateEditor.showDeleteButton) IconButton(onClick = {
                noteEditorViewModel.showDeleteAlertBox(true)
            }) {
                Icon(
                    Icons.Default.Delete, tint = colorScheme.error,
                    contentDescription = "Delete a note",
                )
            }
        })
    }, onButtonOverLays = {
        Box(Modifier.fillMaxSize()) {
            OverLayEditorButtons(
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
                onAddTextField = {
                    noteEditorViewModel.addNewText()
                },
                onArrowButton = { focusManager.clearFocus() },
                onRecordMic = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.AUDIO)
                },
                onCameraAction = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.IMAGE)
                },
            )
        }
    }, contentList = { focusRequester ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn {
                state.value.contents.let {
                    itemsIndexed(it.sortedBy { content -> content.position.inc() }) { index, contentValue ->
                        RenderWidget(content = contentValue, focusRequester = focusRequester, onUpdate = { value ->
                            noteEditorViewModel.updateContent(index, value)
                        }, onDelete = { value ->
                            noteEditorViewModel.showDeleteAlertBox(true, deleteNoteContentId = value.id)
                        }, onMediaPreview = {
                            imageDataViewModel.onSetMediaToPreview(
                                (contentValue as NoteContentModel.MediaContent).getMediaUrl(), contentValue.type
                            )
                            when {
                                contentValue.type == ContentType.IMAGE -> navigateTo(Route.ImagePreview)
                                contentValue.type == ContentType.VIDEO -> navigateTo(Route.VideoPreview)
                            }
                        })
                    }
                }
            }
            if(stateEditor.showAudioRecorder)
            AudioRecording(
                modifier =  Modifier.align(Alignment.TopEnd),
                noteContentModel = noteEditorViewModel.addNewContent(context,
                    contentType = ContentType.AUDIO) as NoteContentModel.MediaContent,
                onStop = {
                    noteEditorViewModel.updateContent(content = it)
                         noteEditorViewModel.startStopAudioRecording(false)},
            )
        }
    })


}

@Composable
fun rememberFocusRequester() = remember { FocusRequester() }

@Composable
fun NotesEditor(
    state: State<NoteContentUiState>,
    topBar: @Composable () -> Unit,
    contentList: @Composable (FocusRequester) -> Unit,
    onButtonOverLays: @Composable () -> Unit,
) {
    val isRuledEnabledState = remember { mutableStateOf(false) }
    val focusRequester = rememberFocusRequester()
    val focusManager = LocalFocusManager.current
    val paddingLeft = if (isRuledEnabledState.value) 100.dp else 12.dp
    Scaffold(topBar = topBar) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)

        ) {
            if (isRuledEnabledState.value) RuledPage()
            Column {
                TextField(value = state.value.titleTextState.value, textStyle = typography.titleLarge, placeholder = {
                    Text(
                        "Title : Keep your thoughts alive.",
                        modifier = Modifier.padding(start = paddingLeft),
                    )
                }, colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colorScheme.tertiary
                ), onValueChange = {
                    state.value.titleTextState.value = it
                }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).padding(top = 2.dp)
                )
                HorizontalDivider(color = colorScheme.outline, thickness = 2.dp)
                contentList(focusRequester)
            }
            onButtonOverLays()
        }
    }

}

@Composable
fun RenderWidget(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = rememberFocusRequester(),
    content: NoteContentModel,
    onUpdate: (content: NoteContentModel) -> Unit,
    onDelete: (content: NoteContentModel) -> Unit,
    onMediaPreview: () -> Unit,
) {
    when (content.type) {
        ContentType.TEXT -> {
            NoteTextField(noteContentModel = (content as NoteContentModel.TextContent),
                focusRequester = focusRequester,
                onUpdate = { onUpdate(content.copy(text = it)) })
        }

        ContentType.IMAGE -> {
            val contentImage = content as NoteContentModel.MediaContent
            val path = contentImage.localPath ?: contentImage.url
            ImageCard(imageUrl = path, modifier = Modifier, onClick = onMediaPreview)
        }

        ContentType.VIDEO -> {
            val contentVideo = content as NoteContentModel.MediaContent
            VideoCard(contentVideo, onClick = onMediaPreview)
        }

        ContentType.AUDIO -> {
            val contentAudio = content as NoteContentModel.MediaContent
            AudioPlayerUIState(contentAudio, onDelete = onDelete)
        }

        ContentType.LINK -> {
            val contentLink = content as NoteContentModel.Link
            Text(contentLink.url, modifier = Modifier.clickable {})
        }

        ContentType.DOCX -> {
            val contentDoc = content as NoteContentModel.MediaContent
            val path = contentDoc.localPath ?: contentDoc.url
        }

        ContentType.LOCATION -> {
            val locationContent = content as NoteContentModel.Location
        }

        ContentType.PDF -> {}
        ContentType.GIF -> {}
    }
}

@Composable
fun RuledPage() {
    val lineColor = Color.LightGray
    val marginColor = Color.Red
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 35.dp.toPx()
        val startX = 80.dp.toPx()
        var y = lineSpacing + 80
        while (y < size.height) {
            drawLine(
                color = lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx()
            )
            y += lineSpacing
        }
        drawLine(
            color = marginColor, start = Offset(startX, 0f), end = Offset(startX, size.height), strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = marginColor, start = Offset(startX + 20f, 0f), end = Offset(startX + 20f, size.height), strokeWidth = 2.dp.toPx()
        )
    }
}


@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun NoteEditorPreview() {
    MyApplicationTheme {
        val state = MutableStateFlow(NoteContentUiState()).collectAsStateWithLifecycle()
        NotesEditor(state = state, topBar = {}, onButtonOverLays = {
            OverLayEditorButtons()
        }, contentList = {})
    }
}