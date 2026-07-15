package com.app.pustakam.android.screen.noteEditor

// 🔧 14-Jul-2026: Save-media-to-device — SAF picker launcher for audio/pdf/docx
// 🔧 14-Jul-2026: Save-media-to-device helpers
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
// 🔧 14-Jul-2026: NEW — bottom-sheet imports for the "Save to Gallery / Save as file" chooser
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// 🔧 14-Jul-2026: rememberCoroutineScope — run save file-I/O off the main thread
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.app.pustakam.android.extension.startServiceWrapper
import com.app.pustakam.android.fileUtils.mimeTypeFor
import com.app.pustakam.android.fileUtils.saveMediaToGallery
import com.app.pustakam.android.fileUtils.suggestedFileName
import com.app.pustakam.android.fileUtils.writeMediaToUri
import com.app.pustakam.android.hardware.camera.ImageDataViewModel
import com.app.pustakam.android.permission.AskPermissions
import com.app.pustakam.android.screen.NoteContentUiState
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.navigation.Route
import com.app.pustakam.android.services.locationService.LocationService
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
                        ContentType.IMAGE, ContentType.VIDEO -> navigateTo(
                            CameraData(
                                noteEditorViewModel.noteContentUiState.value.note?.id!!
                            )
                        )
                        ContentType.AUDIO -> noteEditorViewModel.startStopAudioRecording()
                        ContentType.LOCATION -> {
                            noteEditorViewModel.locationState(true)
                        }

                        else -> {}
                    }
                })
            }

            LocationState -> {
                val intent = Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                }
                (context as Activity).startServiceWrapper(intent = intent)
            }

            showDeleteAlert -> {
                if (deleteNoteContentId.isNotnull()) {
                    DeleteNoteAlert(noteTitle = "Recorded Note", onConfirm = {
                        noteEditorViewModel.removeContent(value = deleteNoteContentId!!)
                    }) {
                        noteEditorViewModel.showDeleteAlertBox(false, null)
                    }
                } else {
                    DeleteNoteAlert(
                        noteTitle = if (!noteEditorViewModel.noteContentUiState.value.note?.title.isNullOrEmpty()) noteEditorViewModel.noteContentUiState.value.note?.title!! else "",
                        onConfirm = {
                            noteEditorViewModel.deleteNote(noteId = id!!)
                            noteEditorViewModel.showDeleteAlertBox(false)
                        }) {
                        noteEditorViewModel.showDeleteAlertBox(false)
                    }
                }
            }
        }
    }
    // 🔧 14-Jul-2026: FIX (back button dead while media plays) — this used to be an `.also{}` in the
    //   composition body, so EVERY recomposition with noteStatus == onBackPress fired another
    //   createOrUpdateNote(). Two INSERTs raced and the second success downgraded
    //   onSaveCompletedExit → onSaveCompleted, so onBack() never ran (playback recompositions made
    //   the losing race likely). LaunchedEffect keyed on the status runs exactly ONCE per status
    //   change, no matter how often the screen recomposes.
    LaunchedEffect(stateEditor.noteStatus) {
        when (stateEditor.noteStatus) {
            NoteStatus.onBackPress -> noteEditorViewModel.createOrUpdateNote()
            NoteStatus.onSaveCompletedExit, NoteStatus.exit -> onBack()
            else -> {}
        }
    }
    // 🔧 14-Jul-2026: CRASH FIX — previously getMediaData()/clearPaths() ran directly in the
    //   composition body, mutating the SnapshotStateList that the LazyColumn was reading in the
    //   same frame. With multiple captured images this caused a crash / recomposition loop.
    //   Now the captured paths are consumed once inside a LaunchedEffect (keyed on the list),
    //   off the composition pass.
    val capturedPaths = imageDataViewModel.paths.collectAsStateWithLifecycle().value
    LaunchedEffect(capturedPaths) {
        if (capturedPaths.isNotEmpty()) {
            noteEditorViewModel.getMediaData(capturedPaths)
            imageDataViewModel.clearPaths()
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
                modifier = Modifier.align(alignment = Alignment.BottomEnd),
                onAddTextField = {
                    noteEditorViewModel.addNewText()
                },
                onArrowButton = { focusManager.clearFocus() },
                onRecordMic = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.AUDIO)
                },
                onLocation = {
                    noteEditorViewModel.preparePermissionDialog(contentType = ContentType.LOCATION)
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
                    // 🔧 15-Jul-2026 Phase 0.3: no more sortedBy on EVERY recomposition (O(n·log n)
                    //   per frame at scale) — contents are kept sorted at load (READ sorts once) and
                    //   all inserts append with increasing positions. This also fixes a latent bug:
                    //   `index` fed to updateContent(index, ..) was an index into the SORTED COPY,
                    //   not into `contents`. Stable `key = content.id` lets Compose reuse item state
                    //   instead of rebinding every card when the list changes.
                    itemsIndexed(it, key = { _, content -> content.id }) { index, contentValue ->
                        RenderWidget(
                            content = contentValue,
                            focusRequester = focusRequester,
                            onUpdate = { value ->
                                noteEditorViewModel.updateContent(index, value)
                            },
                            onDelete = { value ->
                                noteEditorViewModel.showDeleteAlertBox(
                                    true,
                                    deleteNoteContentId = value.id
                                )
                            },
                            onShare = {},
                            onMediaPreview = {
                                // 🔧 14-Jul-2026: CHANGED — pass the content id so VideoPreviewScreen
                                //   can drive the standalone player by mediaId (no path guessing).
                                imageDataViewModel.onSetMediaToPreview(
                                    (contentValue as NoteContentModel.MediaContent).getMediaUrl(),
                                    contentValue.type,
                                    mediaId = contentValue.id
                                )
                                when {
                                    contentValue.type == ContentType.IMAGE -> navigateTo(Route.ImagePreview)
                                    contentValue.type == ContentType.VIDEO -> navigateTo(Route.VideoPreview)
                                }
                            })
                    }
                }
            }
            if (stateEditor.showAudioRecorder)
                AudioRecording(
                    modifier = Modifier.align(Alignment.TopEnd),
                    noteContentModel = noteEditorViewModel.addNewContent(
                        context,
                        contentType = ContentType.AUDIO
                    ) as NoteContentModel.MediaContent,
                    onStop = {
                        noteEditorViewModel.updateContent(content = it)
                        noteEditorViewModel.startStopAudioRecording(false)
                    },
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
    Scaffold(topBar = topBar, floatingActionButton = onButtonOverLays) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {
            if (isRuledEnabledState.value) RuledPage()
            Column {
                TextField(
                    value = state.value.titleTextState.value,
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
                        state.value.titleTextState.value = it
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
                contentList(focusRequester)
            }
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
    onShare: (content: NoteContentModel) -> Unit,
    onMediaPreview: () -> Unit,
) {
    var focusedMediaId by remember { mutableStateOf<String?>(null) }
    when (content.type) {
        ContentType.TEXT -> {
            Box(
                modifier = Modifier
            ) {
                NoteTextField(
                    noteContentModel = (content as NoteContentModel.TextContent),
                    focusRequester = focusRequester,
                    onUpdate = { onUpdate(content.withText(it)) }) { // 🔧 F2: stamps content updatedAt (was plain copy)
//                        if (it.selection.length > 0) {
//                            selectionString.value = if (it.selection.start <= it.selection.end)
//                                it.text.substring(
//                                    it.selection.start,
//                                    it.selection.end
//                                ) else it.text.substring(it.selection.end, it.selection.start)
//                            println("Selected Text : ${selectionString.value}")
//                            isDropDownVisible = true
//                        } else isDropDownVisible = false
                }
            }
        }

        ContentType.IMAGE -> {
            val contentImage = content as NoteContentModel.MediaContent
            val path = contentImage.localPath ?: contentImage.url
            // 🔧 14-Jul-2026: NEW — wrapped with save-to-device overlay (image → Gallery)
            ImageCard(
                imageUrl = path, modifier = Modifier,
                // 🔧 14-Jul-2026: CHANGED — reveal reverted to LONG-PRESS (2.5s auto-hide inside the
                //   card, iOS parity). `visible` is true on long-press, false when the timer fires;
                //   clear the id only if this card still owns it.
                onShowActions = { visible ->
                    focusedMediaId = when {
                        visible -> contentImage.id
                        focusedMediaId == contentImage.id -> null
                        else -> focusedMediaId
                    }
                },
                onClick = onMediaPreview){
                 MediaSaveOverlay(contentImage,
                     isFocused = focusedMediaId == content.id,
                     onDelete = {
                     onDelete(contentImage)
                 },
                     onShare =  {}
                 )
            }

        }

        ContentType.VIDEO -> {
            val contentVideo = content as NoteContentModel.MediaContent
            // 🔧 14-Jul-2026: NEW — wrapped with save-to-device overlay (video → Gallery)
            VideoCard(
                contentVideo,
                modifier = Modifier,
                // 🔧 14-Jul-2026: CHANGED — same long-press reveal + timed hide as ImageCard.
                onShowActions =  { visible ->
                    focusedMediaId = when {
                        visible -> contentVideo.id
                        focusedMediaId == contentVideo.id -> null
                        else -> focusedMediaId
                    }
                },
                onClick = onMediaPreview,

            ) {
                MediaSaveOverlay(
                    contentVideo, onDelete = {
                        onDelete(contentVideo)
                    },
                    isFocused = focusedMediaId == contentVideo.id,
                    onShare = {}
                )
            }
        }

        ContentType.AUDIO -> {
            val contentAudio = content as NoteContentModel.MediaContent
            val context = LocalContext.current
            // 🔧 14-Jul-2026: PERF — coroutine scope so the file copy runs off the main thread.
            val scope = rememberCoroutineScope()
            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(mimeTypeFor(contentAudio.type))
            ) { uri ->
                if (uri != null) {
                    // 🔧 14-Jul-2026: PERF — copy bytes on IO, confirm on the main thread.
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { writeMediaToUri(context, contentAudio, uri) }
                        Toast.makeText(
                            context,
                            if (ok) "Saved to selected location" else "Save failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            // Audio save lives inside the player card; onSave opens the SAF picker (default Downloads).
            AudioPlayerUIState(contentAudio, onDelete = onDelete, onSave = {
                    exportLauncher.launch(suggestedFileName(contentAudio))
                })
        }

        ContentType.LINK -> {
            val contentLink = content as NoteContentModel.Link
            Text(contentLink.url, modifier = Modifier.clickable {})
        }

        ContentType.DOCX -> {
            val contentDoc = content as NoteContentModel.MediaContent
            contentDoc.localPath ?: contentDoc.url
        }

        ContentType.LOCATION -> {
            content as NoteContentModel.Location

        }

        ContentType.PDF -> {}
        ContentType.GIF -> {}
    }
}

// 🔧 14-Jul-2026: NEW FEATURE — "Save media to device" overlay.
//   Renders the given media card with a small save icon pinned top-end.
//   • IMAGE / VIDEO  -> saveMediaToGallery() writes silently into the system Gallery.
//   • AUDIO / PDF / DOCX / GIF -> opens the ACTION_CREATE_DOCUMENT picker (default
//     location Downloads on most devices) so the user picks the path, then the file
//     bytes are copied to the chosen Uri via writeMediaToUri().
//   A Toast confirms success/failure. Purely additive — the wrapped card is unchanged.
//   Usage:  MediaSaveOverlay(media = contentImage) { ImageCard(...) }

// 🔧 14-Jul-2026: @OptIn required — ModalBottomSheet / rememberModalBottomSheetState are experimental.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.MediaSaveOverlay(
    media: NoteContentModel.MediaContent,
    isFocused : Boolean = false,
    onDelete: () -> Unit = {},
    onShare : ()-> Unit ={},
) {

    val context = LocalContext.current
    // 🔧 14-Jul-2026: PERF — coroutine scope so file copies run off the main thread.
    val scope = rememberCoroutineScope()
    val isGalleryType = media.type == ContentType.IMAGE || media.type == ContentType.VIDEO

    // SAF picker for non-gallery media (pdf/docx/gif). The pre-filled name carries the correct
    // extension; the returned Uri is the user-selected destination. (Audio is handled inside
    // the audio player card — see AudioPlayerUIState.)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeTypeFor(media.type))
    ) { uri ->
        if (uri != null) {
            // 🔧 14-Jul-2026: PERF — copy bytes on IO, confirm on the main thread.
            scope.launch {
                val ok = withContext(Dispatchers.IO) { writeMediaToUri(context, media, uri) }
                Toast.makeText(
                    context,
                    if (ok) "Saved to selected location" else "Save failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 🔧 14-Jul-2026: NEW FEATURE — controls the "Save to Gallery / Save as file" bottom sheet.
    //   Only used for IMAGE/VIDEO; other types still export straight to the SAF picker.
    var showSaveSheet by remember { mutableStateOf(false) }

    // 🔧 14-Jul-2026: helper — silent MediaStore save, run on IO, Toast on the main thread.
    val saveToGallery: () -> Unit = {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { saveMediaToGallery(context, media) }
            Toast.makeText(
                context,
                if (ok) "Saved to Gallery" else "Save failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    if(isFocused)
        Row(modifier = Modifier.fillMaxWidth()
            .background(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = .05f),
                    Color.Black.copy(alpha = .30f),
                    Color.Black.copy(alpha = .55f)
                )
            )
        ).align(Alignment.BottomEnd), horizontalArrangement = Arrangement.End) {
            IconButton(
                onClick = {
                    if (isGalleryType) {
                        // 🔧 14-Jul-2026: CHANGED — was a silent gallery save; now opens the two-option
                        //   bottom sheet so the user picks Gallery vs. a file location.
                        showSaveSheet = true
                    } else {
                        // Opens the picker with the suggested file name; default folder = Downloads.
                        exportLauncher.launch(suggestedFileName(media))
                    }
                },
                modifier = Modifier.padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SaveAlt,
                    contentDescription = "Save media to device",
                    tint = colorScheme.primary,
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Save media to device",
                    tint = colorScheme.primary,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Save media to device",
                    tint = colorScheme.error,
                )
            }
        }

    // 🔧 14-Jul-2026: NEW FEATURE — the save-options bottom sheet (IMAGE/VIDEO only).
    //   Row 1 → Save to Gallery (silent MediaStore write).
    //   Row 2 → Save to location as file (SAF picker, default Downloads).
    if (showSaveSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false },
            sheetState = sheetState,
        ) {
            ListItem(
                headlineContent = { Text("Save to Gallery") },
                leadingContent = {
                    Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = colorScheme.primary)
                },
                modifier = Modifier.clickable {
                    showSaveSheet = false
                    saveToGallery()
                }
            )
            ListItem(
                headlineContent = { Text("Save to location as file") },
                leadingContent = {
                    Icon(Icons.Filled.SaveAs, contentDescription = null, tint = colorScheme.primary)
                },
                modifier = Modifier.clickable {
                    showSaveSheet = false
                    // Reuses the existing SAF export; mimeTypeFor already handles image/png & video/mp4.
                    exportLauncher.launch(suggestedFileName(media))
                }
            )
        }
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
                color = lineColor,
                start = Offset(0f, y),
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