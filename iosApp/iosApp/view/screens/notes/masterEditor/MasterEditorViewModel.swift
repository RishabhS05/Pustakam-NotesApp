import SwiftUI
import Combine
import shared

final class MasterEditorViewModel: ObservableObject {

    @Published private(set) var note: Note?
    @Published private(set) var canvas = CanvasEditorState()
    @Published private(set) var texts: [String: MasterTextState] = [:]
    @Published private(set) var isLoading = false
    @Published var keyboardDismissToken: Int = 0

    private let adapter: NotesBridgeAdapter
    private let canvasBridge: CanvasBridge
    private var noteId: String?

    init(
        adapter: NotesBridgeAdapter = NotesBridgeAdapter(),
        canvasBridge: CanvasBridge = CanvasBridge()
    ) {
        self.adapter = adapter
        self.canvasBridge = canvasBridge
    }

    func text(for nodeId: String) -> MasterTextState? { texts[nodeId] }

    func content(for node: CanvasNode) -> NoteContentModel? {
        guard let contentId = node.contentId else { return nil }
        return note?.contents.first { $0.id == contentId }
    }

    func load(noteId: String?) {
        guard let noteId else { return }
        self.noteId = noteId
        isLoading = true
        adapter.readNote(noteId: noteId) { [weak self] state in
            guard let self else { return }
            if let loaded = state.data { self.hydrate(loaded) }
        }
    }

    private func hydrate(_ loaded: Note) {
        note = loaded
        isLoading = false

        let textContents = loaded.contents.compactMap { $0 as? NoteContentModel.TextContent }
        canvasBridge.load(noteId: loaded.id) { [weak self] document, savedViewport in
            guard let self else { return }
            var nodes = document.nodes
            if nodes.isEmpty {
                nodes = self.buildInitialNodes(loaded, textContents)
                self.canvasBridge.saveAll(noteId: loaded.id, nodes: nodes)
            }

            var built: [String: MasterTextState] = [:]
            for node in nodes where node.kind == CanvasNodeKind.masterText {
                guard let contentId = node.contentId,
                      let content = textContents.first(where: { $0.id == contentId }) else { continue }
                built[node.id] = MasterTextState.companion.of(
                    document: RichTextCodec.shared.documentFrom(content: content)
                )
            }

            DispatchQueue.main.async {
                self.texts = built
                self.canvas = self.canvas.doCopy(
                    document: CanvasDocument(nodes: nodes),
                    viewport: savedViewport?.withSize(
                        width: self.canvas.viewport.widthPx,
                        height: self.canvas.viewport.heightPx
                    ) ?? self.canvas.viewport,
                    tool: self.canvas.tool,
                    selectedNodeId: self.canvas.selectedNodeId,
                    draggingNodeId: self.canvas.draggingNodeId,
                    editingNodeId: self.canvas.editingNodeId
                )
            }
        }
    }

    private func buildInitialNodes(
        _ loaded: Note,
        _ textContents: [NoteContentModel.TextContent]
    ) -> [CanvasNode] {
        let contents: [NoteContentModel.TextContent] = textContents.isEmpty
            ? [NoteContentObjectHelper.shared.createText(
                noteId: loaded.id, positionedAt: 0, text: "")]
            : textContents
        var y: Float = 0
        return contents.map { content in
            let node = CanvasNode.companion.masterText(
                contentId: content.id,
                x: 0,
                y: y,
                width: CanvasNode.companion.DEFAULT_TEXT_WIDTH,
                height: CanvasNode.companion.DEFAULT_TEXT_HEIGHT
            )
            y += CanvasNode.companion.DEFAULT_TEXT_HEIGHT + CanvasNode.companion.DEFAULT_GAP
            return node
        }
    }

    func onCanvasIntent(_ intent: CanvasEditorIntent) {
        let before = canvas
        canvas = CanvasEditorReducer.shared.reduce(state: before, intent: intent)
        persistCanvas(before: before, intent: intent)
    }

    private func persistCanvas(before: CanvasEditorState, intent: CanvasEditorIntent) {
        guard let noteId else { return }
        if intent is CanvasEditorIntentEndDrag {
            if let dragging = before.draggingNodeId,
               let node = canvas.document.nodeById(nodeId: dragging) {
                canvasBridge.move(nodeId: node.id, x: node.rect.x, y: node.rect.y)
            }
        } else if let add = intent as? CanvasEditorIntentAddNode {
            canvasBridge.save(noteId: noteId, node: add.node)
        } else if let remove = intent as? CanvasEditorIntentRemoveNode {
            canvasBridge.remove(nodeId: remove.nodeId)
        } else {
            canvasBridge.saveViewport(noteId: noteId, viewport: canvas.viewport)
        }
    }

    func onTextIntent(nodeId: String, intent: MasterTextIntent) {
        guard let current = texts[nodeId] else { return }
        let next = MasterTextReducer.shared.reduce(state: current, intent: intent)
        texts[nodeId] = next
        if next.document != current.document { persistText(nodeId: nodeId, state: next) }
    }

    private func persistText(nodeId: String, state: MasterTextState) {
        guard let note,
              let contentId = canvas.document.nodeById(nodeId: nodeId)?.contentId,
              let content = note.contents
                .compactMap({ $0 as? NoteContentModel.TextContent })
                .first(where: { $0.id == contentId }) else { return }

        let updated = RichTextCodec.shared.applyTo(content: content, document: state.document)
        let contents = note.contents.map { $0.id == updated.id ? updated : $0 }
        let nextNote = note.withContents(newContents: contents)
        self.note = nextNote
        adapter.createOrUpdateNote(note: nextNote) { _ in }
    }

    func addWidgetNearFocused(kind: CanvasNodeKind, content: NoteContentModel? = nil) {
        guard let note else { return }
        let anchor = CanvasCommands.shared.anchorOf(state: canvas)
        let node: CanvasNode
        if let anchor {
            node = CanvasNode.companion.nextTo(
                anchor: anchor,
                kind: kind,
                contentId: content?.id,
                width: kind == CanvasNodeKind.masterText
                    ? CanvasNode.companion.DEFAULT_TEXT_WIDTH
                    : CanvasNode.companion.DEFAULT_MEDIA_WIDTH,
                height: kind == CanvasNodeKind.masterText
                    ? CanvasNode.companion.DEFAULT_TEXT_HEIGHT
                    : CanvasNode.companion.DEFAULT_MEDIA_HEIGHT,
                gap: CanvasNode.companion.DEFAULT_GAP
            )
        } else {
            node = CanvasNode.companion.of(
                kind: kind,
                contentId: content?.id,
                x: canvas.document.bounds.right + CanvasNode.companion.DEFAULT_GAP,
                y: canvas.document.bounds.y,
                width: CanvasNode.companion.DEFAULT_MEDIA_WIDTH,
                height: CanvasNode.companion.DEFAULT_MEDIA_HEIGHT,
                parentId: nil
            )
        }

        if let content {
            let nextNote = note.withContents(newContents: note.contents + [content])
            self.note = nextNote
            adapter.createOrUpdateNote(note: nextNote) { _ in }
            if let text = content as? NoteContentModel.TextContent {
                texts[node.id] = MasterTextState.companion.of(
                    document: RichTextCodec.shared.documentFrom(content: text)
                )
            }
        }
        onCanvasIntent(CanvasCommands.shared.addNode(node: node))
        if let anchor {
            onCanvasIntent(CanvasCommands.shared.linkNodes(fromId: anchor.id, toId: node.id))
        }
    }

    func addTextNode() {
        guard let note else { return }
        let content = NoteContentObjectHelper.shared.createText(
            noteId: note.id,
            positionedAt: Double(note.contents.count),
            text: ""
        )
        addWidgetNearFocused(kind: CanvasNodeKind.masterText, content: content)
    }
}
