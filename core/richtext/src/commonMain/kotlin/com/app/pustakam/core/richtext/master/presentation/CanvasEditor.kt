package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind
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

    data class RenameNode(val nodeId: String, val name: String) : CanvasEditorIntent()

    data class ReparentNode(val nodeId: String, val parentId: String?) : CanvasEditorIntent()

    data class ReplaceDocument(val document: CanvasDocument) : CanvasEditorIntent()
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

            // tapping the node that is already being edited must NOT drop focus — doing so
            // resigned and re-claimed the iOS keyboard on every tap inside the text
            is CanvasEditorIntent.SelectAt -> {
                val canvasX = state.viewport.toCanvasX(intent.screenX)
                val canvasY = state.viewport.toCanvasY(intent.screenY)
                val hit = state.document.hitTest(canvasX, canvasY)
                state.copy(
                    selectedNodeId = hit?.id,
                    editingNodeId = state.editingNodeId.takeIf { it == hit?.id }
                )
            }

            is CanvasEditorIntent.SelectNode -> state.copy(
                selectedNodeId = intent.nodeId,
                editingNodeId = state.editingNodeId.takeIf { it == intent.nodeId }
            )

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
                    val dx = intent.deltaX / state.viewport.scale
                    val dy = intent.deltaY / state.viewport.scale
                    // a page carries the widgets sitting on it
                    val moved = (listOf(node) + state.document.descendantsOf(node.id))
                        .map { it.movedBy(dx, dy) }
                    state.copy(document = state.document.replacingAll(moved))
                }
            }

            // a widget released over a page joins that page; released on bare canvas it leaves
            CanvasEditorIntent.EndDrag -> {
                val dragged = state.draggingNodeId?.let { state.document.nodeById(it) }
                if (dragged == null || dragged.isPage) {
                    state.copy(draggingNodeId = null)
                } else {
                    val target = state.document.pageAt(dragged.rect.centerX, dragged.rect.centerY)
                    val document =
                        if (target?.id == dragged.parentId) state.document
                        else state.document
                            .replacing(dragged.reparentedTo(target?.id))
                            .broughtToFront(dragged.id)
                    state.copy(draggingNodeId = null, document = document)
                }
            }

            is CanvasEditorIntent.ReparentNode -> {
                val node = state.document.nodeById(intent.nodeId)
                if (node == null || node.id == intent.parentId) state
                else state.copy(
                    document = state.document
                        .replacing(node.reparentedTo(intent.parentId))
                        .broughtToFront(node.id)
                )
            }

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

            // removing a page never deletes its widgets — they fall back onto the bare canvas
            is CanvasEditorIntent.RemoveNode -> {
                val removed = state.document.nodeById(intent.nodeId)
                val orphans = state.document.childrenOf(intent.nodeId)
                    .map { it.reparentedTo(removed?.parentId) }
                state.copy(
                    document = state.document.replacingAll(orphans).removing(intent.nodeId),
                    selectedNodeId = state.selectedNodeId.takeIf { it != intent.nodeId },
                    editingNodeId = state.editingNodeId.takeIf { it != intent.nodeId }
                )
            }

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

            is CanvasEditorIntent.RenameNode -> {
                val node = state.document.nodeById(intent.nodeId)
                if (node == null) state
                else state.copy(document = state.document.replacing(node.renamedTo(intent.name)))
            }

            is CanvasEditorIntent.ReplaceDocument ->
                state.copy(document = intent.document, selectedNodeId = null, editingNodeId = null)
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

    fun linkNodes(fromId: String, toId: String): CanvasEditorIntent =
        CanvasEditorIntent.LinkNodes(fromId, toId)

    fun anchorOf(state: CanvasEditorState): CanvasNode? =
        (state.editingNodeId ?: state.selectedNodeId)?.let { state.document.nodeById(it) }

    fun linkedNodes(state: CanvasEditorState, nodeId: String): List<CanvasNode> =
        state.document.linkedTo(nodeId)

    fun removeNode(nodeId: String): CanvasEditorIntent = CanvasEditorIntent.RemoveNode(nodeId)

    fun renameNode(nodeId: String, name: String): CanvasEditorIntent =
        CanvasEditorIntent.RenameNode(nodeId, name)

    fun replaceDocument(document: CanvasDocument): CanvasEditorIntent =
        CanvasEditorIntent.ReplaceDocument(document)

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

    // 🔧 09-Aug-2026 G8: Swift cannot see Kotlin default arguments or nested sealed subtypes,
    //   so every construction and every intent test below is exposed as a plain function
    fun initialState(): CanvasEditorState = CanvasEditorState()

    fun loaded(
        state: CanvasEditorState,
        nodes: List<CanvasNode>,
        viewport: Viewport?
    ): CanvasEditorState = state.copy(
        document = CanvasDocument(nodes),
        viewport = viewport?.withSize(state.viewport.widthPx, state.viewport.heightPx)
            ?: state.viewport
    )

    fun isEndDrag(intent: CanvasEditorIntent): Boolean = intent is CanvasEditorIntent.EndDrag

    fun addedNode(intent: CanvasEditorIntent): CanvasNode? =
        (intent as? CanvasEditorIntent.AddNode)?.node

    fun removedNodeId(intent: CanvasEditorIntent): String? =
        (intent as? CanvasEditorIntent.RemoveNode)?.nodeId

    fun resizedNodeId(intent: CanvasEditorIntent): String? =
        (intent as? CanvasEditorIntent.ResizeNode)?.nodeId

    fun reparentedNodeId(intent: CanvasEditorIntent): String? =
        (intent as? CanvasEditorIntent.ReparentNode)?.nodeId

    fun affectsViewport(intent: CanvasEditorIntent): Boolean = when (intent) {
        is CanvasEditorIntent.Pan,
        is CanvasEditorIntent.Zoom,
        is CanvasEditorIntent.ZoomTo,
        CanvasEditorIntent.ZoomIn,
        CanvasEditorIntent.ZoomOut,
        CanvasEditorIntent.ZoomToFit,
        is CanvasEditorIntent.FocusNode -> true

        else -> false
    }

    fun defaultWidth(kind: CanvasNodeKind): Float =
        if (kind == CanvasNodeKind.MASTER_TEXT) CanvasNode.DEFAULT_TEXT_WIDTH
        else CanvasNode.DEFAULT_MEDIA_WIDTH

    fun defaultHeight(kind: CanvasNodeKind): Float =
        if (kind == CanvasNodeKind.MASTER_TEXT) CanvasNode.DEFAULT_TEXT_HEIGHT
        else CanvasNode.DEFAULT_MEDIA_HEIGHT

    fun nodeNextTo(anchor: CanvasNode, kind: CanvasNodeKind, contentId: String?): CanvasNode =
        CanvasNode.nextTo(
            anchor = anchor,
            kind = kind,
            contentId = contentId,
            width = defaultWidth(kind),
            height = defaultHeight(kind)
        )

    fun nodeAtEdge(
        state: CanvasEditorState,
        kind: CanvasNodeKind,
        contentId: String?
    ): CanvasNode {
        val bounds = state.document.bounds
        return CanvasNode.of(
            kind = kind,
            contentId = contentId,
            x = bounds.right + CanvasNode.DEFAULT_GAP,
            y = bounds.y,
            width = defaultWidth(kind),
            height = defaultHeight(kind)
        )
    }

    /** The page new widgets land on: the selected node's page, else the first page. */
    fun selectedPage(state: CanvasEditorState): CanvasNode? {
        val focus = anchorOf(state) ?: return state.document.pages.firstOrNull()
        return state.document.pageOf(focus.id) ?: state.document.pages.firstOrNull()
    }

    fun pageAt(state: CanvasEditorState, canvasX: Float, canvasY: Float): CanvasNode? =
        state.document.pageAt(canvasX, canvasY)

    fun childrenOf(state: CanvasEditorState, nodeId: String): List<CanvasNode> =
        state.document.childrenOf(nodeId)

    fun isPage(node: CanvasNode): Boolean = node.isPage

    fun reparentNode(nodeId: String, parentId: String?): CanvasEditorIntent =
        CanvasEditorIntent.ReparentNode(nodeId, parentId)

    /** Stacks a new widget down the page, below whatever is already on it. */
    fun childNodeIn(
        state: CanvasEditorState,
        page: CanvasNode,
        kind: CanvasNodeKind,
        contentId: String?
    ): CanvasNode {
        val siblings = state.document.childrenOf(page.id)
        val width = minOf(defaultWidth(kind), page.rect.width - PAGE_PADDING * 2f)
        val top = siblings.maxOfOrNull { it.rect.bottom }?.plus(PAGE_PADDING)
            ?: (page.rect.y + PAGE_PADDING)
        return CanvasNode.of(
            kind = kind,
            contentId = contentId,
            x = page.rect.x + PAGE_PADDING,
            y = top,
            width = maxOf(width, CanvasNode.MIN_SIZE),
            height = defaultHeight(kind),
            parentId = page.id
        )
    }

    /**
     * A new MASTER_TEXT is a new page beside the others; anything else is a widget placed on
     * the selected page, falling back to free canvas when the note has no page yet.
     */
    fun nodeFor(state: CanvasEditorState, kind: CanvasNodeKind, contentId: String?): CanvasNode {
        if (kind == CanvasNodeKind.MASTER_TEXT) {
            val anchor = anchorOf(state)?.let { state.document.pageOf(it.id) }
            return if (anchor == null) nodeAtEdge(state, kind, contentId)
            else nodeNextTo(anchor, kind, contentId)
        }
        val page = selectedPage(state) ?: return nodeAtEdge(state, kind, contentId)
        return childNodeIn(state, page, kind, contentId)
    }

    fun resizedTo(
        state: CanvasEditorState,
        nodeId: String,
        deltaXPx: Float,
        deltaYPx: Float
    ): CanvasEditorIntent? {
        val node = state.document.nodeById(nodeId)?.takeIf { !it.locked } ?: return null
        val scale = if (state.viewport.scale <= 0f) 1f else state.viewport.scale
        return CanvasEditorIntent.ResizeNode(
            nodeId = nodeId,
            width = node.rect.width + deltaXPx / scale,
            height = node.rect.height + deltaYPx / scale
        )
    }

    const val PAGE_PADDING = 24f

    fun stackedTextNodes(contentIds: List<String>): List<CanvasNode> {
        var y = 0f
        return contentIds.mapIndexed { index, contentId ->
            val node = CanvasNode.masterText(contentId = contentId, x = 0f, y = y)
            y += CanvasNode.DEFAULT_TEXT_HEIGHT + CanvasNode.DEFAULT_GAP
            node.copy(z = index)
        }
    }
}
