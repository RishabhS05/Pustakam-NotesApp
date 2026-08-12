package com.app.pustakam.core.richtext.master.model

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.common.util.ContentType.*
import com.app.pustakam.core.common.util.UniqueIdGenerator

data class CanvasNode(
    val id: String,
    val kind: ContentType,
    val name: String = "",
    val rect: CanvasRect,
    val z: Int = 0,
    val contentId: String? = null,
    val parentId: String? = null,
    val locked: Boolean = false,
    val hidden: Boolean = false,
    val links: List<String> = emptyList()
) {
    fun movedBy(deltaX: Float, deltaY: Float): CanvasNode =
        copy(rect = rect.translated(deltaX, deltaY))

    fun resizedTo(width: Float, height: Float): CanvasNode =
        copy(rect = rect.copy(width = maxOf(width, MIN_SIZE), height = maxOf(height, MIN_SIZE)))

    fun movedTo(x: Float, y: Float): CanvasNode = copy(rect = rect.copy(x = x, y = y))

    fun renamedTo(newName: String): CanvasNode = copy(name = newName)

    fun displayName(fallbackIndex: Int = 0): String =
        if (name.isNotBlank()) name else defaultName(kind, fallbackIndex)

    fun linkedTo(other: String): CanvasNode =
        if (links.contains(other)) this else copy(links = links + other)

    fun reparentedTo(newParentId: String?): CanvasNode = copy(parentId = newParentId)

    fun raisedTo(newZ: Int): CanvasNode = copy(z = newZ)

    val isPage: Boolean get() = parentId == null && kind == ContentType.TEXT

    companion object {
        const val MIN_SIZE = 24f

        // 🔧 09-Aug-2026: a 720-wide page fit a phone at ~0.5 zoom, which rendered 16sp body
        //   text at 8sp. Narrower paper keeps the fitted zoom — and the type — readable.
        const val DEFAULT_TEXT_WIDTH = 360f
        const val DEFAULT_TEXT_HEIGHT = 640f
        const val DEFAULT_GAP = 48f
        const val DEFAULT_MEDIA_WIDTH = 420f
        const val DEFAULT_MEDIA_HEIGHT = 320f

        /** Body type on canvas paper sits a notch above the note editor's. */
        const val BASE_FONT_SCALE = 1.15f

        fun defaultName(kind: ContentType, index: Int): String = when (kind) {
        TEXT -> "Page ${index + 1}"
        AUDIO, IMAGE, VIDEO, GIF -> "Media ${index + 1}"
            TABLE -> "Table ${index + 1}"
        DOCX, PDF, TXT, MD, EPUB, OTHER -> "Document ${index + 1}"
           LINK -> "Link ${index + 1}"
           LOCATION -> "Location ${index + 1}"
            DRAWING -> "Drawing ${index + 1}"
            FORMULA -> ""
        }

        fun of(
            kind: ContentType,
            contentId: String?,
            x: Float,
            y: Float,
            width: Float = DEFAULT_TEXT_WIDTH,
            height: Float = DEFAULT_TEXT_HEIGHT,
            parentId: String? = null,
            name: String = ""
        ): CanvasNode = CanvasNode(
            id = UniqueIdGenerator.generateUniqueId(),
            kind = kind,
            name = name,
            rect = CanvasRect(x, y, width, height),
            contentId = contentId,
            parentId = parentId
        )

        fun nextTo(
            anchor: CanvasNode,
            kind: ContentType,
            contentId: String?,
            width: Float = DEFAULT_TEXT_WIDTH,
            height: Float = DEFAULT_TEXT_HEIGHT,
            gap: Float = DEFAULT_GAP
        ): CanvasNode = of(
            kind = kind,
            contentId = contentId,
            x = anchor.rect.right + gap,
            y = anchor.rect.y,
            width = width,
            height = height,
            parentId = anchor.id
        )

        fun masterText(
            contentId: String?,
            x: Float = 0f,
            y: Float = 0f,
            width: Float = DEFAULT_TEXT_WIDTH,
            height: Float = DEFAULT_TEXT_HEIGHT
        ): CanvasNode = CanvasNode(
            id = UniqueIdGenerator.generateUniqueId(),
            kind = ContentType.TEXT,
            rect = CanvasRect(x, y, width, height),
            contentId = contentId
        )
    }
}

data class CanvasDocument(
    val nodes: List<CanvasNode> = emptyList()
) {
    val bounds: CanvasRect
        get() = nodes.map { it.rect }.reduceOrNull { acc, rect -> acc.union(rect) } ?: CanvasRect()

    /** Top-level paper. Widgets live on a page through [CanvasNode.parentId]. */
    val pages: List<CanvasNode>
        get() = nodes.filter { it.parentId == null && it.kind == TEXT }

    fun nodeById(nodeId: String): CanvasNode? = nodes.firstOrNull { it.id == nodeId }

    fun nodeForContent(contentId: String): CanvasNode? =
        nodes.firstOrNull { it.contentId == contentId }

    fun childrenOf(nodeId: String): List<CanvasNode> = nodes.filter { it.parentId == nodeId }

    fun descendantsOf(nodeId: String): List<CanvasNode> {
        val direct = childrenOf(nodeId)
        return direct + direct.flatMap { descendantsOf(it.id) }
    }

    /** The page a point lands on, topmost first — this is what a drop re-parents to. */
    fun pageAt(canvasX: Float, canvasY: Float): CanvasNode? =
        pages.filterNot { it.hidden }.sortedBy { it.z }
            .lastOrNull { it.rect.contains(canvasX, canvasY) }

    fun pageOf(nodeId: String): CanvasNode? =
        nodeById(nodeId)?.let { node ->
            if (node.parentId == null) node.takeIf { it.kind == TEXT }
            else pageOf(node.parentId)
        }

    fun overlaps(node: CanvasNode): CanvasNode? =
        pages.firstOrNull { it.id != node.id && it.rect.intersects(node.rect) }

    fun withoutOverlap(node: CanvasNode, gap: Float): CanvasNode {
        var placed = node
        var guard = 0
        while (guard < 64) {
            val clash = overlaps(placed) ?: return placed
            placed = placed.movedTo(clash.rect.right + gap, placed.rect.y)
            guard++
        }
        return placed
    }

    fun replacingAll(updated: List<CanvasNode>): CanvasDocument {
        if (updated.isEmpty()) return this
        val byId = updated.associateBy { it.id }
        return copy(nodes = nodes.map { byId[it.id] ?: it })
    }

    fun replacing(node: CanvasNode): CanvasDocument {
        val index = nodes.indexOfFirst { it.id == node.id }
        if (index < 0) return copy(nodes = nodes + node)
        return copy(nodes = nodes.toMutableList().apply { set(index, node) })
    }

    fun adding(node: CanvasNode): CanvasDocument = copy(nodes = nodes + node)

    fun removing(nodeId: String): CanvasDocument = copy(nodes = nodes.filterNot { it.id == nodeId })

    fun broughtToFront(nodeId: String): CanvasDocument {
        val top = (nodes.maxOfOrNull { it.z } ?: 0) + 1
        return nodeById(nodeId)?.let { replacing(it.copy(z = top)) } ?: this
    }

    fun inDrawOrder(): List<CanvasNode> = nodes.filterNot { it.hidden }.sortedBy { it.z }

    fun visibleIn(rect: CanvasRect, overscan: Float = OVERSCAN): List<CanvasNode> {
        val area = rect.inflated(overscan)
        return inDrawOrder().filter { it.rect.intersects(area) }
    }

    fun hitTest(canvasX: Float, canvasY: Float): CanvasNode? =
        inDrawOrder().lastOrNull { !it.locked && it.rect.contains(canvasX, canvasY) }

    fun linkedTo(nodeId: String): List<CanvasNode> {
        val node = nodeById(nodeId) ?: return emptyList()
        val outgoing = node.links.mapNotNull { nodeById(it) }
        val incoming = nodes.filter { it.links.contains(nodeId) }
        return (outgoing + incoming).distinctBy { it.id }
    }

    companion object {
        const val OVERSCAN = 240f
    }
}
