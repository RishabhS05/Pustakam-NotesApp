package com.app.pustakam.android.screen.noteEditor

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.permission.AskPermissions
import com.app.pustakam.android.screen.NoteContentUiState
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.LoadImage
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.alert.DeleteNoteAlert
import com.app.pustakam.android.widgets.audio.AudioPlayerUIState
import com.app.pustakam.android.widgets.audio.AudioRecording
import com.app.pustakam.android.widgets.fabWidget.OverLayEditorButtons
import com.app.pustakam.android.widgets.textField.NoteTextField
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.extensions.toLocalFormat
import com.app.pustakam.util.ContentType
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditorView(
    id: String? = null,
    onBack: () -> Unit = {},
) {
    val noteEditorViewModel: NoteEditorViewModel = viewModel()
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
                    noteEditorViewModel.addNewContent(context)
                })
            }

            showDeleteAlert -> {
                DeleteNoteAlert(noteTitle = if (!noteEditorViewModel.noteContentUiState.value.note?.title.isNullOrEmpty()) noteEditorViewModel.noteContentUiState.value.note?.title!! else "", onConfirm = {
                    noteEditorViewModel.deleteNote(noteId = id!!)
                    noteEditorViewModel.showDeleteAlertBox(false)
                }) {
                    noteEditorViewModel.showDeleteAlertBox(false)
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
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = "Save",
                )
            }
            IconButton(onClick = noteEditorViewModel::createOrUpdateNote) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_save_as),
                    contentDescription = "Save As",
                )
            }
            IconButton(onClick = noteEditorViewModel::createOrUpdateNote) {
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
                    noteEditorViewModel.setContentType(contentType = ContentType.TEXT)
                    noteEditorViewModel.addNewContent(context)
                },
                onArrowButton = { focusManager.clearFocus() },
                onRecordVideo = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.VIDEO)
                },
                onRecordMic = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.AUDIO)
                },
                onAddImage = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.IMAGE)
                },
            )
        }
    }, contentList = {
        LazyColumn {
            state.value.contents.let {
                itemsIndexed(it.sortedBy { content -> content.position.inc() }) { index, contentValue ->
                    RenderWidget(content = contentValue, onUpdate = { value ->
                        noteEditorViewModel.updateContent(index, value)
                    }){value->
                        noteEditorViewModel.removeContent(value)
                    }
                }
            }
        }
    })
}

@Composable
fun rememberFocusRequester() = remember { FocusRequester() }

@Composable
fun NotesEditor(
    state: State<NoteContentUiState>,
    topBar: @Composable () -> Unit,
    contentList: @Composable () -> Unit,
    onButtonOverLays: @Composable () -> Unit,
) {
    // Note content state
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
                TextField(value = state.value.titleTextState.value,
                    textStyle = typography.titleLarge, placeholder = {
                    Text(
                        "Title : Keep your thoughts alive.",
                        modifier = Modifier.padding(start = paddingLeft),
                    )
                }, colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = colorScheme.tertiary
                ), onValueChange = {
                    state.value.titleTextState.value = it
                }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).padding(top = 2.dp)
                )
                HorizontalDivider(color = colorScheme.outline, thickness = 2.dp)
                contentList()
            }
            onButtonOverLays()
        }
    }

}

@Composable
fun RenderWidget(
    modifier: Modifier = Modifier,
    content: NoteContentModel,
    onUpdate: (content: NoteContentModel) -> Unit,
    onDelete:  (content: NoteContentModel) -> Unit,
) {
    when (content.type) {
        ContentType.TEXT -> {
            NoteTextField(noteContentModel = (content as NoteContentModel.TextContent), onUpdate = {
                onUpdate(content.copy(text = it))
            })
        }

        ContentType.IMAGE -> {
            val contentImage = content as NoteContentModel.ImageContent
            val path = contentImage.localPath ?: contentImage.url
            Card {
                LoadImage(url = path, modifier = Modifier)
            }
        }
        ContentType.VIDEO -> {
            val contentVideo = content as NoteContentModel.MediaContent
            val path = contentVideo.localPath ?: contentVideo.url

            Card {

            }
        }

        ContentType.AUDIO -> {
            val contentAudio = content as NoteContentModel.MediaContent
            if (contentAudio.duration > 0) {
                AudioPlayerUIState(onDelete = onDelete)
            } else {
                AudioRecording(contentAudio, onStop = { onUpdate(it) }, onDelete= onDelete)
            }
        }

        ContentType.LINK -> {
            val contentLink = content as NoteContentModel.Link
            Text(contentLink.url, modifier = Modifier.clickable {

            })
        }

        ContentType.DOCX -> {
            val contentDoc = content as NoteContentModel.DocContent
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