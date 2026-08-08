package com.app.pustakam.android.screen.masterEditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.core.richtext.codec.RichTextCodec
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind
import com.app.pustakam.core.richtext.master.presentation.CanvasCommands
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorIntent
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorReducer
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorState
import com.app.pustakam.core.richtext.master.presentation.MasterTextIntent
import com.app.pustakam.core.richtext.master.presentation.MasterTextReducer
import com.app.pustakam.core.richtext.master.presentation.MasterTextState
import com.app.pustakam.core.richtext.master.presentation.NoteCanvasConverter
import com.app.pustakam.feature.notes.domain.repository.ICanvasRepository
import com.app.pustakam.feature.notes.domain.usecase.CreateORUpdateNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.ReadNoteUseCase
import com.app.pustakam.core.common.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class MasterEditorUiState(
    val note: Note? = null,
    val canvas: CanvasEditorState = CanvasEditorState(),
    val texts: Map<String, MasterTextState> = emptyMap(),
    val isLoading: Boolean = false
) {
    fun textFor(nodeId: String): MasterTextState? = texts[nodeId]
}

class MasterEditorViewModel : ViewModel(), KoinComponent {

    private val canvasRepository by inject<ICanvasRepository>()
    private val readNoteUseCase by inject<ReadNoteUseCase>()
    private val saveNoteUseCase by inject<CreateORUpdateNoteUseCase>()

    private val _state = MutableStateFlow(MasterEditorUiState())
    val state: StateFlow<MasterEditorUiState> = _state.asStateFlow()

    private var noteId: String? = null

    fun load(id: String?) {
        noteId = id
        if (id == null) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            readNoteUseCase(id).collect { result ->
                if (result is Result.Success) {
                    val note = result.data.data as? Note ?: return@collect
                    hydrate(note)
                }
            }
        }
    }

    private suspend fun hydrate(note: Note) {
        val stored = canvasRepository.load(note.id)
        val savedViewport = canvasRepository.loadViewport(note.id)

        val textContents = note.contents.filterIsInstance<NoteContentModel.TextContent>()
        val nodes = if (stored.nodes.isNotEmpty()) {
            stored.nodes
        } else {
            buildInitialNodes(note, textContents).also {
                canvasRepository.saveAll(note.id, it)
            }
        }

        val texts = nodes
            .filter { it.kind == CanvasNodeKind.MASTER_TEXT }
            .mapNotNull { node ->
                val content = textContents.firstOrNull { it.id == node.contentId }
                    ?: return@mapNotNull null
                node.id to MasterTextState.of(RichTextCodec.documentFrom(content))
            }
            .toMap()

        _state.update {
            it.copy(
                note = note,
                isLoading = false,
                canvas = it.canvas.copy(
                    document = com.app.pustakam.core.richtext.master.model.CanvasDocument(nodes),
                    viewport = savedViewport?.withSize(
                        it.canvas.viewport.widthPx,
                        it.canvas.viewport.heightPx
                    ) ?: it.canvas.viewport
                ),
                texts = texts
            )
        }
    }

    private fun buildInitialNodes(
        note: Note,
        textContents: List<NoteContentModel.TextContent>
    ): List<CanvasNode> {
        val contents = textContents.ifEmpty {
            listOf(NoteContentObjectHelper.createText(noteId = note.id, positionedAt = 0.0))
        }
        var y = 0f
        return contents.mapIndexed { index, content ->
            val node = CanvasNode.masterText(contentId = content.id, x = 0f, y = y)
            y += CanvasNode.DEFAULT_TEXT_HEIGHT + 48f
            node.copy(z = index)
        }
    }

    fun onCanvasIntent(intent: CanvasEditorIntent) {
        val before = _state.value.canvas
        val next = CanvasEditorReducer.reduce(before, intent)
        _state.update { it.copy(canvas = next) }
        persistCanvasChange(before, next, intent)
    }

    private fun persistCanvasChange(
        before: CanvasEditorState,
        next: CanvasEditorState,
        intent: CanvasEditorIntent
    ) {
        val id = noteId ?: return
        viewModelScope.launch {
            when (intent) {
                is CanvasEditorIntent.EndDrag -> {
                    before.draggingNodeId
                        ?.let { next.document.nodeById(it) }
                        ?.let { canvasRepository.move(it.id, it.rect.x, it.rect.y) }
                }

                is CanvasEditorIntent.ResizeNode ->
                    next.document.nodeById(intent.nodeId)
                        ?.let { canvasRepository.resize(it.id, it.rect.width, it.rect.height) }

                is CanvasEditorIntent.AddNode -> canvasRepository.save(id, intent.node)

                is CanvasEditorIntent.RemoveNode -> canvasRepository.remove(intent.nodeId)

                is CanvasEditorIntent.Pan,
                is CanvasEditorIntent.Zoom,
                is CanvasEditorIntent.ZoomTo,
                CanvasEditorIntent.ZoomIn,
                CanvasEditorIntent.ZoomOut,
                CanvasEditorIntent.ZoomToFit,
                is CanvasEditorIntent.FocusNode -> canvasRepository.saveViewport(id, next.viewport)

                else -> Unit
            }
        }
    }

    fun onTextIntent(nodeId: String, intent: MasterTextIntent) {
        val current = _state.value.texts[nodeId] ?: return
        val next = MasterTextReducer.reduce(current, intent)
        _state.update { it.copy(texts = it.texts + (nodeId to next)) }
        if (next.document != current.document) persistText(nodeId, next)
    }

    private fun persistText(nodeId: String, textState: MasterTextState) {
        val note = _state.value.note ?: return
        val contentId = _state.value.canvas.document.nodeById(nodeId)?.contentId ?: return
        val content = note.contents
            .filterIsInstance<NoteContentModel.TextContent>()
            .firstOrNull { it.id == contentId } ?: return

        val updated = RichTextCodec.applyTo(content, textState.document)
        val contents = note.contents.map { if (it.id == updated.id) updated else it }
        val nextNote = note.withContents(contents)
        _state.update { it.copy(note = nextNote) }
        viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
    }

    fun addWidgetNearFocused(kind: CanvasNodeKind, content: NoteContentModel? = null) {
        val note = _state.value.note ?: return
        val canvas = _state.value.canvas
        val anchor = CanvasCommands.anchorOf(canvas)
        val node = if (anchor == null) {
            CanvasNode.of(
                kind = kind,
                contentId = content?.id,
                x = canvas.document.bounds.right + CanvasNode.DEFAULT_GAP,
                y = canvas.document.bounds.y,
                width = CanvasNode.DEFAULT_MEDIA_WIDTH,
                height = CanvasNode.DEFAULT_MEDIA_HEIGHT
            )
        } else {
            CanvasNode.nextTo(
                anchor = anchor,
                kind = kind,
                contentId = content?.id,
                width = if (kind == CanvasNodeKind.MASTER_TEXT) CanvasNode.DEFAULT_TEXT_WIDTH
                else CanvasNode.DEFAULT_MEDIA_WIDTH,
                height = if (kind == CanvasNodeKind.MASTER_TEXT) CanvasNode.DEFAULT_TEXT_HEIGHT
                else CanvasNode.DEFAULT_MEDIA_HEIGHT
            )
        }

        if (content != null) {
            val nextNote = note.withContents(note.contents + content)
            _state.update { it.copy(note = nextNote) }
            viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
        }
        if (kind == CanvasNodeKind.MASTER_TEXT && content is NoteContentModel.TextContent) {
            _state.update {
                it.copy(
                    texts = it.texts + (node.id to MasterTextState.of(RichTextCodec.documentFrom(content)))
                )
            }
        }
        onCanvasIntent(CanvasEditorIntent.AddNode(node))
        anchor?.let { onCanvasIntent(CanvasCommands.linkNodes(it.id, node.id)) }
    }

    fun addMediaNear(content: NoteContentModel) {
        val kind = when (content.type) {
            com.app.pustakam.core.common.util.ContentType.LINK -> CanvasNodeKind.LINK
            com.app.pustakam.core.common.util.ContentType.LOCATION -> CanvasNodeKind.LOCATION
            com.app.pustakam.core.common.util.ContentType.PDF,
            com.app.pustakam.core.common.util.ContentType.DOCX -> CanvasNodeKind.DOCUMENT
            else -> CanvasNodeKind.MEDIA
        }
        addWidgetNearFocused(kind, content)
    }

    fun linkedNodes(nodeId: String) = CanvasCommands.linkedNodes(_state.value.canvas, nodeId)

    fun deleteNode(nodeId: String) {
        val note = _state.value.note ?: return
        val contentId = _state.value.canvas.document.nodeById(nodeId)?.contentId
        onCanvasIntent(CanvasCommands.removeNode(nodeId))
        _state.update { it.copy(texts = it.texts - nodeId) }
        if (contentId != null) {
            val nextNote = note.withContents(note.contents.filterNot { it.id == contentId })
            _state.update { it.copy(note = nextNote) }
            viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
        }
    }

    fun renameNode(nodeId: String, name: String) {
        onCanvasIntent(CanvasCommands.renameNode(nodeId, name))
        viewModelScope.launch { canvasRepository.rename(nodeId, name) }
    }

    fun rebuildLayoutFromNote() {
        val note = _state.value.note ?: return
        val document = NoteCanvasConverter.toCanvas(note.contents)
        onCanvasIntent(CanvasCommands.replaceDocument(document))
        _state.update { current ->
            val texts = document.nodes
                .filter { it.kind == CanvasNodeKind.MASTER_TEXT }
                .mapNotNull { node ->
                    val content = note.contents
                        .filterIsInstance<NoteContentModel.TextContent>()
                        .firstOrNull { it.id == node.contentId } ?: return@mapNotNull null
                    node.id to MasterTextState.of(RichTextCodec.documentFrom(content))
                }
                .toMap()
            current.copy(texts = texts)
        }
        viewModelScope.launch {
            canvasRepository.removeAll(note.id)
            canvasRepository.saveAll(note.id, document.nodes)
        }
    }

    fun applyCanvasOrderToNote() {
        val note = _state.value.note ?: return
        val reordered = NoteCanvasConverter.reorderContents(
            note.contents,
            _state.value.canvas.document
        )
        val nextNote = note.withContents(reordered)
        _state.update { it.copy(note = nextNote) }
        viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
    }

    fun addTextNode() {
        val note = _state.value.note ?: return
        val content = NoteContentObjectHelper.createText(
            noteId = note.id,
            positionedAt = note.contents.size.toDouble()
        )
        val bounds = _state.value.canvas.document.bounds
        val node = CanvasNode.masterText(
            contentId = content.id,
            x = bounds.right + 48f,
            y = bounds.y
        )
        val nextNote = note.withContents(note.contents + content)
        _state.update {
            it.copy(
                note = nextNote,
                texts = it.texts + (node.id to MasterTextState.of(RichTextCodec.documentFrom(content)))
            )
        }
        onCanvasIntent(CanvasEditorIntent.AddNode(node))
        viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
    }
}
