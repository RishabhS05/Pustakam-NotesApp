package com.app.pustakam.android.widgets.masterEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.presentation.CanvasCommands
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorIntent
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorState
import com.app.pustakam.core.richtext.master.presentation.CanvasTool

/**
 * The scaling layer. Everything to do with pan and zoom lives here and nowhere else: this Box
 * owns the pinch, the drag-to-pan and the viewport size, and every page below is positioned and
 * sized from [CanvasEditorState.viewport]. Node composables never read the viewport themselves.
 */
@Composable
fun MasterCanvas(
    state: CanvasEditorState,
    modifier: Modifier = Modifier,
    onIntent: (CanvasEditorIntent) -> Unit,
    onRename: (String, String) -> Unit = { _, _ -> },
    nodeContent: @Composable (CanvasNode, Boolean) -> Unit
) {
    val colors = SmartTextTokens.colors
    val viewport = state.viewport
    val permits = state.permits

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.page)
            .onSizeChanged {
                onIntent(CanvasCommands.viewportResized(it.width.toFloat(), it.height.toFloat()))
            }
            // Intercepted on the INITIAL pass, before children see the event. detectTransformGestures
            // runs on the Main pass, so the BasicTextField filling each page consumed the pinch
            // first and the canvas never scaled. Two fingers are always ours; one finger is only
            // ours in hand mode, which leaves normal taps and text selection to the page.
            .pointerInput(permits) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        val multiTouch = pressed >= 2
                        val owns = (multiTouch && permits.canZoom) ||
                            (permits.canPan && state.tool != CanvasTool.SELECT)
                        if (owns) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (multiTouch && permits.canZoom && zoom != 1f) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                onIntent(CanvasCommands.zoom(zoom, centroid.x, centroid.y))
                            }
                            if (pan != Offset.Zero && permits.canPan) {
                                onIntent(CanvasCommands.pan(pan.x, pan.y))
                            }
                            if (multiTouch || pan != Offset.Zero) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(state.document, permits) {
                detectTapGestures(
                    onTap = { onIntent(CanvasCommands.selectAt(it.x, it.y)) },
                    onDoubleTap = { position ->
                        val node = state.document.hitTest(
                            viewport.toCanvasX(position.x),
                            viewport.toCanvasY(position.y)
                        )
                        if (node != null) onIntent(CanvasCommands.setEditing(node.id))
                        else if (CanvasCommands.isEditing(state)) {
                            onIntent(CanvasCommands.exitEditing())
                        } else onIntent(CanvasCommands.zoomToFit())
                    }
                )
            }
    ) {
        // keyed by id: without this, a z-order change reorders the children and Compose
        // rebuilds the subtree, dropping focus out of whichever field was being typed in
        state.visibleNodes.forEach { node ->
            key(node.id) {
                MasterCanvasNode(
                    node = node,
                    state = state,
                    onIntent = onIntent,
                    onRename = onRename,
                    nodeContent = nodeContent
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MasterCanvasNode(
    node: CanvasNode,
    state: CanvasEditorState,
    onIntent: (CanvasEditorIntent) -> Unit,
    onRename: (String, String) -> Unit,
    nodeContent: @Composable (CanvasNode, Boolean) -> Unit
) {
    val colors = SmartTextTokens.colors
    val density = LocalDensity.current
    // pointerInput below is keyed on node.id only, so its lambda would capture the state from
    // first composition forever. resizedTo() would then add each delta to the original width.
    val liveState by rememberUpdatedState(state)
    val screen = CanvasCommands.screenRectOf(node, state.viewport)
    val isSelected = node.id == state.selectedNodeId
    val isEditing = node.id == state.editingNodeId
    // dragging only after the node is selected, so a pinch that starts over a page still
    // reaches the scaling layer above instead of being consumed as a node drag
    val permits = state.permits
    val draggable = isSelected && permits.canDragNode && !node.locked

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
                if (isSelected) Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(8.dp))
                else Modifier.border(1.dp, colors.divider, RoundedCornerShape(8.dp))
            )
            .pointerInput(node.id, draggable) {
                if (!draggable) return@pointerInput
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

        if (isSelected && permits.canResizeNode && !node.locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(22.dp)
                    .background(colors.accent, RoundedCornerShape(4.dp))
                    .pointerInput(node.id) {
                        detectDragGestures(
                            onDragStart = { onIntent(CanvasCommands.beginResize(node.id)) },
                            onDragEnd = { onIntent(CanvasCommands.endResize()) },
                            onDragCancel = { onIntent(CanvasCommands.endResize()) },
                            onDrag = { change, drag ->
                                change.consume()
                                CanvasCommands
                                    .resizedTo(liveState, node.id, drag.x, drag.y)
                                    ?.let(onIntent)
                            }
                        )
                    }
            )
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
