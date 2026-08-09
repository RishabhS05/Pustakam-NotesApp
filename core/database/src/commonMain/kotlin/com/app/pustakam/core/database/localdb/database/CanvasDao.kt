package com.app.pustakam.core.database.localdb.database

import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.database.NotesDatabase
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind
import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport

class CanvasDao(private val database: NotesDatabase) {

    private val queries get() = database.notesDatabaseQueries

    fun upsert(noteId: String, node: CanvasNode) {
        queries.upsertCanvasNode(
            id = node.id,
            noteId = noteId,
            kind = node.kind.name,
            name = node.name,
            contentId = node.contentId,
            parentId = node.parentId,
            x = node.rect.x.toDouble(),
            y = node.rect.y.toDouble(),
            width = node.rect.width.toDouble(),
            height = node.rect.height.toDouble(),
            z = node.z.toLong(),
            locked = if (node.locked) 1L else 0L,
            hidden = if (node.hidden) 1L else 0L,
            links = node.links.joinToString(LINK_SEPARATOR),
            updatedAt = getCurrentTimestamp().toString()
        )
    }

    fun upsertAll(noteId: String, nodes: List<CanvasNode>) {
        queries.transaction { nodes.forEach { upsert(noteId, it) } }
    }

    fun delete(nodeId: String) = queries.deleteCanvasNode(nodeId)

    fun deleteAll(noteId: String) = queries.deleteCanvasNodesForNote(noteId)

    fun nodes(noteId: String): List<CanvasNode> =
        queries.selectCanvasNodes(noteId).executeAsList().map { it.toNode() }

    fun nodesIn(noteId: String, rect: CanvasRect): List<CanvasNode> =
        queries.selectCanvasNodesInRect(
            noteId = noteId,
            right = rect.right.toDouble(),
            left = rect.x.toDouble(),
            bottom = rect.bottom.toDouble(),
            top = rect.y.toDouble()
        ).executeAsList().map { it.toNode() }

    fun count(noteId: String): Long = queries.countCanvasNodes(noteId).executeAsOne()

    fun nodeForContent(contentId: String): CanvasNode? =
        queries.selectCanvasNodeForContent(contentId).executeAsOneOrNull()?.toNode()

    fun pruneOrphans() = queries.deleteOrphanCanvasNodes()

    fun move(nodeId: String, x: Float, y: Float) =
        queries.moveCanvasNode(x.toDouble(), y.toDouble(), getCurrentTimestamp().toString(), nodeId)

    fun resize(nodeId: String, width: Float, height: Float) =
        queries.resizeCanvasNode(
            width.toDouble(),
            height.toDouble(),
            getCurrentTimestamp().toString(),
            nodeId
        )

    fun raise(nodeId: String, z: Int) =
        queries.raiseCanvasNode(z.toLong(), getCurrentTimestamp().toString(), nodeId)

    fun rename(nodeId: String, name: String) =
        queries.renameCanvasNode(name, getCurrentTimestamp().toString(), nodeId)

    fun saveViewport(noteId: String, viewport: Viewport) {
        queries.upsertCanvasViewport(
            noteId = noteId,
            offsetX = viewport.offsetX.toDouble(),
            offsetY = viewport.offsetY.toDouble(),
            scale = viewport.scale.toDouble(),
            updatedAt = getCurrentTimestamp().toString()
        )
    }

    fun viewport(noteId: String): Viewport? =
        queries.selectCanvasViewport(noteId).executeAsOneOrNull()?.let {
            Viewport(
                offsetX = it.offsetX.toFloat(),
                offsetY = it.offsetY.toFloat(),
                scale = it.scale.toFloat()
            )
        }

    private fun com.app.pustakam.core.database.CanvasNodeEntity.toNode(): CanvasNode = CanvasNode(
        id = id,
        kind = runCatching { CanvasNodeKind.valueOf(kind) }
            .getOrDefault(CanvasNodeKind.MASTER_TEXT),
        name = name,
        rect = CanvasRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()),
        z = z.toInt(),
        contentId = contentId,
        parentId = parentId,
        locked = locked == 1L,
        hidden = hidden == 1L,
        links = if (links.isEmpty()) emptyList() else links.split(LINK_SEPARATOR)
    )

    companion object {
        private const val LINK_SEPARATOR = ","
    }
}
