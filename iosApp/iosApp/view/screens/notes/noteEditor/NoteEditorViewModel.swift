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
    var isNoteReady: Bool { note != nil }
}

class NoteEditorViewModel: ObservableObject {

    @Published var state = NoteEditorUIState()

    private let adapter: NotesBridgeAdapter
    private let contentBridge: NoteContentBridge
    private var contentUpdatesHandle: Closeable?
    private var dirtyContentIds = Set<String>()

    /// DI per series convention: defaults keep call sites/tests simple. (fixes E1, E6)
    init(noteId: String? = nil,
         adapter: NotesBridgeAdapter = NotesBridgeAdapter(),
         contentBridge: NoteContentBridge = NoteContentBridge()) {
        self.adapter = adapter
        self.contentBridge = contentBridge
        load(noteId: noteId)
        observeContentUpdates()
    }

    deinit {
        contentUpdatesHandle?.close()
        contentBridge.dispose()
    }

    private func observeContentUpdates() {
        contentUpdatesHandle = contentBridge.observeSelectedMedia { [weak self] media in
            guard let self else { return }
            for updated in media {
                guard let index = self.state.noteContents.firstIndex(where: { $0.id == updated.id }) else { continue }
                guard self.state.noteContents[index].updatedAt != updated.updatedAt else { continue }
                self.state.noteContents[index] = updated
            }
        }
    }

    // MARK: - Load
    func refresh() {
        guard let id = state.note?.id, !id.isEmpty else { return }
        load(noteId: id)
    }

    private func load(noteId: String?) {
        adapter.readNote(noteId: noteId) { [weak self] result in
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

    private func apply(note: Note) {
        state.note = note
        state.title = note.title ?? ""
        state.noteContents = note.contents as? [NoteContentModel] ?? []
        contentBridge.setSelectedNote(note: note)
    }

    // MARK: - Content editing (single list; note.contents materialized only at save)

    func addContent(content: NoteContentModel) {
        guard state.isNoteReady else { return }                 // fixes E3 crash window
        dirtyContentIds.insert(content.id)   // 🔧 15-Jul-2026 iOS parity: new block → must be saved
        state.noteContents.append(content)
        if content.isPlayingMedia(), let media = content as? NoteContentModel.MediaContent {
            contentBridge.updateMediaContent(content: media)    // safe cast (was as!)
        }
    }

    func updateContent(content: NoteContentModel) {
        dirtyContentIds.insert(content.id)   // 🔧 15-Jul-2026 iOS parity: touched → will be saved
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

        // 🔧 30-Jul-2026 Phase 4 — folder + file name now come from the SHARED PathPolicy instead of
        //   being built inline. Same rule Android uses, so the two platforms can no longer drift.
        //   ONE deliberate change: the folder is now lowercase ("image/<noteId>") where iOS used to
        //   write "IMAGE/<noteId>". Existing media is unaffected — resolveLocalFilePath re-anchors
        //   everything after /Documents/ verbatim, so old rows keep their uppercase path and still
        //   resolve. No migration needed. See migration-guide.md Phase 4.
        switch media {
        case .image(let image):
            let type = ContentType.image
            guard let data = image.pngData() else { return }
            let dest = PathPolicy.shared.capturePath(type: type, noteId: noteId, timestamp: timeStamp)
            let path = saveImageFile(data: data, in: dest.folder, to: dest.fileName)
            saveMedia(type: type, localPath: path, noteId: noteId, timestamp: "\(timeStamp)")

        case .video(let path):
            let type = ContentType.video
            let dest = PathPolicy.shared.capturePath(type: type, noteId: noteId, timestamp: timeStamp)
            guard let saved = copyFile(to: dest.folder, fileName: dest.fileName, from: path)
            else { return }
            saveMedia(type: type, localPath: saved.path, noteId: noteId, timestamp: "\(timeStamp)")

        case .audio(let path):
            let type = ContentType.audio
            let dest = PathPolicy.shared.capturePath(type: type, noteId: noteId, timestamp: timeStamp)
            guard let saved = copyFile(to: dest.folder, fileName: dest.fileName, from: path)
            else { return }
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
        // 🔧 15-Jul-2026 iOS parity (Phase 2.3): thumbnail generated AFTER the media is already
        //   visible, off the main thread; on completion the LATEST version of the content is
        //   updated by id (never clobbers meanwhile edits) — updateContent marks it dirty too.
        generateThumbnailAsync(for: media)
    }

    // MARK: - File import (18-Jul-2026)

    // 🔧 18-Jul-2026: NEW FEATURE (file import) — device multi-pick: copy on background, append on main
    func importFiles(urls: [URL]) {
        guard let noteId = state.note?.id else {
            state.errorMessage = "Note is still loading. Try again."
            return
        }
        guard !urls.isEmpty else { return }
        state.isLoading = true
        let startPosition = Double(state.noteContents.count)
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            let items = FileImportService.importPicked(urls: urls, noteId: noteId, startPosition: startPosition)
            DispatchQueue.main.async {
                guard let self else { return }
                self.state.isLoading = false
                guard !items.isEmpty else {
                    self.state.errorMessage = "Couldn't import the selected files."
                    return
                }
                items.forEach { self.addContent(content: $0) }          // dirty + playlist handled inside
                items.forEach { self.generateThumbnailAsync(for: $0) }  // image/video thumbs only
            }
        }
    }

    // 🔧 18-Jul-2026: NEW FEATURE (file import) — link import: download if it IS a file, else "No file found"
    func importFromLink(_ url: String) {
        guard let noteId = state.note?.id else {
            state.errorMessage = "Note is still loading. Try again."
            return
        }
        guard !url.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        state.isLoading = true
        FileImportService.importFromLink(url, noteId: noteId,
                                         startPosition: Double(state.noteContents.count)) { [weak self] result in
            guard let self else { return }
            self.state.isLoading = false
            switch result {
            case .success(let items):
                items.forEach { self.addContent(content: $0) }
                items.forEach { self.generateThumbnailAsync(for: $0) }
            case .noFileFound:
                self.state.errorMessage = "No file found at this link."
            case .failed(let message):
                self.state.errorMessage = message
            }
        }
    }

    // 🔧 15-Jul-2026 iOS parity (Phase 2.3): background thumbnail job (Android
    //   generateThumbnailsFor parity). Images and videos only; audio has no thumbnail.
    private func generateThumbnailAsync(for media: NoteContentModel.MediaContent) {
        guard media.type == ContentType.image || media.type == ContentType.video,
              let path = media.localPath, !path.isEmpty else { return }
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let thumb = generateThumbnail(sourcePath: path, type: media.type) else { return }
            DispatchQueue.main.async {
                guard let self else { return }
                let latest = self.state.noteContents.first { $0.id == media.id }
                    as? NoteContentModel.MediaContent ?? media
                self.updateContent(content: latest.withThumbnail(path: thumb))
            }
        }
    }

    // MARK: - Save / Delete

    /// Replaces createorUpdateNoteCall(). Guarded, materializes state → Note, surfaces errors.
    // 📖 23-Jul-2026 FIX (full-screen reader stuck on the loader for a JUST-ADDED file): imported
    //   files live only in the in-memory note until a save; the reader reads the DB, so a not-yet-
    //   saved note/content isn't found and the read fails → the reader spins on the loader forever.
    //   This flushes the current note through the SAME create/update bridge (→ use case), then runs
    //   onSaved so navigation happens only after the file exists in the DB. No title requirement:
    //   a file-only note must still open.
    func saveThenOpen(onSaved: @escaping () -> Void) {
        guard !state.isDeleted, let note = state.note, !state.noteContents.isEmpty else {
            onSaved(); return
        }
        let toSave = note.withTitleAndContents(newTitle: state.title, newContents: state.noteContents)
        let dirtySnapshot = dirtyContentIds
        adapter.createOrUpdateNote(note: toSave, dirtyContentIds: dirtySnapshot) { [weak self] result in
            switch result {
            case .success:
                self?.dirtyContentIds.subtract(dirtySnapshot)
                onSaved()
            case .failure(let error):
                // still navigate — the reader will show its own error if the file truly can't load
                print("saveThenOpen failed [\(error.code)] \(error.message)")
                onSaved()
            case .loading, .idle:
                break
            }
        }
    }
    func isNoteValid() -> Bool {
        guard let note = state.note else { return false}
        if (state.title.isEmpty) {
            if (state.noteContents.isEmpty){
               return false
            }
        }
        return true
    }


    func saveNote() {
         guard !state.isDeleted, let note = state.note, isNoteValid() else { return }
        
        
        // 🔧 15-Jul-2026 iOS parity (Phase 2.1): split oversized plain-text blocks BEFORE the
        //   upsert — never while typing. Shared TextBlockSplitter (same as Android): paragraph
        //   boundaries, fractional positions, rich-text blocks skipped. New chunks join the
        //   dirty set so the dirty-row save writes them.
        if let split = TextBlockSplitter.shared.splitOversized(contents: state.noteContents) {
            state.noteContents = split.contents as? [NoteContentModel] ?? state.noteContents
            split.changedIds.forEach { id in
                if let id = id as? String { dirtyContentIds.insert(id) }
            }
        }

        // Note is immutable (val) — build the edited copy via the Kotlin helper.
        // fixes V2 (title saved) + E4 (contents materialized once, at save)
        let toSave = note.withTitleAndContents(
            newTitle: state.title,
            newContents: state.noteContents
        )

        // 🔧 15-Jul-2026 iOS parity (Phase 0.4): dirty-row save — only touched content rows are
        //   written (plus the header). The snapshot is cleared on success; anything edited DURING
        //   the save stays dirty for the next one (Android parity).
        let dirtySnapshot = dirtyContentIds
        adapter.createOrUpdateNote(note: toSave, dirtyContentIds: dirtySnapshot) { [weak self] result in
            switch result {
            case .success:
                self?.dirtyContentIds.subtract(dirtySnapshot)
            case .failure(let error):
                self?.state.errorMessage = error.message
                print("saveNote failed [\(error.code)] \(error.message)")
            case .loading, .idle:
                break
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
                self.state.isDeleted = true
                onDelete(noteId: noteId)
                onDeleted()
                // View dismisses
            case .failure(let error):
                self.state.isLoading = false
                self.state.errorMessage = error.message
            case .idle: break
            }
        }
    }
   private func onDelete(noteId : String) {
        adapter.deleteNote(noteId: noteId , onState: { _ in })
    }

    // 🔧 14-Jul-2026: NEW — delete ONE content block (the long-pressed item), note stays.
    //   Fixes: image delete alert was wired to whole-note delete → entire note vanished.
    //   Mirrors Android NoteEditorViewModel.removeContent():
    //     1. media → remove its local file from disk
    //     2. remove the row from DB via bridge deleteNoteContentUseCase
    //     3. drop the item from state.noteContents (UI updates instantly)
    //   Usage (from View, after user confirms the alert):
    //     noteEditorViewModel.deleteContent(contentId: id)
    func deleteContent(contentId: String) {
        dirtyContentIds.remove(contentId)   // 🔧 15-Jul-2026 iOS parity: deleted → nothing to save
        guard let content = state.noteContents.first(where: { $0.id == contentId }) else { return }

        // 1. remove local media file (image/video/audio) — safe no-op for text
        if content.isMediaFile(), let media = content as? NoteContentModel.MediaContent,
           let localPath = media.localPath {
            // 🔧 15-Jul-2026 iOS MEDIA-LOST FIX: resolve first — stored paths keep the OLD container
            //   UUID after an app update, so deleting at the raw path would orphan the real file.
            deleteFile(filePath: LocalFilePathResolver_iosKt.resolveLocalFilePath(path: localPath) ?? localPath)
            // thumbnail lives in Documents/thumbnails — remove it with its media
            if let thumb = LocalFilePathResolver_iosKt.resolveLocalFilePath(path: media.thumbnailPath) {
                deleteFile(filePath: thumb)
            }
        }

        // 2. remove from DB (write call — survives screen death, like saveNote)
        adapter.deleteNoteContent(contentId: contentId) { [weak self] result in
            if case .failure(let error) = result {
                self?.state.errorMessage = error.message
                print("deleteContent failed [\(error.code)] \(error.message)")
            }
        }

        // 3. remove from UI state — save() materializes contents from this list,
        //    so the deleted block can never come back on the next save
        state.noteContents.removeAll { $0.id == contentId }
    }

    func shareNote() {}     // stub kept (nothing deleted)
}
