import shared
import Combine
import SwiftUI

/// Single UI-state surface for the editor (mirror of the list slice pattern).
struct NoteEditorUIState {
    var note: Note? = nil                       // fixes E2: now inside @Published state
    var title: String = ""                      // fixes V2: title owned by VM, saved reliably
    var noteContents: [NoteContentModel] = []   // SINGLE source of truth (fixes E4)
    var isLoading = false
    var isDeleted = false                       // fixes V3: save() refuses after delete
    var errorMessage: String? = nil

    /// E3 guard: content actions allowed only once the note exists.
    var isNoteReady: Bool { note != nil }
}

class NoteEditorViewModel: ObservableObject {

    @Published var state = NoteEditorUIState()

    private let adapter: NotesBridgeAdapter
    private let contentBridge: NoteContentBridge

    /// DI per series convention: defaults keep call sites/tests simple. (fixes E1, E6)
    init(note: Note? = nil,
         adapter: NotesBridgeAdapter = NotesBridgeAdapter(),
         contentBridge: NoteContentBridge = NoteContentBridge()) {
        self.adapter = adapter
        self.contentBridge = contentBridge
        load(note: note)
    }

    deinit { contentBridge.dispose() }          // adapter cleans itself up

    // MARK: - Load

    private func load(note: Note?) {
        if let note {
            apply(note: note)
        } else {
            // New note: bridge returns a fresh empty Note (pre-generated id/timestamps).
            adapter.readNote(noteId: nil) { [weak self] result in
                guard let self else { return }
                switch result {
                case .loading:            self.state.isLoading = true
                case .success(let note):  self.state.isLoading = false
                                          if let note { self.apply(note: note) }
                case .failure(let error): self.state.isLoading = false
                                          self.state.errorMessage = error.message
                case .idle: break
                }
            }
        }
    }

    private func apply(note: Note) {
        state.note = note
        state.title = note.title ?? ""
        state.noteContents = note.contents as? [NoteContentModel] ?? []
        contentBridge.setSelectedNote(note: note)
    }

    // MARK: - Content editing (single list; note.contents materialized only at save)

    func addContent(content: NoteContentModel) {
        guard state.isNoteReady else { return }                 // fixes E3 crash window
        state.noteContents.append(content)
        if content.isPlayingMedia(), let media = content as? NoteContentModel.MediaContent {
            contentBridge.updateMediaContent(content: media)    // safe cast (was as!)
        }
    }

    func updateContent(content: NoteContentModel) {
        if let index = state.noteContents.firstIndex(where: { $0.id == content.id }) {
            state.noteContents[index] = content
        } else {
            addContent(content: content)
        }
        // NOTE: no more parallel note.contents bookkeeping (E4) — save() materializes.
    }

    func addNewText() {
        guard let noteId = state.note?.id else { return }       // fixes E3 (was note!.id)
        let text = NoteContentObjectHelper.shared.createText(
            noteId: noteId,
            positionedAt: Double(state.noteContents.count),     // 🔧 C4: position is Double now
            text: "")
        addContent(content: text)
    }

    func getCapturedData(media: CapturedMedia?) {
        guard let noteId = state.note?.id else { return }       // fixes E3 (was note!.id)
        let timeStamp = DateTimeUtilsKt.getCurrentTimestamp()

        switch media {
        case .image(let image):
            let type = ContentType.image
            guard let data = image.pngData() else { return }
            let path = saveImageFile(data: data,
                                     in: "\(type.name)/\(noteId)",
                                     to: "\(timeStamp)\(type.getExt())")
            saveMedia(type: type, localPath: path, noteId: noteId, timestamp: "\(timeStamp)")

        case .video(let path):
            let type = ContentType.video
            guard let saved = copyFile(to: "\(type.name)/\(noteId)",
                                       fileName: "\(timeStamp)\(type.getExt())",
                                       from: path) else { return }
            saveMedia(type: type, localPath: saved.path, noteId: noteId, timestamp: "\(timeStamp)")

        case .audio(let path):
            let type = ContentType.audio
            guard let saved = copyFile(to: "\(type.name)/\(noteId)",
                                       fileName: "\(timeStamp)\(type.getExt())",
                                       from: path) else { return }
            saveMedia(type: type, localPath: saved.path, noteId: noteId, timestamp: "\(timeStamp)")

        case .none:
            break
        }
    }

    /// ONE construction/append path for captured media (was triplicated + note.contents).
    /// Private method (promoted from nested func): reusable by future flows, e.g. file import.
    /// noteId/timestamp are explicit params now — callers must keep them consistent
    /// with the saved file's folder/name (the nested version guaranteed this by capture).
    private func saveMedia(type: ContentType, localPath: String,
                           noteId: String, timestamp: String) {  // timestamps stay String (platform formats differ)
        let media = NoteContentObjectHelper.shared.createMedia(
            contentType: type,
            noteId: noteId,
            // 🔧 C4: Double position, appended at end (was hardcoded 0 for every media — ordering bug)
            positionedAt: Double(state.noteContents.count),
            localPath: localPath,
            url: "",
            duration: 0,
            timestamp: timestamp,
            // 🔧 C1: new media-metadata params (Kotlin defaults don't export to Swift — pass explicitly)
            title: "",
            mimeType: "",
            sizeBytes: 0,
            width: 0,
            height: 0,
            thumbnailPath: nil)
        addContent(content: media)
    }

    // MARK: - Save / Delete

    /// Replaces createorUpdateNoteCall(). Guarded, materializes state → Note, surfaces errors.
    func saveNote() {
        guard !state.isDeleted else { return }                  // fixes V3 (zombie note)
        guard let note = state.note else { return }             // fixes E5 (was note!)

        // Note is immutable (val) — build the edited copy via the Kotlin helper.
        // fixes V2 (title saved) + E4 (contents materialized once, at save)
        let toSave = note.withTitleAndContents(
            newTitle: state.title,
            newContents: state.noteContents
        )

        adapter.createOrUpdateNote(note: toSave) { [weak self] result in
            if case .failure(let error) = result {
                self?.state.errorMessage = error.message
                print("saveNote failed [\(error.code)] \(error.message)")
            }
        }
    }

    /// Replaces deleteNoteCall(); View no longer runs Task/casts. (fixes V5)
    func deleteNote(onDeleted: @escaping () -> Void) {
        guard let noteId = state.note?.id else { return }
        adapter.deleteNote(noteId: noteId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .loading:
                self.state.isLoading = true
            case .success:
                self.state.isLoading = false
                self.state.isDeleted = true                     // blocks onDisappear save
                onDeleted()                                     // View dismisses
            case .failure(let error):
                self.state.isLoading = false
                self.state.errorMessage = error.message
            case .idle: break
            }
        }
    }

    func shareNote() {}     // stub kept (nothing deleted)
}
