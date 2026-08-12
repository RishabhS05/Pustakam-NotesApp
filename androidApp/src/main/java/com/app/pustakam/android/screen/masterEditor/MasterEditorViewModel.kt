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
import com.app.pustakam.core.richtext.master.presentation.CanvasCommands
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorIntent
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorReducer
import com.app.pustakam.core.richtext.master.presentation.CanvasEditorState
import com.app.pustakam.core.richtext.master.presentation.MasterTextIntent
import com.app.pustakam.core.richtext.master.presentation.MasterTextReducer
import com.app.pustakam.core.richtext.master.presentation.MasterTextState
import com.app.pustakam.core.richtext.master.presentation.NoteCanvasConverter
import com.app.pustakam.feature.notes.domain.editor.EditorCapabilityReducer
import com.app.pustakam.feature.notes.domain.editor.EditorCapabilityState
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
        val stored = readCanvas(note.id)
        val savedViewport = readCanvasViewport(note.id)

        val textContents = note.contents.filterIsInstance<NoteContentModel.TextContent>()
        val nodes = if (stored.nodes.isNotEmpty()) {
            stored.nodes
        } else {
            buildInitialNodes(note, textContents).also {
                saveCanvasNodes(note.id, it)
            }
        }

        val texts = nodes
            .filter { it.kind == ContentType.TEXT }
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
                            ?.let { saveCanvasNode(id, it) }
                    }

                    is CanvasEditorIntent.ReparentNode ->
                        next.document.nodeById(intent.nodeId)
                            ?.let { saveCanvasNode(id, it) }

                    is CanvasEditorIntent.ResizeNode ->
                        next.document.nodeById(intent.nodeId)
                            ?.let { resizeCanvasNode(it.id, it.rect.width, it.rect.height) }

                    is CanvasEditorIntent.AddNode -> saveCanvasNode(id, intent.node)

                    is CanvasEditorIntent.RemoveNode -> removeCanvasNode(intent.nodeId)

                    is CanvasEditorIntent.SelectAt,
                    is CanvasEditorIntent.SelectNode -> {
                        CanvasCommands.fittedPageId(next, intent)
                            ?.let { next.document.nodeById(it) }
                            ?.takeIf { it.rect != before.document.nodeById(it.id)?.rect }
                            ?.let { resizeCanvasNode(it.id, it.rect.width, it.rect.height) }
                        if (before.viewport != next.viewport) saveCanvasViewport(id, next.viewport)
                    }

                    is CanvasEditorIntent.Pan,
                    is CanvasEditorIntent.Zoom,
                    is CanvasEditorIntent.ZoomTo,
                    CanvasEditorIntent.ZoomIn,
                    CanvasEditorIntent.ZoomOut,
                    CanvasEditorIntent.ZoomToFit,
                    is CanvasEditorIntent.FocusNode ->
                        saveCanvasViewport(id, next.viewport)

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

    fun addPage() {
        val note = _state.value.note ?: return
        val content = NoteContentObjectHelper.createText(
            noteId = note.id,
            positionedAt = note.contents.size.toDouble()
        )
        val page = CanvasCommands.pageNode(_state.value.canvas, content.id)
        val nextNote = note.withContents(note.contents + content)
        _state.update {
            it.copy(
                note = nextNote,
                texts = it.texts + (page.id to MasterTextState.of(RichTextCodec.documentFrom(content)))
            )
        }
        onCanvasIntent(CanvasEditorIntent.AddNode(page))
        onCanvasIntent(CanvasCommands.selectNode(page.id))
        viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
    }

    fun addWidget(kind: ContentType, content: NoteContentModel? = null) {
        if (kind == ContentType.TEXT) {
            addPage()
            return
        }
        val note = _state.value.note ?: return
        val page = CanvasCommands.pageForSpawn(_state.value.canvas)
        if (page == null) {
            addPage()
            addWidget(kind, content)
            return
        }
        val node = CanvasCommands.widgetIn(_state.value.canvas, page, kind, content?.id)
        if (content != null) {
            val nextNote = note.withContents(note.contents + content)
            _state.update { it.copy(note = nextNote) }
            viewModelScope.launch { saveNoteUseCase(nextNote).collect { } }
        }
        onCanvasIntent(CanvasEditorIntent.AddNode(node))
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
        }
    }

    fun importFromLink(context: Context, url: String) {
        val note = _state.value.note ?: return
        if (url.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = FileImportManager.importFromUrl(
                context.applicationContext, note.id, note.contents.count().toDouble(), url
            )
        }
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
        viewModelScope.launch { renameCanvasNode(nodeId, name) }
    }

    fun rebuildLayoutFromNote() {
        val note = _state.value.note ?: return
        val document = NoteCanvasConverter.toCanvas(note.contents)
        onCanvasIntent(CanvasCommands.replaceDocument(document))
        _state.update { current ->
            val texts = document.nodes
                .filter { it.kind == ContentType.TEXT }
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
            clearCanvas(note.id)
            saveCanvasNodes(note.id, document.nodes)
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

    fun requestCapture(type: ContentType) {
        val note = _state.value.note ?: return
        val draft = if (type == ContentType.AUDIO) {
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
                capabilities = EditorCapabilityReducer.requestCapture(it.capabilities, type)
            )
        }
    }

    fun onCaptured(content: NoteContentModel?) {
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

}
