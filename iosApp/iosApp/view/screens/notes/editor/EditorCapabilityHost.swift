import SwiftUI
import shared

struct EditorCapabilityCallbacks {
    var onState: (EditorCapabilityState) -> Void = { _ in }
    var onOpenCamera: () -> Void = {}
    var onCaptured: (CapturedMedia?) -> Void = { _ in }
    var onFilesPicked: ([URL]) -> Void = { _ in }
    var onImportLink: (String) -> Void = { _ in }
    var onDeleteContent: (String) -> Void = { _ in }
    var onDeleteNote: () -> Void = {}
    var onPermissionDenied: (CaptureKind) -> Void = { _ in }
}

struct EditorCapabilityHost: ViewModifier {

    let state: EditorCapabilityState
    let noteTitle: String
    let callbacks: EditorCapabilityCallbacks

    @State private var showLinkPrompt = false
    @State private var linkText = ""
    @State private var showFilePicker = false

    private let cameraPermission = CameraPermission()
    private let micPermission = MicPermission()

    private var reducer: EditorCapabilityReducer { EditorCapabilityReducer.shared }

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) { recorder }
            .onChange(of: state.isPermissionPromptVisible) { _, visible in
                if visible { resolvePermission() }
            }
            .confirmationDialog(
                "Import files",
                isPresented: importSheetBinding,
                titleVisibility: .visible
            ) {
                Button("Import files from device") { showFilePicker = true }
                Button("Import from link") { linkText = ""; showLinkPrompt = true }
                Button("Cancel", role: .cancel) { dismissImport() }
            }
            .sheet(isPresented: $showFilePicker) {
                MultiFilePicker { urls in
                    callbacks.onFilesPicked(urls)
                    dismissImport()
                }
            }
            .alert("Import from link", isPresented: $showLinkPrompt) {
                TextField("https://example.com/file.pdf", text: $linkText)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
                Button("Import") {
                    callbacks.onImportLink(linkText)
                    dismissImport()
                }
                Button("Cancel", role: .cancel) { dismissImport() }
            } message: {
                Text("Downloads the file behind any URL.")
            }
            .alert("Delete this block?", isPresented: deleteContentBinding) {
                Button("Delete", role: .destructive) {
                    if let id = state.pendingDeleteContentId { callbacks.onDeleteContent(id) }
                    callbacks.onState(reducer.dismissDelete(state: state))
                }
                Button("Cancel", role: .cancel) {
                    callbacks.onState(reducer.dismissDelete(state: state))
                }
            }
            .alert("Delete \(noteTitle)?", isPresented: deleteNoteBinding) {
                Button("Delete", role: .destructive) {
                    callbacks.onDeleteNote()
                    callbacks.onState(reducer.dismissDelete(state: state))
                }
                Button("Cancel", role: .cancel) {
                    callbacks.onState(reducer.dismissDelete(state: state))
                }
            }
    }

    @ViewBuilder
    private var recorder: some View {
        if state.isRecordingAudio {
            AudioRecorderView { media in
                callbacks.onCaptured(media)
                callbacks.onState(reducer.stopAudio(state: state))
            }
        }
    }

    private var importSheetBinding: Binding<Bool> {
        Binding(
            get: { state.isImportSheetVisible },
            set: { if !$0 { dismissImport() } }
        )
    }

    private var deleteContentBinding: Binding<Bool> {
        Binding(
            get: { state.pendingDeleteContentId != nil },
            set: { if !$0 { callbacks.onState(reducer.dismissDelete(state: state)) } }
        )
    }

    private var deleteNoteBinding: Binding<Bool> {
        Binding(
            get: { state.isDeleteNotePromptVisible },
            set: { if !$0 { callbacks.onState(reducer.dismissDelete(state: state)) } }
        )
    }

    private func dismissImport() {
        callbacks.onState(reducer.setImportSheet(state: state, visible: false))
    }

    private func resolvePermission() {
        guard let kind = state.pendingCapture else { return }
        switch kind {
        case CaptureKind.image, CaptureKind.video:
            requirePermission(cameraPermission, onGranted: {
                requirePermission(micPermission, onGranted: {
                    callbacks.onState(reducer.granted(state: state))
                    callbacks.onOpenCamera()
                }, onDenied: { deny(kind) })
            }, onDenied: { deny(kind) })

        case CaptureKind.audio:
            requirePermission(micPermission, onGranted: {
                callbacks.onState(reducer.granted(state: state))
            }, onDenied: { deny(kind) })

        default:
            callbacks.onState(reducer.granted(state: state))
        }
    }

    private func deny(_ kind: CaptureKind) {
        callbacks.onState(reducer.denied(state: state))
        callbacks.onPermissionDenied(kind)
    }
}

enum EditorCapture {

    static func persist(
        media: CapturedMedia?,
        noteId: String,
        positionedAt: Double
    ) -> NoteContentModel.MediaContent? {
        guard let media else { return nil }
        let timestamp = DateTimeUtilsKt.getCurrentTimestamp()
        switch media {
        case .image(let image):
            guard let data = image.pngData() else { return nil }
            let dest = PathPolicy.shared.capturePath(
                type: ContentType.image, noteId: noteId, timestamp: timestamp
            )
            let path = saveImageFile(data: data, in: dest.folder, to: dest.fileName)
            return content(ContentType.image, path, noteId, positionedAt, "\(timestamp)")

        case .video(let source):
            return copied(ContentType.video, source, noteId, positionedAt, timestamp)

        case .audio(let source):
            return copied(ContentType.audio, source, noteId, positionedAt, timestamp)
        }
    }

    private static func copied(
        _ type: ContentType,
        _ source: URL,
        _ noteId: String,
        _ positionedAt: Double,
        _ timestamp: Int64
    ) -> NoteContentModel.MediaContent? {
        let dest = PathPolicy.shared.capturePath(
            type: type, noteId: noteId, timestamp: timestamp
        )
        guard let saved = copyFile(to: dest.folder, fileName: dest.fileName, from: source)
        else { return nil }
        return content(type, saved.path, noteId, positionedAt, "\(timestamp)")
    }

    private static func content(
        _ type: ContentType,
        _ localPath: String,
        _ noteId: String,
        _ positionedAt: Double,
        _ timestamp: String
    ) -> NoteContentModel.MediaContent {
        NoteContentObjectHelper.shared.createMedia(
            contentType: type,
            noteId: noteId,
            positionedAt: positionedAt,
            localPath: localPath,
            url: "",
            duration: 0,
            timestamp: timestamp,
            title: "",
            mimeType: "",
            sizeBytes: 0,
            width: 0,
            height: 0,
            thumbnailPath: nil
        )
    }
}

extension View {
    func editorCapabilities(
        state: EditorCapabilityState,
        noteTitle: String,
        callbacks: EditorCapabilityCallbacks
    ) -> some View {
        modifier(
            EditorCapabilityHost(state: state, noteTitle: noteTitle, callbacks: callbacks)
        )
    }
}
