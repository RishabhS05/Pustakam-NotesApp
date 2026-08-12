package com.app.pustakam.feature.notes.domain.bridge

import com.app.pustakam.core.common.coroutines.provideDispatcher
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.Viewport
import com.app.pustakam.feature.notes.domain.usecase.ClearCanvasUseCase
import com.app.pustakam.feature.notes.domain.usecase.MoveCanvasNodeUseCase
import com.app.pustakam.feature.notes.domain.usecase.ReadCanvasUseCase
import com.app.pustakam.feature.notes.domain.usecase.ReadCanvasViewportUseCase
import com.app.pustakam.feature.notes.domain.usecase.RemoveCanvasNodeUseCase
import com.app.pustakam.feature.notes.domain.usecase.RenameCanvasNodeUseCase
import com.app.pustakam.feature.notes.domain.usecase.ResizeCanvasNodeUseCase
import com.app.pustakam.feature.notes.domain.usecase.SaveCanvasNodeUseCase
import com.app.pustakam.feature.notes.domain.usecase.SaveCanvasNodesUseCase
import com.app.pustakam.feature.notes.domain.usecase.SaveCanvasViewportUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CanvasBridge : KoinComponent {

    private val readCanvas by inject<ReadCanvasUseCase>()
    private val readCanvasViewport by inject<ReadCanvasViewportUseCase>()
    private val saveCanvasNode by inject<SaveCanvasNodeUseCase>()
    private val saveCanvasNodes by inject<SaveCanvasNodesUseCase>()
    private val moveCanvasNode by inject<MoveCanvasNodeUseCase>()
    private val resizeCanvasNode by inject<ResizeCanvasNodeUseCase>()
    private val renameCanvasNode by inject<RenameCanvasNodeUseCase>()
    private val removeCanvasNode by inject<RemoveCanvasNodeUseCase>()
    private val clearCanvas by inject<ClearCanvasUseCase>()
    private val saveCanvasViewport by inject<SaveCanvasViewportUseCase>()
    private val scope = CoroutineScope(SupervisorJob() + provideDispatcher().io)
    fun load(
        noteId: String,
        onLoaded: (CanvasDocument, Viewport?) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val document = readCanvas(noteId)
                val viewport = readCanvasViewport(noteId)
                onLoaded(document, viewport)
            } catch (error: Throwable) {
                onError(error.message ?: "Could not read the canvas for this note.")
            }
        }
    }

    fun save(noteId: String, node: CanvasNode) {
        scope.launch { saveCanvasNode(noteId, node) }
    }

    fun saveAll(noteId: String, nodes: List<CanvasNode>) {
        scope.launch { saveCanvasNodes(noteId, nodes) }
    }

    fun move(nodeId: String, x: Float, y: Float) {
        scope.launch { moveCanvasNode(nodeId, x, y) }
    }

    fun resize(nodeId: String, width: Float, height: Float) {
        scope.launch { resizeCanvasNode(nodeId, width, height) }
    }

    fun rename(nodeId: String, name: String) {
        scope.launch { renameCanvasNode(nodeId, name) }
    }

    fun removeAll(noteId: String) {
        scope.launch { clearCanvas(noteId) }
    }

    fun remove(nodeId: String) {
        scope.launch { removeCanvasNode(nodeId) }
    }

    fun saveViewport(noteId: String, viewport: Viewport) {
        scope.launch { saveCanvasViewport(noteId, viewport) }
    }

    fun dispose() = scope.cancel()
}
