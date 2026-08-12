package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.data.usecases.BaseUseCase
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport
import com.app.pustakam.feature.notes.domain.repository.ICanvasRepository
import org.koin.core.component.inject

abstract class CanvasBaseUseCase : BaseUseCase() {
    protected val canvasRepository: ICanvasRepository by inject()
}

class ReadCanvasUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String): CanvasDocument =
        runCatching { canvasRepository.load(noteId) }.getOrElse { CanvasDocument() }
}

class ReadVisibleCanvasUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String, rect: CanvasRect): CanvasDocument =
        runCatching { canvasRepository.loadVisible(noteId, rect) }.getOrElse { CanvasDocument() }
}

class ReadCanvasViewportUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String): Viewport? =
        runCatching { canvasRepository.loadViewport(noteId) }.getOrNull()
}

class SaveCanvasNodeUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String, node: CanvasNode) {
        runCatching { canvasRepository.save(noteId, node) }
    }
}

class SaveCanvasNodesUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String, nodes: List<CanvasNode>) {
        runCatching { canvasRepository.saveAll(noteId, nodes) }
    }
}

class MoveCanvasNodeUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(nodeId: String, x: Float, y: Float) {
        runCatching { canvasRepository.move(nodeId, x, y) }
    }
}

class ResizeCanvasNodeUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(nodeId: String, width: Float, height: Float) {
        runCatching { canvasRepository.resize(nodeId, width, height) }
    }
}

class RenameCanvasNodeUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(nodeId: String, name: String) {
        runCatching { canvasRepository.rename(nodeId, name) }
    }
}

class RemoveCanvasNodeUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(nodeId: String) {
        runCatching { canvasRepository.remove(nodeId) }
    }
}

class ClearCanvasUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String) {
        runCatching { canvasRepository.removeAll(noteId) }
    }
}

class SaveCanvasViewportUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke(noteId: String, viewport: Viewport) {
        runCatching { canvasRepository.saveViewport(noteId, viewport) }
    }
}

class PruneCanvasOrphansUseCase : CanvasBaseUseCase() {
    suspend operator fun invoke() {
        runCatching { canvasRepository.pruneOrphans() }
    }
}
