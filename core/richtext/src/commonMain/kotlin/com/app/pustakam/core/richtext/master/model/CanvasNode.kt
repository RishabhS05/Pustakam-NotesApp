package com.app.pustakam.core.richtext.master.model

import com.app.pustakam.core.common.util.UniqueIdGenerator
import kotlinx.serialization.Serializable

@Serializable
enum class CanvasNodeKind {
    MASTER_TEXT,
    MEDIA,
    TABLE,
    DOCUMENT,
    LINK,
    LOCATION,
    DRAWING
}

@Serializable
data class CanvasNode(
    val id: String,
    val kind: CanvasNodeKind,
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

    fun linkedTo(other: String): CanvasNode =
        if (links.contains(other)) this else copy(links = links + other)

    companion object {
        const val MIN_SIZE = 24f
        const val DEFAULT_TEXT_WIDTH = 720f
        const val DEFAULT_TEXT_HEIGHT = 960f

        fun masterText(
            contentId: String?,
            x: Float = 0f,
            y: Float = 0f,
            width: Float = DEFAULT_TEXT_WIDTH,
            height: Float = DEFAULT_TEXT_HEIGHT
        ): CanvasNode = CanvasNode(
            id = UniqueIdGenerator.generateUniqueId(),
            kind = CanvasNodeKind.MASTER_TEXT,
            rect = CanvasRect(x, y, width, height),
            contentId = contentId
        )
    }
}

@Serializable
data class CanvasDocument(
    val nodes: List<CanvasNode> = emptyList()
) {
    val bounds: CanvasRect
        get() = nodes.map { it.rect }.reduceOrNull { acc, rect -> acc.union(rect) } ?: CanvasRect()

    fun nodeById(nodeId: String): CanvasNode? = nodes.firstOrNull { it.id == nodeId }

    fun nodeForContent(contentId: String): CanvasNode? =
        nodes.firstOrNull { it.contentId == contentId }

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
