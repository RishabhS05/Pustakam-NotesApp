package com.app.pustakam.android.widgets.masterEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onRename: (String, String) -> Unit = { _, _ -> },
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
                Column(modifier = Modifier.fillMaxSize()) {
                    MasterNodeNameBar(
                        node = node,
                        index = state.document.nodes.indexOfFirst { it.id == node.id },
                        isSelected = isSelected,
                        onRename = { onRename(node.id, it) }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        nodeContent(node, isEditing)
                    }
                }
            }
        }
    }
}


@Composable
private fun MasterNodeNameBar(
    node: CanvasNode,
    index: Int,
    isSelected: Boolean,
    onRename: (String) -> Unit
) {
    val colors = SmartTextTokens.colors
    var editing by remember(node.id) { mutableStateOf(false) }
    var draft by remember(node.id, node.name) { mutableStateOf(node.name) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.accentSoft else colors.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (editing) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 12.sp),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    editing = false
                    onRename(draft.trim())
                }),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = node.displayName(index.coerceAtLeast(0)),
                style = TextStyle(
                    color = if (isSelected) colors.accent else colors.onSurfaceMuted,
                    fontSize = 12.sp
                ),
                modifier = Modifier.clickable { editing = true }
            )
        }
    }
}
