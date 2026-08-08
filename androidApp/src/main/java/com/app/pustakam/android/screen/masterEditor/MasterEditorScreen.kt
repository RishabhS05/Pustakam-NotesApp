package com.app.pustakam.android.screen.masterEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.widgets.masterEditor.MasterCanvas
import com.app.pustakam.android.widgets.masterEditor.MasterTextWidget
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind
import com.app.pustakam.core.richtext.master.presentation.CanvasCommands
import com.app.pustakam.core.richtext.master.presentation.CanvasTool

@Composable
fun MasterEditorScreen(
    noteId: String? = null,
    viewModel: MasterEditorViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val colors = SmartTextTokens.colors
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val canvas = uiState.canvas

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    Box(modifier = Modifier.fillMaxSize().background(colors.page)) {
        MasterCanvas(
            state = canvas,
            onIntent = viewModel::onCanvasIntent
        ) { node, isEditing ->
            when (node.kind) {
                CanvasNodeKind.MASTER_TEXT -> {
                    val textState = uiState.textFor(node.id)
                    if (textState != null) {
                        MasterTextWidget(
                            state = textState,
                            modifier = Modifier.padding(16.dp),
                            scale = canvas.viewport.scale,
                            readOnly = !isEditing,
                            onIntent = { viewModel.onTextIntent(node.id, it) },
                            onFocusChanged = { focused ->
                                if (focused) {
                                    viewModel.onCanvasIntent(CanvasCommands.setEditing(node.id))
                                }
                            }
                        )
                    }
                }

                else -> Box(modifier = Modifier.fillMaxSize())
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
            ) {
                Icon(
                    imageVector = if (canvas.tool == CanvasTool.HAND) Icons.Default.PanTool
                    else Icons.Default.TouchApp,
                    contentDescription = "Hand tool",
                    tint = if (canvas.tool == CanvasTool.HAND) colors.accent else colors.onSurface
                )
            }
            IconButton(onClick = { viewModel.addTextNode() }) {
                Icon(Icons.Default.Add, contentDescription = "Add text", tint = colors.accent)
            }
        }
    }
}
