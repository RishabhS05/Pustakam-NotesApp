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

    // 🔧 09-Aug-2026: a throw in here used to be swallowed by scope.launch, so a failed read
    //   left the editor on an empty canvas with no error anywhere. Failures now report back.
    fun load(
        noteId: String,
        onLoaded: (CanvasDocument, Viewport?) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val document = repository.load(noteId)
                val viewport = repository.loadViewport(noteId)
                onLoaded(document, viewport)
            } catch (error: Throwable) {
                onError(error.message ?: "Could not read the canvas for this note.")
            }
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

    fun rename(nodeId: String, name: String) {
        scope.launch { repository.rename(nodeId, name) }
    }

    fun removeAll(noteId: String) {
        scope.launch { repository.removeAll(noteId) }
    }

    fun remove(nodeId: String) {
        scope.launch { repository.remove(nodeId) }
    }

    fun saveViewport(noteId: String, viewport: Viewport) {
        scope.launch { repository.saveViewport(noteId, viewport) }
    }

    fun dispose() = scope.cancel()
}
