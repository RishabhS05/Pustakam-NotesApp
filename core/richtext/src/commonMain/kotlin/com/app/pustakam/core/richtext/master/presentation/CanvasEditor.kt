package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport

enum class CanvasTool {
    SELECT,
    HAND
}

data class CanvasEditorState(
    val document: CanvasDocument = CanvasDocument(),
    val viewport: Viewport = Viewport(),
    val tool: CanvasTool = CanvasTool.SELECT,
    val selectedNodeId: String? = null,
    val draggingNodeId: String? = null,
    val editingNodeId: String? = null
) {
    val visibleNodes: List<CanvasNode>
        get() = document.visibleIn(viewport.visibleRect)

    val selectedNode: CanvasNode? get() = selectedNodeId?.let { document.nodeById(it) }

    val zoomPercent: Int get() = viewport.percent

    fun isVisible(nodeId: String): Boolean = visibleNodes.any { it.id == nodeId }
}

sealed class CanvasEditorIntent {

    data class ViewportResized(val width: Float, val height: Float) : CanvasEditorIntent()

    data class Pan(val deltaX: Float, val deltaY: Float) : CanvasEditorIntent()

    data class Zoom(val factor: Float, val focusX: Float, val focusY: Float) : CanvasEditorIntent()

    data class ZoomTo(val scale: Float, val focusX: Float, val focusY: Float) : CanvasEditorIntent()

    data object ZoomIn : CanvasEditorIntent()

    data object ZoomOut : CanvasEditorIntent()

    data object ZoomToFit : CanvasEditorIntent()

    data class FocusNode(val nodeId: String) : CanvasEditorIntent()

    data class SelectAt(val screenX: Float, val screenY: Float) : CanvasEditorIntent()

    data class SelectNode(val nodeId: String?) : CanvasEditorIntent()

    data class BeginDrag(val nodeId: String) : CanvasEditorIntent()

    data class DragBy(val deltaX: Float, val deltaY: Float) : CanvasEditorIntent()

    data object EndDrag : CanvasEditorIntent()

    data class ResizeNode(val nodeId: String, val width: Float, val height: Float) :
        CanvasEditorIntent()

    data class AddNode(val node: CanvasNode) : CanvasEditorIntent()

    data class RemoveNode(val nodeId: String) : CanvasEditorIntent()

    data class SetTool(val tool: CanvasTool) : CanvasEditorIntent()

    data class SetEditing(val nodeId: String?) : CanvasEditorIntent()

    data class LinkNodes(val fromId: String, val toId: String) : CanvasEditorIntent()
}

object CanvasEditorReducer {

    fun reduce(state: CanvasEditorState, intent: CanvasEditorIntent): CanvasEditorState =
        when (intent) {

            is CanvasEditorIntent.ViewportResized ->
                state.copy(viewport = state.viewport.withSize(intent.width, intent.height))

            is CanvasEditorIntent.Pan ->
                state.copy(viewport = state.viewport.panned(intent.deltaX, intent.deltaY))

            is CanvasEditorIntent.Zoom -> state.copy(
                viewport = state.viewport.zoomed(intent.factor, intent.focusX, intent.focusY)
            )

            is CanvasEditorIntent.ZoomTo -> state.copy(
                viewport = state.viewport.scaledTo(intent.scale, intent.focusX, intent.focusY)
            )

            CanvasEditorIntent.ZoomIn -> state.copy(
                viewport = state.viewport.scaledTo(
                    Viewport.nextZoomStop(state.viewport.scale),
                    state.viewport.widthPx / 2f,
                    state.viewport.heightPx / 2f
                )
            )

            CanvasEditorIntent.ZoomOut -> state.copy(
                viewport = state.viewport.scaledTo(
                    Viewport.previousZoomStop(state.viewport.scale),
                    state.viewport.widthPx / 2f,
                    state.viewport.heightPx / 2f
                )
            )

            CanvasEditorIntent.ZoomToFit -> {
                val bounds = state.document.bounds
                if (bounds.width <= 0f) state
                else state.copy(viewport = state.viewport.focusedOn(bounds))
            }

            is CanvasEditorIntent.FocusNode -> {
                val node = state.document.nodeById(intent.nodeId)
                if (node == null) state
                else state.copy(
                    viewport = state.viewport.focusedOn(node.rect),
                    selectedNodeId = node.id
                )
            }

            is CanvasEditorIntent.SelectAt -> {
                val canvasX = state.viewport.toCanvasX(intent.screenX)
                val canvasY = state.viewport.toCanvasY(intent.screenY)
                val hit = state.document.hitTest(canvasX, canvasY)
                state.copy(selectedNodeId = hit?.id, editingNodeId = null)
            }

            is CanvasEditorIntent.SelectNode ->
                state.copy(selectedNodeId = intent.nodeId, editingNodeId = null)

            is CanvasEditorIntent.BeginDrag -> state.copy(
                draggingNodeId = intent.nodeId,
                selectedNodeId = intent.nodeId,
                document = state.document.broughtToFront(intent.nodeId)
            )

            is CanvasEditorIntent.DragBy -> {
                val node = state.draggingNodeId
                    ?.let { state.document.nodeById(it) }
                    ?.takeIf { !it.locked }
                if (node == null) {
                    state
                } else {
                    state.copy(
                        document = state.document.replacing(
                            node.movedBy(
                                intent.deltaX / state.viewport.scale,
                                intent.deltaY / state.viewport.scale
                            )
                        )
                    )
                }
            }

            CanvasEditorIntent.EndDrag -> state.copy(draggingNodeId = null)

            is CanvasEditorIntent.ResizeNode -> {
                val node = state.document.nodeById(intent.nodeId)
                if (node == null) {
                    state
                } else {
                    state.copy(
                        document = state.document.replacing(
                            node.resizedTo(intent.width, intent.height)
                        )
                    )
                }
            }

            is CanvasEditorIntent.AddNode -> state.copy(
                document = state.document.adding(intent.node).broughtToFront(intent.node.id),
                selectedNodeId = intent.node.id
            )

            is CanvasEditorIntent.RemoveNode -> state.copy(
                document = state.document.removing(intent.nodeId),
                selectedNodeId = state.selectedNodeId.takeIf { it != intent.nodeId },
                editingNodeId = state.editingNodeId.takeIf { it != intent.nodeId }
            )

            is CanvasEditorIntent.SetTool -> state.copy(tool = intent.tool, draggingNodeId = null)

            is CanvasEditorIntent.SetEditing -> state.copy(
                editingNodeId = intent.nodeId,
                selectedNodeId = intent.nodeId ?: state.selectedNodeId
            )

            is CanvasEditorIntent.LinkNodes -> {
                val from = state.document.nodeById(intent.fromId)
                if (from == null) state
                else state.copy(document = state.document.replacing(from.linkedTo(intent.toId)))
            }
        }
}

object CanvasCommands {

    fun viewportResized(width: Float, height: Float): CanvasEditorIntent =
        CanvasEditorIntent.ViewportResized(width, height)

    fun pan(deltaX: Float, deltaY: Float): CanvasEditorIntent =
        CanvasEditorIntent.Pan(deltaX, deltaY)

    fun zoom(factor: Float, focusX: Float, focusY: Float): CanvasEditorIntent =
        CanvasEditorIntent.Zoom(factor, focusX, focusY)

    fun zoomIn(): CanvasEditorIntent = CanvasEditorIntent.ZoomIn

    fun zoomOut(): CanvasEditorIntent = CanvasEditorIntent.ZoomOut

    fun zoomToFit(): CanvasEditorIntent = CanvasEditorIntent.ZoomToFit

    fun focusNode(nodeId: String): CanvasEditorIntent = CanvasEditorIntent.FocusNode(nodeId)

    fun selectAt(screenX: Float, screenY: Float): CanvasEditorIntent =
        CanvasEditorIntent.SelectAt(screenX, screenY)

    fun selectNode(nodeId: String?): CanvasEditorIntent = CanvasEditorIntent.SelectNode(nodeId)

    fun beginDrag(nodeId: String): CanvasEditorIntent = CanvasEditorIntent.BeginDrag(nodeId)

    fun dragBy(deltaX: Float, deltaY: Float): CanvasEditorIntent =
        CanvasEditorIntent.DragBy(deltaX, deltaY)

    fun endDrag(): CanvasEditorIntent = CanvasEditorIntent.EndDrag

    fun addNode(node: CanvasNode): CanvasEditorIntent = CanvasEditorIntent.AddNode(node)

    fun removeNode(nodeId: String): CanvasEditorIntent = CanvasEditorIntent.RemoveNode(nodeId)

    fun setEditing(nodeId: String?): CanvasEditorIntent = CanvasEditorIntent.SetEditing(nodeId)

    fun useHandTool(): CanvasEditorIntent = CanvasEditorIntent.SetTool(CanvasTool.HAND)

    fun useSelectTool(): CanvasEditorIntent = CanvasEditorIntent.SetTool(CanvasTool.SELECT)

    fun isHandTool(state: CanvasEditorState): Boolean = state.tool == CanvasTool.HAND

    fun screenRectOf(node: CanvasNode, viewport: Viewport): CanvasRect = CanvasRect(
        x = viewport.toScreenX(node.rect.x),
        y = viewport.toScreenY(node.rect.y),
        width = node.rect.width * viewport.scale,
        height = node.rect.height * viewport.scale
    )
}
