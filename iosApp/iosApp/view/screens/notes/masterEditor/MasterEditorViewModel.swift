import shared
import Combine
import SwiftUI

final class MasterEditorViewModel: ObservableObject {

    @Published private(set) var note: Note?
    @Published private(set) var noteContents: [NoteContentModel] = []
    @Published private(set) var canvas = CanvasCommands.shared.initialState()
    @Published private(set) var texts: [String: MasterTextState] = [:]
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var capabilities = EditorCapabilityCommands.shared.empty()
    @Published var keyboardDismissToken: Int = 0

    private let adapter: NotesBridgeAdapter
    private let canvasBridge: CanvasBridgeAdapter
    private var dirtyContentIds = Set<String>()
    private var noteId: String?
    private var hydratedNoteId: String?

    private var commands: CanvasCommands { CanvasCommands.shared }

    private var textContents: [NoteContentModel.TextContent] {
        noteContents.compactMap { $0 as? NoteContentModel.TextContent }
    }

    init(
        adapter: NotesBridgeAdapter = NotesBridgeAdapter(),
        canvasBridge: CanvasBridgeAdapter = CanvasBridgeAdapter()
    ) {
        self.adapter = adapter
        self.canvasBridge = canvasBridge
    }

    func text(for nodeId: String) -> MasterTextState? { texts[nodeId] }

    func content(for node: CanvasNode) -> NoteContentModel? {
        guard let contentId = node.contentId else { return nil }
        return noteContents.first { $0.id == contentId }
    }

    // MARK: - Load

    /// A nil id is passed straight through, the same as the note editor does: the repository
    /// answers with a fresh empty note rather than nothing at all.
    func load(noteId: String?) {
        guard self.noteId != noteId || note == nil, dirtyContentIds.isEmpty else { return }
        self.noteId = noteId
        adapter.readNote(noteId: noteId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .loading:
                self.isLoading = true
            case .success(let note):
                self.isLoading = false
                if let note { self.apply(note: note) }
            case .failure(let error):
                self.isLoading = false
                self.errorMessage = error.message
            case .idle:
                break
            }
        }
    }

    private func apply(note: Note) {
        self.note = note
        if dirtyContentIds.isEmpty {
            noteContents = note.contents
        }
        guard hydratedNoteId != note.id else {
            refreshMissingTexts()
            return
        }
        hydratedNoteId = note.id
        hydrateCanvas(noteId: note.id)
    }

    private func hydrateCanvas(noteId: String) {
        let contents = textContents
        canvasBridge.readCanvas(noteId: noteId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .loading:
                self.isLoading = true
            case .success(let document):
                self.isLoading = false
                var nodes: [CanvasNode] = document?.nodes ?? []
                if nodes.isEmpty {
                    nodes = self.seedNodes(noteId: noteId, contents: contents)
                    self.canvasBridge.saveAll(noteId: noteId, nodes: nodes)
                }
                self.readViewport(noteId: noteId, nodes: nodes)
            case .failure(let error):
                self.isLoading = false
                self.errorMessage = error.message
                self.hydratedNoteId = nil
                self.render(nodes: self.seedNodes(noteId: noteId, contents: contents), viewport: nil)
            case .idle:
                break
            }
        }
    }

    private func readViewport(noteId: String, nodes: [CanvasNode]) {
        canvasBridge.readViewport(noteId: noteId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let viewport):
                self.render(nodes: nodes, viewport: viewport)
            case .failure:
                self.render(nodes: nodes, viewport: nil)
            case .loading, .idle:
                break
            }
        }
    }

    private func render(nodes: [CanvasNode], viewport: Viewport?) {
        texts = buildTexts(nodes: nodes, contents: textContents, keeping: texts)
        canvas = commands.loaded(state: canvas, nodes: nodes, viewport: viewport)
    }

    private func refreshMissingTexts() {
        let existing = texts
        let built = buildTexts(nodes: canvas.document.nodes, contents: textContents, keeping: existing)
        if Set(built.keys) != Set(existing.keys) { texts = built }
    }

    private func seedNodes(
        noteId: String,
        contents: [NoteContentModel.TextContent]
    ) -> [CanvasNode] {
        if contents.isEmpty {
            let seed = NoteContentObjectHelper.shared.createText(
                noteId: noteId,
                positionedAt: 0,
                text: ""
            )
            addContent(seed)
            return commands.stackedTextNodes(contentIds: [seed.id])
        }
        return commands.stackedTextNodes(contentIds: contents.map { $0.id })
    }

    private func buildTexts(
        nodes: [CanvasNode],
        contents: [NoteContentModel.TextContent],
        keeping existing: [String: MasterTextState] = [:]
    ) -> [String: MasterTextState] {
        var built: [String: MasterTextState] = [:]
        for node in nodes where node.isText {
            guard let contentId = node.contentId else { continue }
            if let live = existing[node.id] {
                built[node.id] = live
                continue
            }
            guard let content = contents.first(where: { $0.id == contentId }) else { continue }
            built[node.id] = MasterTextState.companion.of(
                document: RichTextCodec.shared.documentFrom(content: content)
            )
        }
        return built
    }

    // MARK: - Content editing

    private func addContent(_ content: NoteContentModel) {
        dirtyContentIds.insert(content.id)
        noteContents.append(content)
    }

    private func updateContent(_ content: NoteContentModel) {
        dirtyContentIds.insert(content.id)
        if let index = noteContents.firstIndex(where: { $0.id == content.id }) {
            noteContents[index] = content
        } else {
            noteContents.append(content)
        }
    }

    private func removeContent(id: String) {
        dirtyContentIds.insert(id)
        noteContents.removeAll { $0.id == id }
    }

    func saveNote() {
        guard let note else { return }
        let toSave = note.withContents(newContents: noteContents)
        let dirtySnapshot = dirtyContentIds
        adapter.createOrUpdateNote(note: toSave, dirtyContentIds: dirtySnapshot) { [weak self] result in
            switch result {
            case .success:
                self?.dirtyContentIds.subtract(dirtySnapshot)
            case .failure(let error):
                self?.errorMessage = error.message
                print("MasterEditor saveNote failed [\(error.code)] \(error.message)")
            case .loading, .idle:
                break
            }
        }
    }

    // MARK: - Canvas

    func onCanvasIntent(_ intent: CanvasEditorIntent) {
        let before = canvas
        canvas = CanvasEditorReducer.shared.reduce(state: before, intent: intent)
        persistCanvas(before: before, intent: intent)
    }

    private func persistCanvas(before: CanvasEditorState, intent: CanvasEditorIntent) {
        guard let noteId else { return }
        if commands.isEndDrag(intent: intent) {
            guard let dragging = before.draggingNodeId,
                  let node = canvas.document.nodeById(nodeId: dragging) else { return }
            // full upsert, not move(): a drop can also have changed the parent page
            canvasBridge.save(noteId: noteId, node: node)
            for child in canvas.document.descendantsOf(nodeId: node.id) {
                canvasBridge.save(noteId: noteId, node: child)
            }
        } else if let reparentedId = commands.reparentedNodeId(intent: intent) {
            guard let node = canvas.document.nodeById(nodeId: reparentedId) else { return }
            canvasBridge.save(noteId: noteId, node: node)
        } else if let added = commands.addedNode(intent: intent) {
            canvasBridge.save(noteId: noteId, node: added)
        } else if let removedId = commands.removedNodeId(intent: intent) {
            canvasBridge.remove(nodeId: removedId)
        } else if let resizedId = commands.resizedNodeId(intent: intent) {
            guard let node = canvas.document.nodeById(nodeId: resizedId) else { return }
            canvasBridge.resize(nodeId: node.id, width: node.rect.width, height: node.rect.height)
        } else if let fittedId = commands.fittedPageId(state: canvas, intent: intent) {
            if let page = canvas.document.nodeById(nodeId: fittedId),
               page.rect != before.document.nodeById(nodeId: fittedId)?.rect {
                canvasBridge.resize(
                    nodeId: page.id,
                    width: page.rect.width,
                    height: page.rect.height
                )
            }
            if canvas.viewport != before.viewport {
                canvasBridge.saveViewport(noteId: noteId, viewport: canvas.viewport)
            }
        } else if commands.affectsViewport(intent: intent) {
            canvasBridge.saveViewport(noteId: noteId, viewport: canvas.viewport)
        }
    }

    // MARK: - Text

    func onTextIntent(nodeId: String, intent: MasterTextIntent) {
        guard let current = texts[nodeId] else { return }
        let next = MasterTextReducer.shared.reduce(state: current, intent: intent)
        texts[nodeId] = next
        if next.document != current.document { persistText(nodeId: nodeId, state: next) }
    }

    private func persistText(nodeId: String, state: MasterTextState) {
        guard let contentId = canvas.document.nodeById(nodeId: nodeId)?.contentId,
              let content = textContents.first(where: { $0.id == contentId }) else { return }
        updateContent(RichTextCodec.shared.applyTo(content: content, document: state.document))
        saveNote()
    }

    // MARK: - Nodes

    func addPage() {
        guard let note else { return }
        let content = NoteContentObjectHelper.shared.createText(
            noteId: note.id,
            positionedAt: Double(noteContents.count),
            text: ""
        )
        let page = commands.pageNode(state: canvas, contentId: content.id)
        addContent(content)
        texts[page.id] = MasterTextState.companion.of(
            document: RichTextCodec.shared.documentFrom(content: content)
        )
        onCanvasIntent(commands.addNode(node: page))
        onCanvasIntent(commands.selectNode(nodeId: page.id))
        saveNote()
    }

    func addWidget(kind : ContentType , content: NoteContentModel? = nil) {
        guard note != nil, content != nil else { return }
        
        if kind == ContentType.text {
            addPage()
            return
        }
        guard let page = commands.pageForSpawn(state: canvas) else {
            addPage()
            addWidget(kind:  kind, content: content)
            return
        }
        let node = commands.widgetIn(
            state: canvas,
            page: page,
            kind: content!.type,
            contentId: content?.id
        )
        if let content {
            addContent(content)
            saveNote()
        }
        onCanvasIntent(commands.addNode(node: node))
    }

    func deleteNode(nodeId: String) {
        let contentId = canvas.document.nodeById(nodeId: nodeId)?.contentId
        onCanvasIntent(commands.removeNode(nodeId: nodeId))
        texts[nodeId] = nil
        guard let contentId else { return }
        removeContent(id: contentId)
        saveNote()
    }

    func onCapabilityState(_ next: EditorCapabilityState) {
        capabilities = next
    }

    func requestCapture(_ type: ContentType) {
        capabilities = EditorCapabilityReducer.shared.requestCapture(
            state: capabilities,
            type: type
        )
    }

    func openImportSheet() {
        capabilities = EditorCapabilityReducer.shared.setImportSheet(
            state: capabilities,
            visible: true
        )
    }

    func onCaptured(_ media: CapturedMedia?) {
        guard let note else { return }
        let content = EditorCapture.persist(
            media: media,
            noteId: note.id,
            positionedAt: Double(noteContents.count)
        )
        if let content {
            addWidget(
                kind: content.type,
                content: content
            )
        }
        capabilities = EditorCapabilityReducer.shared.captureFinished(state: capabilities)
    }

    func importFiles(urls: [URL]) {
        guard let note else { return }
        let items = FileImportService.importPicked(
            urls: urls,
            noteId: note.id,
            startPosition: Double(noteContents.count)
        )
        for item in items {
            addWidget(kind: item.type, content: item)
        }
    }

    func importFromLink(_ url: String) {
        guard let note else { return }
        FileImportService.importFromLink(
            url,
            noteId: note.id,
            startPosition: Double(noteContents.count)
        ) { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let items):
                for item in items {
                    self.addWidget(
                        kind: item.type,
                        content: item
                    )
                }
            case .noFileFound:
                self.errorMessage = "No file found at this link."
            case .failed(let message):
                self.errorMessage = message
            }
        }
    }

    func askDeleteContent(_ contentId: String) {
        capabilities = EditorCapabilityReducer.shared.askDeleteContent(
            state: capabilities,
            contentId: contentId
        )
    }

    func deleteContent(_ contentId: String) {
        guard let nodeId = canvas.document.nodeForContent(contentId: contentId)?.id else {
            removeContent(id: contentId)
            saveNote()
            return
        }
        deleteNode(nodeId: nodeId)
    }

    func renameNode(nodeId: String, name: String) {
        onCanvasIntent(commands.renameNode(nodeId: nodeId, name: name))
        canvasBridge.rename(nodeId: nodeId, name: name)
    }

    // MARK: - Conversion

    func rebuildLayoutFromNote() {
        guard let noteId else { return }
        let document = NoteCanvasConverter.shared.toCanvas(contents: noteContents)
        onCanvasIntent(commands.replaceDocument(document: document))
        texts = buildTexts(nodes: document.nodes, contents: textContents)
        canvasBridge.removeAll(noteId: noteId)
        canvasBridge.saveAll(noteId: noteId, nodes: document.nodes)
    }

    func applyCanvasOrderToNote() {
        let reordered = NoteCanvasConverter.shared.reorderContents(
            contents: noteContents,
            document: canvas.document
        )
        noteContents = reordered
        noteContents.forEach { dirtyContentIds.insert($0.id) }
        saveNote()
    }
}
