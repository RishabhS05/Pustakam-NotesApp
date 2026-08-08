package com.app.pustakam.android.widgets.masterEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.presentation.CanvasCommands
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorIntent
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorState
import com.app.pustakam.core.richtext.master.presentation.CanvasTool

@Composable
fun MasterCanvas(
    state: CanvasEditorState,
    modifier: Modifier = Modifier,
    onIntent: (CanvasEditorIntent) -> Unit,
    nodeContent: @Composable (CanvasNode, Boolean) -> Unit
) {
    val colors = SmartTextTokens.colors
    val density = LocalDensity.current
    val viewport = state.viewport
    val handMode = state.tool == CanvasTool.HAND

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.page)
            .onSizeChanged {
                onIntent(
                    CanvasCommands.viewportResized(it.width.toFloat(), it.height.toFloat())
                )
            }
            .pointerInput(handMode) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onIntent(CanvasCommands.zoom(zoom, centroid.x, centroid.y))
                    }
                    if (pan != androidx.compose.ui.geometry.Offset.Zero) {
                        onIntent(CanvasCommands.pan(pan.x, pan.y))
                    }
                }
            }
            .pointerInput(state.document, handMode) {
                detectTapGestures(
                    onTap = { onIntent(CanvasCommands.selectAt(it.x, it.y)) },
                    onDoubleTap = { position ->
                        val canvasX = viewport.toCanvasX(position.x)
                        val canvasY = viewport.toCanvasY(position.y)
                        val node = state.document.hitTest(canvasX, canvasY)
                        if (node != null) {
                            onIntent(CanvasCommands.focusNode(node.id))
                        } else {
                            onIntent(CanvasCommands.zoomToFit())
                        }
                    }
                )
            }
    ) {
        state.visibleNodes.forEach { node ->
            val screen = CanvasCommands.screenRectOf(node, viewport)
            val isSelected = node.id == state.selectedNodeId
            val isEditing = node.id == state.editingNodeId

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = screen.x
                        translationY = screen.y
                    }
                    .size(
                        width = with(density) { screen.width.toDp() },
                        height = with(density) { screen.height.toDp() }
                    )
                    .background(colors.surface, RoundedCornerShape(8.dp))
                    .then(
                        if (isSelected) {
                            Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(8.dp))
                        } else {
                            Modifier.border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                        }
                    )
                    .pointerInput(node.id, handMode, isEditing) {
                        if (handMode || isEditing || node.locked) return@pointerInput
                        detectDragGestures(
                            onDragStart = { onIntent(CanvasCommands.beginDrag(node.id)) },
                            onDragEnd = { onIntent(CanvasCommands.endDrag()) },
                            onDragCancel = { onIntent(CanvasCommands.endDrag()) },
                            onDrag = { change, drag ->
                                change.consume()
                                onIntent(CanvasCommands.dragBy(drag.x, drag.y))
                            }
                        )
                    }
            ) {
                nodeContent(node, isEditing)
            }
        }
    }
}
