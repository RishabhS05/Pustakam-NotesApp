package com.app.pustakam.android.screen.masterEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.editor.EditorCapabilityCallbacks
import com.app.pustakam.android.screen.editor.EditorCapabilityHost
import com.app.pustakam.android.screen.editor.permissionsFor
import com.app.pustakam.android.widgets.masterEditor.MasterCanvas
import com.app.pustakam.feature.notes.domain.editor.CaptureKind
import com.app.pustakam.android.widgets.smartText.SmartTextKeyboardToolbar
import com.app.pustakam.android.widgets.smartText.SmartTextSheet
import com.app.pustakam.android.widgets.smartText.SmartTextSheetHost
import com.app.pustakam.core.richtext.master.presentation.MasterTextCommands
import com.app.pustakam.core.richtext.presentation.SmartTextCommands
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind
import com.app.pustakam.core.richtext.master.presentation.CanvasCommands
import com.app.pustakam.core.richtext.master.presentation.CanvasTool

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterEditorScreen(
    noteId: String? = null,
    viewModel: MasterEditorViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenMedia: (String?) -> Unit = {},
    onCaptureMedia: (String?) -> Unit = {}
) {
    val colors = SmartTextTokens.colors
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val canvas = uiState.canvas
    var showAttach by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(SmartTextSheet.NONE) }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    EditorCapabilityHost(
        state = uiState.capabilities,
        noteTitle = uiState.note?.title.orEmpty(),
        permissions = permissionsFor(uiState.capabilities.pendingCapture),
        audioDraft = { uiState.audioDraft },
        callbacks = EditorCapabilityCallbacks(
            onState = viewModel::onCapabilityState,
            onOpenCamera = { onCaptureMedia(uiState.note?.id) },
            onAudioSaved = viewModel::onCaptured,
            onFilesPicked = { context, uris -> viewModel.importDeviceFiles(context, uris) },
            onImportLink = { context, link -> viewModel.importFromLink(context, link) },
            onDeleteContent = viewModel::deleteContent,
            onDeleteNote = { onBack() }
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.page)) {
        MasterCanvas(
            state = canvas,
            onIntent = viewModel::onCanvasIntent,
            onRename = viewModel::renameNode
        ) { node, isEditing ->
            MasterNodeContent(
                node = node,
                isEditing = isEditing,
                scale = canvas.viewport.scale,
                textState = uiState.textFor(node.id),
                content = uiState.note?.contents?.firstOrNull { it.id == node.contentId },
                onTextIntent = { viewModel.onTextIntent(node.id, it) },
                onFocused = { viewModel.onCanvasIntent(CanvasCommands.setEditing(node.id)) },
                onOpenMedia = { onOpenMedia(node.contentId) },
                onDelete = { viewModel.deleteNode(node.id) }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .background(colors.toolbar, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = { viewModel.onCanvasIntent(CanvasCommands.zoomOut()) }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = colors.onSurface)
            }
            Text(
                text = "${canvas.zoomPercent}%",
                style = TextStyle(color = colors.onSurfaceMuted, fontSize = 13.sp)
            )
            IconButton(onClick = { viewModel.onCanvasIntent(CanvasCommands.zoomIn()) }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = colors.onSurface)
            }
            IconButton(onClick = { viewModel.onCanvasIntent(CanvasCommands.zoomToFit()) }) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = "Fit to screen",
                    tint = colors.onSurface
                )
            }
            IconButton(
                onClick = {
                    viewModel.onCanvasIntent(
                        if (canvas.tool == CanvasTool.HAND) CanvasCommands.useSelectTool()
                        else CanvasCommands.useHandTool()
                    )
                }
            ) { Icon(
                    imageVector = if (canvas.tool == CanvasTool.HAND) Icons.Default.PanTool
                    else Icons.Default.TouchApp,
                    contentDescription = "Hand tool",
                    tint = if (canvas.tool == CanvasTool.HAND) colors.accent else colors.onSurface
                ) }
            IconButton(onClick = { showAttach = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add widget", tint = colors.accent)
            }
        }

        val focusedText = canvas.editingNodeId?.let { uiState.textFor(it) }
        if (focusedText != null) {
            SmartTextKeyboardToolbar(
                toolbar = focusedText.toolbar,
                expanded = focusedText.isToolbarExpanded,
                canUndo = focusedText.canUndo,
                canRedo = focusedText.canRedo,
                onAction = { action ->
                    val nodeId = canvas.editingNodeId ?: return@SmartTextKeyboardToolbar
                    if (SmartTextCommands.isMore(action)) {
                        viewModel.onTextIntent(
                            nodeId,
                            MasterTextCommands.setToolbarExpanded(!focusedText.isToolbarExpanded)
                        )
                    } else if (SmartTextCommands.isDismiss(action)) {
                        viewModel.onCanvasIntent(CanvasCommands.setEditing(null))
                    } else {
                        val intent = MasterTextCommands.forToolbar(action)
                        if (intent != null) {
                            viewModel.onTextIntent(nodeId, intent)
                        } else {
                            // style / colour / size / align / link open a sheet — without this
                            // branch they silently did nothing in the MasterEditor
                            sheet = SmartTextSheet
                                .fromIndex(MasterTextCommands.sheetIndex(action))
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            )
        }

        val sheetNodeId = canvas.editingNodeId
        if (sheet != SmartTextSheet.NONE && focusedText != null && sheetNodeId != null) {
            SmartTextSheetHost(
                sheet = sheet,
                currentStyle = focusedText.toolbar.paragraphStyle,
                currentAlign = focusedText.toolbar.align,
                currentFontSize = focusedText.toolbar.fontSize,
                currentLink = focusedText.toolbar.link,
                searchQuery = "",
                replacement = "",
                matchCount = 0,
                currentMatch = 0,
                tableRowCount = 0,
                tableColumnCount = 0,
                onDismiss = { sheet = SmartTextSheet.NONE },
                onStyle = {
                    viewModel.onTextIntent(sheetNodeId, MasterTextCommands.setParagraphStyle(it))
                    sheet = SmartTextSheet.NONE
                },
                onAlign = {
                    viewModel.onTextIntent(sheetNodeId, MasterTextCommands.setAlignment(it))
                    sheet = SmartTextSheet.NONE
                },
                onColor = {
                    val intent = if (sheet == SmartTextSheet.BACKGROUND_COLOR) {
                        MasterTextCommands.setBackgroundColor(it)
                    } else {
                        MasterTextCommands.setTextColor(it)
                    }
                    viewModel.onTextIntent(sheetNodeId, intent)
                    sheet = SmartTextSheet.NONE
                },
                onFontSize = {
                    viewModel.onTextIntent(sheetNodeId, MasterTextCommands.setFontSize(it))
                    sheet = SmartTextSheet.NONE
                },
                onLink = {
                    viewModel.onTextIntent(sheetNodeId, MasterTextCommands.setLink(it))
                    sheet = SmartTextSheet.NONE
                },
                onTable = { sheet = SmartTextSheet.NONE },
                onInsertTable = { _, _ -> sheet = SmartTextSheet.NONE },
                onSearchQuery = {},
                onReplacement = {},
                onFindNext = {},
                onFindPrevious = {},
                onReplaceCurrent = {},
                onReplaceAll = {}
            )
        }

        if (showAttach) {
            ModalBottomSheet(onDismissRequest = { showAttach = false }) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = "Add beside the focused widget",
                        style = TextStyle(color = colors.onSurfaceMuted, fontSize = 13.sp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "Rebuild layout from note order",
                        style = TextStyle(color = colors.accent, fontSize = 16.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttach = false
                                viewModel.rebuildLayoutFromNote()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                    Text(
                        text = "Apply canvas order back to the note",
                        style = TextStyle(color = colors.accent, fontSize = 16.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttach = false
                                viewModel.applyCanvasOrderToNote()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                    listOf(
                        "Text" to CanvasNodeKind.MASTER_TEXT,
                        "Table" to CanvasNodeKind.TABLE,
                        "Drawing" to CanvasNodeKind.DRAWING
                    ).forEach { (label, kind) ->
                        Text(
                            text = label,
                            style = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAttach = false
                                    if (kind == CanvasNodeKind.MASTER_TEXT) viewModel.addTextNode()
                                    else viewModel.addWidgetNearFocused(kind)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }

                    listOf(
                        "Photo or video" to CaptureKind.IMAGE,
                        "Record audio" to CaptureKind.AUDIO,
                        "Location" to CaptureKind.LOCATION,
                        "Import a file" to CaptureKind.FILE
                    ).forEach { (label, kind) ->
                        Text(
                            text = label,
                            style = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAttach = false
                                    viewModel.requestCapture(kind)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}
