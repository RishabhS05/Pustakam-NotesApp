package com.app.pustakam.android.screen.noteEditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.AskPermissions
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.LoadImage
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.android.widgets.alert.DeleteNoteAlert
import com.app.pustakam.android.widgets.fabWidget.OverLayEditorButtons
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.ContentType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditorView(
    id: String? = null,
    onBack: () -> Unit = {},
) {
    val noteEditorViewModel: NoteEditorViewModel = viewModel()
    BackHandler {
        /**  block direct exit as my scope is getting distroyed  */
        noteEditorViewModel.changeNoteStatus(NoteStatus.onBackPress)
    }
    val state = noteEditorViewModel.noteUIState
        .collectAsStateWithLifecycle().value.apply {
            when {
                isLoading -> LoadingUI()
                error.isNotnull() -> SnackBarUi(error = error!!) {
                    noteEditorViewModel.clearError()
                }
                showPermissionAlert == true -> {
                    AskPermissions(permissionsRequired = permissions,
                        onDismiss = { noteEditorViewModel.showPermissionAlert(null)},
                        onGrantPermission = { noteEditorViewModel.showPermissionAlert(null) })
                }
                showDeleteAlert ->
                    DeleteNoteAlert(noteTitle = if (!note?.title.isNullOrEmpty()) note?.title!! else "",
                        onConfirm = {
                            noteEditorViewModel.deleteNote(noteId = id!!)
                            noteEditorViewModel.showDeleteAlert(false)
                        }
                    ) { noteEditorViewModel.showDeleteAlert(false)
                    }
            }
        }
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                noteEditorViewModel.changeNoteStatus(null)
                noteEditorViewModel.readFromDataBase(id)
            }

            else -> {}
        }
    }
    when (state.noteStatus) {
        NoteStatus.onBackPress -> {
            noteEditorViewModel.createOrUpdateNote()
        }
        NoteStatus.onSaveCompletedExit -> {
            onBack()
        }

        else -> {}
    }
    val focusManager = LocalFocusManager.current
    NotesEditor(state = state,
        topBar = {
            TopAppBar(title = { /*TODO*/ },
                actions = {
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
                    if(state.showDeleteButton)
                    IconButton(onClick = {
                        noteEditorViewModel.showDeleteAlert(true)
                    }) {
                        Icon(
                            Icons.Filled.Delete, tint = Color(0xFFC62828),
                            contentDescription = "Delete a note",
                        )
                    }
                })
        },
        onButtonOverLays = {
            Box(Modifier.fillMaxSize()) {
                OverLayEditorButtons(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd),
                    onAddTextField = {
                        noteEditorViewModel.prepareInitialContent(ContentType.TEXT)
                    },
                    onArrowButton = { focusManager.clearFocus() },
                    onRecordVideo = {
                        noteEditorViewModel.preparePermissionDialog(contentType = ContentType.VIDEO)
                    },
                    onRecordMic = {
                        noteEditorViewModel.preparePermissionDialog(contentType = ContentType.AUDIO)
                    },
                    onAddImage = {
                        noteEditorViewModel.preparePermissionDialog( contentType = ContentType.IMAGE)
                    },
                )
            }
        }
    )
}

@Composable
fun NotesEditor(
    state: NoteUIState,
    topBar: @Composable () -> Unit,
    onButtonOverLays: @Composable () -> Unit,
) {
    // Note content state
    val isRuledEnabledState = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val paddingLeft = if (isRuledEnabledState.value) 100.dp else 12.dp
    Scaffold(topBar = topBar)
    { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {
            if (isRuledEnabledState.value) RuledPage()
            Column {
                TextField(
                    value = state.titleTextState.value,
                    textStyle = typography.titleLarge,
                    placeholder = {
                        Text(
                            "Title : Keep your thoughts alive.",
                            modifier = Modifier.padding(start = paddingLeft),
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colorScheme.tertiary
                    ),
                    onValueChange = {
                        state.titleTextState.value = it
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(top = 2.dp)
                )
                HorizontalDivider(color = colorScheme.outline, thickness = 2.dp)
            }
            onButtonOverLays()
        }
    }

}

@Composable
fun SpawnWidget(
    modifier: Modifier = Modifier,
    content: NoteContentModel
) {
    when (content.type) {
        ContentType.TEXT -> {
            val content = content as NoteContentModel.TextContent
            TextField(value = content.text, onValueChange = {
            })
        }

        ContentType.IMAGE -> {
            val content = content as NoteContentModel.ImageContent
            val path = content.localPath ?: content.url
            Card {
                LoadImage(url = path, modifier = Modifier)
            }

        }

        ContentType.VIDEO -> {
            val content = content as NoteContentModel.VideoContent
            val path = content.localPath ?: content.url
            Card {

            }
        }

        ContentType.AUDIO -> {
            val content = content as NoteContentModel.AudioContent
            val path = content.localPath ?: content.url

        }

        ContentType.LINK -> {
            val content = content as NoteContentModel.Link
            Text(content.url, modifier = Modifier.clickable {

            })
        }

        ContentType.DOCX -> {
            val content = content as NoteContentModel.DocContent
            val path = content.localPath ?: content.url
        }

        ContentType.LOCATION -> {
            val content = content as NoteContentModel.Location
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
                color = lineColor, start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += lineSpacing
        }
        drawLine(
            color = marginColor,
            start = Offset(startX, 0f),
            end = Offset(startX, size.height),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = marginColor,
            start = Offset(startX + 20f, 0f),
            end = Offset(startX + 20f, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}


@Preview
@Composable
private fun NoteEditorPreview() {
    MyApplicationTheme {
        NotesEditor(state = NoteUIState(), topBar = {}) {
            OverLayEditorButtons()
        }
    }
}