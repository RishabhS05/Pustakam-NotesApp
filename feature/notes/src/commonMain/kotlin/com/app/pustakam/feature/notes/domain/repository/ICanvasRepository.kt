package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport

interface ICanvasRepository {

    suspend fun load(noteId: String): CanvasDocument

    suspend fun loadVisible(noteId: String, rect: CanvasRect): CanvasDocument

    suspend fun save(noteId: String, node: CanvasNode)

    suspend fun saveAll(noteId: String, nodes: List<CanvasNode>)

    suspend fun move(nodeId: String, x: Float, y: Float)

    suspend fun resize(nodeId: String, width: Float, height: Float)

    suspend fun raise(nodeId: String, z: Int)

    suspend fun rename(nodeId: String, name: String)

    suspend fun remove(nodeId: String)

    suspend fun removeAll(noteId: String)

    suspend fun count(noteId: String): Int

    suspend fun nodeForContent(contentId: String): CanvasNode?

    suspend fun pruneOrphans()

    suspend fun loadViewport(noteId: String): Viewport?

    suspend fun saveViewport(noteId: String, viewport: Viewport)
}
