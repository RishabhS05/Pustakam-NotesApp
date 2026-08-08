package com.app.pustakam.feature.notes.domain.bridge

import com.app.pustakam.core.common.coroutines.provideDispatcher
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.Viewport
import com.app.pustakam.feature.notes.domain.repository.ICanvasRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CanvasBridge : KoinComponent {

    private val repository by inject<ICanvasRepository>()
    private val scope = CoroutineScope(SupervisorJob() + provideDispatcher().io)

    fun load(noteId: String, onLoaded: (CanvasDocument, Viewport?) -> Unit) {
        scope.launch {
            val document = repository.load(noteId)
            val viewport = repository.loadViewport(noteId)
            onLoaded(document, viewport)
        }
    }

    fun save(noteId: String, node: CanvasNode) {
        scope.launch { repository.save(noteId, node) }
    }

    fun saveAll(noteId: String, nodes: List<CanvasNode>) {
        scope.launch { repository.saveAll(noteId, nodes) }
    }

    fun move(nodeId: String, x: Float, y: Float) {
        scope.launch { repository.move(nodeId, x, y) }
    }

    fun resize(nodeId: String, width: Float, height: Float) {
        scope.launch { repository.resize(nodeId, width, height) }
    }

    fun remove(nodeId: String) {
        scope.launch { repository.remove(nodeId) }
    }

    fun saveViewport(noteId: String, viewport: Viewport) {
        scope.launch { repository.saveViewport(noteId, viewport) }
    }

    fun dispose() = scope.cancel()
}
