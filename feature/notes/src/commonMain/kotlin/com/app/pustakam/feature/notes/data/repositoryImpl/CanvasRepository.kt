package com.app.pustakam.feature.notes.data.repositoryImpl

import com.app.pustakam.core.common.coroutines.provideDispatcher
import com.app.pustakam.core.database.NotesDatabase
import com.app.pustakam.core.database.localdb.database.CanvasDao
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport
import com.app.pustakam.feature.notes.domain.repository.ICanvasRepository
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CanvasRepository : ICanvasRepository, KoinComponent {

    private val database by inject<NotesDatabase>()
    private val dao by lazy { CanvasDao(database) }

    private val dispatcher by lazy { provideDispatcher() }

    private suspend fun <T> onIo(block: () -> T): T =
        withContext(dispatcher.io) { block() }

    override suspend fun load(noteId: String): CanvasDocument =
        onIo { CanvasDocument(dao.nodes(noteId)) }

    override suspend fun loadVisible(noteId: String, rect: CanvasRect): CanvasDocument =
        onIo { CanvasDocument(dao.nodesIn(noteId, rect)) }

    override suspend fun save(noteId: String, node: CanvasNode) = onIo { dao.upsert(noteId, node) }

    override suspend fun saveAll(noteId: String, nodes: List<CanvasNode>) =
        onIo { dao.upsertAll(noteId, nodes) }

    override suspend fun move(nodeId: String, x: Float, y: Float) = onIo { dao.move(nodeId, x, y) }

    override suspend fun resize(nodeId: String, width: Float, height: Float) =
        onIo { dao.resize(nodeId, width, height) }

    override suspend fun raise(nodeId: String, z: Int) = onIo { dao.raise(nodeId, z) }

    override suspend fun rename(nodeId: String, name: String) = onIo { dao.rename(nodeId, name) }

    override suspend fun remove(nodeId: String) = onIo { dao.delete(nodeId) }

    override suspend fun removeAll(noteId: String) = onIo { dao.deleteAll(noteId) }

    override suspend fun count(noteId: String): Int = onIo { dao.count(noteId).toInt() }

    override suspend fun nodeForContent(contentId: String): CanvasNode? =
        onIo { dao.nodeForContent(contentId) }

    override suspend fun pruneOrphans() = onIo { dao.pruneOrphans() }

    override suspend fun loadViewport(noteId: String): Viewport? = onIo { dao.viewport(noteId) }

    override suspend fun saveViewport(noteId: String, viewport: Viewport) =
        onIo { dao.saveViewport(noteId, viewport) }
}
