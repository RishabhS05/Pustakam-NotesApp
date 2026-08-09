package com.app.pustakam.android.screen.masterEditor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.fileimport.FileImportManager
import com.app.pustakam.android.fileimport.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.core.common.util.ContentType
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
import com.app.pustakam.feature.notes.domain.editor.CaptureKind
import com.app.pustakam.feature.notes.domain.editor.EditorCapabilityReducer
import com.app.pustakam.feature.notes.domain.editor.EditorCapabilityState
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
    val isLoading: Boolean = false,
    val capabilities: EditorCapabilityState = EditorCapabilityState(),
    val audioDraft: NoteContentModel.MediaContent? = null
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

    // 🔧 09-Aug-2026: canvas rows are derived layout — a read failure must degrade to an
    //   empty canvas, never take the editor down with it (was an uncaught SQLiteException)
    private suspend fun hydrate(note: Note) {
        val stored = runCatching { canvasRepository.load(note.id) }
            .getOrElse { com.app.pustakam.core.richtext.master.model.CanvasDocument() }
        val savedViewport = runCatching { canvasRepository.loadViewport(note.id) }.getOrNull()

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
                canvas = CanvasCommands.loaded(it.canvas, nodes, savedViewport),
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
        return CanvasCommands.stackedTextNodes(contents.map { it.id })
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
            runCatching {
                when (intent) {
                    // full upsert, not move(): a drop can also have changed the parent page
                    is CanvasEditorIntent.EndDrag -> {
                        before.draggingNodeId
                            ?.let { next.document.nodeById(it) }
                            ?.let { canvasRepository.save(id, it) }
                    }

                    is CanvasEditorIntent.ReparentNode ->
                        next.document.nodeById(intent.nodeId)
                            ?.let { canvasRepository.save(id, it) }

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
                    is CanvasEditorIntent.FocusNode ->
                        canvasRepository.saveViewport(id, next.viewport)

                    else -> Unit
                }
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
        val node = CanvasCommands.nodeFor(canvas, kind, content?.id)

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

    fun deleteContent(contentId: String) {
        val nodeId = _state.value.canvas.document.nodeForContent(contentId)?.id
        if (nodeId != null) deleteNode(nodeId) else removeContentOnly(contentId)
    }

    private fun removeContentOnly(contentId: String) {
        val note = _state.value.note ?: return
        val nextNote = note.withContents(note.contents.filterNot { it.id == contentId })
        _state.update { it.copy(note = nextNote) }
        viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
    }

    fun importDeviceFiles(context: Context, uris: List<Uri>) {
        val note = _state.value.note ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val items = FileImportManager.importUris(
                context.applicationContext, note.id, note.contents.count().toDouble(), uris
            )
            withContext(Dispatchers.Main) { items.forEach { addMediaNear(it) } }
        }
    }

    fun importFromLink(context: Context, url: String) {
        val note = _state.value.note ?: return
        if (url.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = FileImportManager.importFromUrl(
                context.applicationContext, note.id, note.contents.count().toDouble(), url
            )
            withContext(Dispatchers.Main) {
                if (result is ImportResult.Success) result.contents.forEach { addMediaNear(it) }
            }
        }
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

    fun onCapabilityState(next: EditorCapabilityState) {
        _state.update { it.copy(capabilities = next) }
    }

    fun requestCapture(kind: CaptureKind) {
        val note = _state.value.note ?: return
        val draft = if (kind == CaptureKind.AUDIO) {
            NoteContentObjectHelper.createMedia(
                contentType = ContentType.AUDIO,
                noteId = note.id,
                positionedAt = note.contents.size.toDouble()
            )
        } else {
            _state.value.audioDraft
        }
        _state.update {
            it.copy(
                audioDraft = draft,
                capabilities = EditorCapabilityReducer.requestCapture(it.capabilities, kind)
            )
        }
    }

    fun onCaptured(content: NoteContentModel?) {
        val media = content ?: return
        addMediaNear(media)
        _state.update {
            it.copy(
                audioDraft = null,
                capabilities = EditorCapabilityReducer.captureFinished(it.capabilities)
            )
        }
    }

    fun askDeleteContent(contentId: String) {
        _state.update {
            it.copy(
                capabilities = EditorCapabilityReducer.askDeleteContent(it.capabilities, contentId)
            )
        }
    }

    fun setAttachSheet(visible: Boolean) {
        _state.update {
            it.copy(capabilities = EditorCapabilityReducer.setAttachSheet(it.capabilities, visible))
        }
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
