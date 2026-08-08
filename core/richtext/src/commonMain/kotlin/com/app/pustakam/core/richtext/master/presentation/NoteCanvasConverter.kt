package com.app.pustakam.core.richtext.master.presentation

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind

object NoteCanvasConverter {

    const val COLUMN_GAP = 48f
    const val ROW_GAP = 32f
    const val MEDIA_PER_ROW = 3

    fun kindOf(content: NoteContentModel): CanvasNodeKind = when (content.type) {
        ContentType.TEXT -> CanvasNodeKind.MASTER_TEXT
        ContentType.LINK -> CanvasNodeKind.LINK
        ContentType.LOCATION -> CanvasNodeKind.LOCATION
        ContentType.PDF, ContentType.DOCX -> CanvasNodeKind.DOCUMENT
        else -> CanvasNodeKind.MEDIA
    }

    private fun isSequentialMedia(content: NoteContentModel): Boolean =
        content.type == ContentType.IMAGE ||
            content.type == ContentType.GIF ||
            content.type == ContentType.VIDEO

    /**
     * Note order becomes canvas layout: text and documents get their own full-width row, and
     * consecutive images/videos are grouped into a strip the way the book reader pages them.
     */
    fun toCanvas(contents: List<NoteContentModel>): CanvasDocument {
        val ordered = contents.sortedBy { it.position }
        val nodes = mutableListOf<CanvasNode>()
        var y = 0f
        var index = 0
        var z = 0
        var previousId: String? = null

        while (index < ordered.size) {
            val content = ordered[index]
            if (isSequentialMedia(content)) {
                val run = mutableListOf<NoteContentModel>()
                while (index < ordered.size && isSequentialMedia(ordered[index])) {
                    run.add(ordered[index])
                    index++
                }
                var x = 0f
                var rowHeight = 0f
                run.forEachIndexed { position, media ->
                    if (position > 0 && position % MEDIA_PER_ROW == 0) {
                        x = 0f
                        y += CanvasNode.DEFAULT_MEDIA_HEIGHT + ROW_GAP
                    }
                    val node = CanvasNode.of(
                        kind = CanvasNodeKind.MEDIA,
                        contentId = media.id,
                        x = x,
                        y = y,
                        width = CanvasNode.DEFAULT_MEDIA_WIDTH,
                        height = CanvasNode.DEFAULT_MEDIA_HEIGHT,
                        name = CanvasNode.defaultName(CanvasNodeKind.MEDIA, nodes.size)
                    ).copy(z = z++)
                    nodes.add(linkFrom(previousId, node, nodes))
                    previousId = node.id
                    x += CanvasNode.DEFAULT_MEDIA_WIDTH + COLUMN_GAP
                    rowHeight = CanvasNode.DEFAULT_MEDIA_HEIGHT
                }
                y += rowHeight + ROW_GAP
                continue
            }

            val kind = kindOf(content)
            val isText = kind == CanvasNodeKind.MASTER_TEXT
            val height = if (isText) CanvasNode.DEFAULT_TEXT_HEIGHT else CanvasNode.DEFAULT_MEDIA_HEIGHT
            val node = CanvasNode.of(
                kind = kind,
                contentId = content.id,
                x = 0f,
                y = y,
                width = if (isText) CanvasNode.DEFAULT_TEXT_WIDTH else CanvasNode.DEFAULT_MEDIA_WIDTH,
                height = height,
                name = CanvasNode.defaultName(kind, nodes.size)
            ).copy(z = z++)
            nodes.add(linkFrom(previousId, node, nodes))
            previousId = node.id
            y += height + ROW_GAP
            index++
        }
        return CanvasDocument(nodes)
    }

    private fun linkFrom(
        previousId: String?,
        node: CanvasNode,
        nodes: MutableList<CanvasNode>
    ): CanvasNode {
        if (previousId == null) return node
        val previousIndex = nodes.indexOfFirst { it.id == previousId }
        if (previousIndex >= 0) {
            nodes[previousIndex] = nodes[previousIndex].linkedTo(node.id)
        }
        return node.copy(parentId = previousId)
    }

    /**
     * Canvas back to a linear note. Link chains define the sequence; anything not in a chain
     * falls back to reading order — top to bottom, then left to right.
     */
    fun toOrderedContentIds(document: CanvasDocument): List<String> {
        val nodes = document.nodes.filter { it.contentId != null }
        if (nodes.isEmpty()) return emptyList()

        val byId = nodes.associateBy { it.id }
        val linked = nodes.flatMap { it.links }.toSet()
        val heads = nodes.filter { it.id !in linked }.sortedWith(readingOrder)

        val visited = mutableSetOf<String>()
        val ordered = mutableListOf<CanvasNode>()

        fun walk(node: CanvasNode) {
            if (!visited.add(node.id)) return
            ordered.add(node)
            node.links.mapNotNull { byId[it] }.sortedWith(readingOrder).forEach { walk(it) }
        }

        heads.forEach { walk(it) }
        nodes.sortedWith(readingOrder).forEach { walk(it) }

        return ordered.mapNotNull { it.contentId }
    }

    fun reorderContents(
        contents: List<NoteContentModel>,
        document: CanvasDocument
    ): List<NoteContentModel> {
        val order = toOrderedContentIds(document)
        if (order.isEmpty()) return contents
        val rank = order.withIndex().associate { (index, id) -> id to index }
        return contents
            .sortedBy { rank[it.id] ?: Int.MAX_VALUE }
            .mapIndexed { index, content -> content.repositioned(index.toDouble()) }
    }

    private fun NoteContentModel.repositioned(newPosition: Double): NoteContentModel = when (this) {
        is NoteContentModel.TextContent -> copy(position = newPosition)
        is NoteContentModel.MediaContent -> copy(position = newPosition)
        is NoteContentModel.Link -> copy(position = newPosition)
        is NoteContentModel.Location -> copy(position = newPosition)
    }

    private val readingOrder = compareBy<CanvasNode>({ it.rect.y }, { it.rect.x })
}
