package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode

import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport

enum class CanvasTool {
    SELECT,
    HAND,
    ZOOM,
    LOCK
}

enum class CanvasGesture {
    NONE,
    DRAGGING,
    RESIZING,
    EDITING
}

data class CanvasPermits(
    val canSelect: Boolean,
    val canPan: Boolean,
    val canZoom: Boolean,
    val canDragNode: Boolean,
    val canResizeNode: Boolean,
    val canEditText: Boolean,
    val canMutateDocument: Boolean
)

data class CanvasEditorState(
    val document: CanvasDocument = CanvasDocument(),
    val viewport: Viewport = Viewport(),
    val tool: CanvasTool = CanvasTool.SELECT,
    val selectedNodeId: String? = null,
    val draggingNodeId: String? = null,
    val editingNodeId: String? = null,
    val resizingNodeId: String? = null,
    val focusedRect: CanvasRect? = null
) {
    val visibleNodes: List<CanvasNode>
        get() = document.visibleIn(viewport.visibleRect)

    val selectedNode: CanvasNode? get() = selectedNodeId?.let { document.nodeById(it) }

    val zoomPercent: Int get() = viewport.percent

    fun isVisible(nodeId: String): Boolean = visibleNodes.any { it.id == nodeId }

    val gesture: CanvasGesture
        get() = when {
            editingNodeId != null -> CanvasGesture.EDITING
            resizingNodeId != null -> CanvasGesture.RESIZING
            draggingNodeId != null -> CanvasGesture.DRAGGING
            else -> CanvasGesture.NONE
        }

    val permits: CanvasPermits
        get() = when (gesture) {
            CanvasGesture.EDITING -> CanvasPermits(
                canSelect = false,
                canPan = false,
                canZoom = false,
                canDragNode = false,
                canResizeNode = false,
                canEditText = true,
                canMutateDocument = true
            )

            CanvasGesture.RESIZING -> CanvasPermits(
                canSelect = false,
                canPan = false,
                canZoom = false,
                canDragNode = false,
                canResizeNode = true,
                canEditText = false,
                canMutateDocument = false
            )

            CanvasGesture.DRAGGING -> CanvasPermits(
                canSelect = false,
                canPan = false,
                canZoom = false,
                canDragNode = true,
                canResizeNode = false,
                canEditText = false,
                canMutateDocument = false
            )

            CanvasGesture.NONE -> when (tool) {
                CanvasTool.LOCK -> CanvasPermits(
                    canSelect = true,
                    canPan = false,
                    canZoom = false,
                    canDragNode = false,
                    canResizeNode = false,
                    canEditText = false,
                    canMutateDocument = false
                )

                CanvasTool.ZOOM -> CanvasPermits(
                    canSelect = false,
                    canPan = true,
                    canZoom = true,
                    canDragNode = false,
                    canResizeNode = false,
                    canEditText = false,
                    canMutateDocument = false
                )

                CanvasTool.HAND -> CanvasPermits(
                    canSelect = false,
                    canPan = true,
                    canZoom = true,
                    canDragNode = false,
                    canResizeNode = false,
                    canEditText = false,
                    canMutateDocument = false
                )

                CanvasTool.SELECT -> CanvasPermits(
                    canSelect = true,
                    canPan = true,
                    canZoom = true,
                    canDragNode = true,
                    canResizeNode = true,
                    canEditText = true,
                    canMutateDocument = true
                )
            }
        }
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

    data class BeginResize(val nodeId: String) : CanvasEditorIntent()

    data object EndResize : CanvasEditorIntent()

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

const val EDIT_TOP_INSET = 12f

const val PAGE_SCREEN_MARGIN = 16f

object CanvasEditorReducer {

    fun reduce(state: CanvasEditorState, intent: CanvasEditorIntent): CanvasEditorState =
        if (blocked(state, intent)) state else apply(state, intent)


    /**
     * A single tap on a page enters edit mode: the canvas stays at 100%, the page itself is
     * resized to the device screen less a margin, and the permits for EDITING lock it there.
     * Growing widgets scroll inside the page and never move its boundary.
     */
    private fun fittedToScreen(
        state: CanvasEditorState,
        page: CanvasNode
    ): CanvasEditorState {
        val viewport = state.viewport
        if (viewport.widthPx <= 0f || viewport.heightPx <= 0f) return state
        val width = viewport.widthPx - PAGE_SCREEN_MARGIN * 2f
        val height = viewport.heightPx - PAGE_SCREEN_MARGIN * 2f
        if (width <= CanvasNode.MIN_SIZE || height <= CanvasNode.MIN_SIZE) return state

        val resized = page.resizedTo(width, height)
        val settled = state.document.replacing(resized)
            .withoutOverlap(resized, CanvasNode.DEFAULT_GAP)
        return state.copy(
            document = state.document.replacing(settled),
            viewport = viewport.copy(
                scale = Viewport.DEFAULT_SCALE,
                offsetX = PAGE_SCREEN_MARGIN - settled.rect.x,
                offsetY = PAGE_SCREEN_MARGIN - settled.rect.y
            )
        )
    }

    private fun blocked(state: CanvasEditorState, intent: CanvasEditorIntent): Boolean {
        val permits = state.permits
        return when (intent) {
            is CanvasEditorIntent.Pan -> !permits.canPan
            is CanvasEditorIntent.Zoom,
            is CanvasEditorIntent.ZoomTo,
            CanvasEditorIntent.ZoomIn,
            CanvasEditorIntent.ZoomOut,
            CanvasEditorIntent.ZoomToFit -> !permits.canZoom

            is CanvasEditorIntent.BeginDrag -> !permits.canDragNode
            is CanvasEditorIntent.DragBy -> !permits.canDragNode
            is CanvasEditorIntent.BeginResize -> !permits.canResizeNode
            is CanvasEditorIntent.ResizeNode -> !permits.canResizeNode
            is CanvasEditorIntent.SetEditing ->
                intent.nodeId != null && !permits.canEditText

            is CanvasEditorIntent.AddNode,
            is CanvasEditorIntent.RemoveNode,
            is CanvasEditorIntent.ReparentNode,
            is CanvasEditorIntent.RenameNode -> !permits.canMutateDocument

            else -> false
        }
    }

    private fun apply(state: CanvasEditorState, intent: CanvasEditorIntent): CanvasEditorState =
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
                else fittedToScreen(state.copy(selectedNodeId = node.id), node)
            }

            // tapping the node that is already being edited must NOT drop focus — doing so
            // resigned and re-claimed the iOS keyboard on every tap inside the text
            is CanvasEditorIntent.SelectAt -> {
                val canvasX = state.viewport.toCanvasX(intent.screenX)
                val canvasY = state.viewport.toCanvasY(intent.screenY)
                val hit = state.document.hitTest(canvasX, canvasY)
                val staysEditing = state.editingNodeId != null && state.editingNodeId == hit?.id
                if (staysEditing) {
                    state.copy(selectedNodeId = hit?.id)
                } else if (hit != null && hit.isPage) {
                    fittedToScreen(
                        state.copy(
                            selectedNodeId = hit.id,
                            editingNodeId = hit.id,
                            focusedRect = null
                        ),
                        hit
                    )
                } else {
                    state.copy(
                        selectedNodeId = hit?.id,
                        editingNodeId = null,
                        viewport = state.focusedRect
                            ?.let { state.viewport.focusedOn(it) } ?: state.viewport,
                        focusedRect = null
                    )
                }
            }

            is CanvasEditorIntent.SelectNode -> {
                val node = intent.nodeId?.let { state.document.nodeById(it) }
                if (node != null && node.isPage && state.editingNodeId != node.id) {
                    fittedToScreen(
                        state.copy(
                            selectedNodeId = node.id,
                            editingNodeId = node.id,
                            focusedRect = null
                        ),
                        node
                    )
                } else {
                    state.copy(
                        selectedNodeId = intent.nodeId,
                        editingNodeId = state.editingNodeId.takeIf { it == intent.nodeId }
                    )
                }
            }

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
                if (dragged == null) {
                    state.copy(draggingNodeId = null)
                } else if (dragged.isPage) {
                    val settled = state.document.withoutOverlap(dragged, CanvasNode.DEFAULT_GAP)
                    val shiftX = settled.rect.x - dragged.rect.x
                    val moved = listOf(settled) + state.document.descendantsOf(dragged.id)
                        .map { it.movedBy(shiftX, 0f) }
                    state.copy(
                        draggingNodeId = null,
                        document = state.document.replacingAll(moved)
                    )
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

            is CanvasEditorIntent.BeginResize ->
                state.copy(resizingNodeId = intent.nodeId, selectedNodeId = intent.nodeId)

            CanvasEditorIntent.EndResize -> state.copy(resizingNodeId = null)

            is CanvasEditorIntent.SetTool -> state.copy(
                tool = intent.tool,
                draggingNodeId = null,
                resizingNodeId = null,
                editingNodeId = null
            )

            is CanvasEditorIntent.SetEditing -> {
                val node = intent.nodeId?.let { state.document.nodeById(it) }
                if (node == null) {
                    state.copy(
                        editingNodeId = null,
                        viewport = state.focusedRect?.let { state.viewport.focusedOn(it) }
                            ?: state.viewport,
                        focusedRect = null
                    )
                } else {
                    state.copy(
                        editingNodeId = node.id,
                        selectedNodeId = node.id,
                        draggingNodeId = null,
                        resizingNodeId = null,
                        focusedRect = state.focusedRect ?: state.viewport.visibleRect,
                        viewport = state.viewport.copy(
                            scale = Viewport.DEFAULT_SCALE,
                            offsetX = PAGE_SCREEN_MARGIN - node.rect.x,
                            offsetY = PAGE_SCREEN_MARGIN - node.rect.y
                        )
                    )
                }
            }

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

    fun useZoomTool(): CanvasEditorIntent = CanvasEditorIntent.SetTool(CanvasTool.ZOOM)

    fun useLockTool(): CanvasEditorIntent = CanvasEditorIntent.SetTool(CanvasTool.LOCK)

    fun setTool(tool: CanvasTool): CanvasEditorIntent = CanvasEditorIntent.SetTool(tool)

    fun tools(): List<CanvasTool> = listOf(
        CanvasTool.SELECT,
        CanvasTool.HAND,
        CanvasTool.ZOOM,
        CanvasTool.LOCK
    )

    fun toolLabel(tool: CanvasTool): String = when (tool) {
        CanvasTool.SELECT -> "Select"
        CanvasTool.HAND -> "Hand"
        CanvasTool.ZOOM -> "Zoom"
        CanvasTool.LOCK -> "Lock"
    }

    fun toolKey(tool: CanvasTool): String = tool.name

    fun isTool(state: CanvasEditorState, tool: CanvasTool): Boolean = state.tool == tool

    fun isHandTool(state: CanvasEditorState): Boolean = state.tool == CanvasTool.HAND

    fun permitsOf(state: CanvasEditorState): CanvasPermits = state.permits

    fun isEditing(state: CanvasEditorState): Boolean =
        state.gesture == CanvasGesture.EDITING

    fun beginResize(nodeId: String): CanvasEditorIntent = CanvasEditorIntent.BeginResize(nodeId)

    fun endResize(): CanvasEditorIntent = CanvasEditorIntent.EndResize

    fun exitEditing(): CanvasEditorIntent = CanvasEditorIntent.SetEditing(null)

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
    ): CanvasEditorState {
        val restored = viewport
            ?.takeIf { it.scale in SANE_MIN_SCALE..SANE_MAX_SCALE }
            ?.withSize(state.viewport.widthPx, state.viewport.heightPx)
        return state.copy(
            document = CanvasDocument(nodes),
            viewport = restored ?: state.viewport.copy(
                scale = Viewport.DEFAULT_SCALE,
                offsetX = 0f,
                offsetY = 0f
            )
        )
    }

    const val SANE_MIN_SCALE = 0.25f

    const val SANE_MAX_SCALE = 4f

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
        is CanvasEditorIntent.SelectAt,
        is CanvasEditorIntent.SelectNode,
        is CanvasEditorIntent.FocusNode -> true

        else -> false
    }

    /** The text field a freshly opened canvas should land in. */
    fun lastTextNodeId(state: CanvasEditorState): String? = state.document.pages.lastOrNull()?.id

    /** The page a select has just fitted to the screen, so its new size can be stored. */
    fun fittedPageId(state: CanvasEditorState, intent: CanvasEditorIntent): String? =
        when (intent) {
            is CanvasEditorIntent.SelectAt,
            is CanvasEditorIntent.SelectNode -> state.selectedNode?.takeIf { it.isPage }?.id

            else -> null
        }

    fun defaultWidth(kind: ContentType): Float =
        if (kind == ContentType.TEXT) CanvasNode.DEFAULT_TEXT_WIDTH
        else CanvasNode.DEFAULT_MEDIA_WIDTH

    fun defaultHeight(kind: ContentType): Float =
        if (kind == ContentType.TEXT) CanvasNode.DEFAULT_TEXT_HEIGHT
        else CanvasNode.DEFAULT_MEDIA_HEIGHT

    fun nodeNextTo(anchor: CanvasNode, kind: ContentType, contentId: String?): CanvasNode =
        CanvasNode.nextTo(
            anchor = anchor,
            kind = kind,
            contentId = contentId,
            width = defaultWidth(kind),
            height = defaultHeight(kind)
        )

    fun nodeAtEdge(
        state: CanvasEditorState,
        kind: ContentType,
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
        kind: ContentType,
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
    fun nodeFor(state: CanvasEditorState, kind: ContentType, contentId: String?): CanvasNode {
        if (kind == ContentType.TEXT) {
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

    const val CARET_MARGIN = 24f

    /** Lines of clearance kept between the caret and the keyboard. */
    const val CARET_TRAILING_LINES = 4

    fun caretRevealPadding(lineHeightPx: Float): Float =
        if (lineHeightPx > 0f) lineHeightPx * CARET_TRAILING_LINES else CARET_MARGIN

    fun pageForSpawn(state: CanvasEditorState): CanvasNode? = selectedPage(state)

    fun needsPage(state: CanvasEditorState): Boolean = state.document.pages.isEmpty()

    fun pageNode(state: CanvasEditorState, contentId: String?): CanvasNode {
        val anchor = state.document.pages.lastOrNull()
        return if (anchor == null) {
            CanvasNode.masterText(contentId = contentId, x = 0f, y = 0f)
        } else {
            nodeNextTo(anchor, ContentType.TEXT, contentId)
        }
    }

    fun widgetIn(
        state: CanvasEditorState,
        page: CanvasNode,
        kind: ContentType,
        contentId: String?
    ): CanvasNode = childNodeIn(state, page, kind, contentId)

    fun stackedTextNodes(contentIds: List<String>): List<CanvasNode> {
        var y = 0f
        return contentIds.mapIndexed { index, contentId ->
            val node = CanvasNode.masterText(contentId = contentId, x = 0f, y = y)
            y += CanvasNode.DEFAULT_TEXT_HEIGHT + CanvasNode.DEFAULT_GAP
            node.copy(z = index)
        }
    }
}
