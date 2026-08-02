import SwiftUI
import shared

struct NoteEditorView: View {
    @Environment(Router.self) var router: Router
    @Environment(\.dismiss) private var dismiss
    @State private var showRecorder = false
    @State private var errorField: ErrorField = ErrorField()
    @State private var noteContent: String = ""
    @State private var isRulledEnabled: Bool = false
    @State private var  showDelete : Bool = false
    // 🔧 14-Jul-2026: NEW — id of the long-pressed content item awaiting delete confirmation.
    //   The DELETE_CONTENT alert deletes exactly this item (before: image delete removed the whole note).
    @State private var deleteContentId : String? = nil
    // 🔧 18-Jul-2026: NEW FEATURE (file import) — import options / picker / link prompt state
    @State private var showImportOptions = false
    @State private var showFilePicker = false
    @State private var showLinkPrompt = false
    @State private var importLink = ""
    // 🔧 20-Jul-2026: NEW — path of the image shown in the full-screen preview (nil = hidden)
    @State private var previewImagePath: String? = nil
    // 🔧 20-Jul-2026: NEW FEATURE (export) — format chooser + spinner while generating
    @State private var showExportOptions = false
    @State private var isExporting = false
    // 🔧 V1 fix: @StateObject (was @ObservedObject + inline init → VM recreated on every
    //           re-render, wiping edits). Note passed via init — setNote() no longer exists.
    @StateObject private var noteEditorViewModel: NoteEditorViewModel
    private var cameraPermission = CameraPermission()
    private var micPermission = MicPermission()
    init(note: Note? = nil) {
        _noteEditorViewModel = StateObject(wrappedValue: NoteEditorViewModel(note: note))
        // title now lives in the VM state (fixes lost-title bug); only showDelete stays local
        _showDelete = State(initialValue: note != nil)
    }
    var body: some View {
        ZStack(alignment: .topLeading) {
            ScrollView(.vertical){
            VStack(alignment: .leading) {
                NoteTextEditor(
                    text: $noteEditorViewModel.state.title,   // 🔧 title owned by VM → actually saved
                    placeholder: "Title : Keep your thoughts alive.",
                    fontSize: 22
                ).frame(minHeight: 20, maxHeight:.infinity)
                    ForEach(noteEditorViewModel.state.noteContents){ noteContent in
                        renderWidget(content: noteContent){
                            updatedContent in
                            noteEditorViewModel.updateContent(content: updatedContent)
                        }
                    }
                }
            }.frame(maxHeight: .infinity, alignment: .top)
            if showRecorder {
                AudioRecorderView(onSave : { media  in
                    noteEditorViewModel.getCapturedData(media:media)
                    showRecorder = false
                }).frame(alignment : .topTrailing)
            }
            if noteEditorViewModel.state.isLoading || isExporting {  // 🔧 spinner: VM state or export
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }
            OverlayEditorButtons(
                showDelete: noteEditorViewModel.state.note != nil,  // 🔧 state.note — updates when async note arrives
                onMediaCapture: {
                    guard cameraPermission.checkCameraPermission() else {
                        setAlert(alertType: .CAMERA)
                        return
                    }
                    guard micPermission.checkMicPermission() else {
                        setAlert(alertType: .MIC)
                        return
                    }
                    router.navigate(to: .Camera(){ data in
                        print("capturedData \(String(describing: data))")
                      noteEditorViewModel.getCapturedData(media: data)
                    })
                },
                onShare: { print("Share action") },
                onRecordMic: {
                    guard micPermission.checkMicPermission() else {
                        setAlert(alertType: .MIC)
                        return
                    }
                    showRecorder = true
                }, onAddTextField: {
                    noteEditorViewModel.addNewText()
                },
                onArrowButton: {},
                onImportFile: { showImportOptions = true }   // 🔧 18-Jul-2026: import entry point
            )
            .frame(alignment: .bottomTrailing)
            .padding()
        }
        // 🔧 18-Jul-2026: NEW FEATURE (file import) — choose device picker or link download
        .confirmationDialog("Import files", isPresented: $showImportOptions, titleVisibility: .visible) {
            Button("Import files from device") { showFilePicker = true }
            Button("Import from link") { importLink = ""; showLinkPrompt = true }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(isPresented: $showFilePicker) {
            MultiFilePicker { urls in noteEditorViewModel.importFiles(urls: urls) }
        }
        // 🔧 20-Jul-2026: NEW — full-screen zoomable image preview (opened from an image card tap)
        .fullScreenCover(isPresented: Binding(
            get: { previewImagePath != nil },
            set: { if !$0 { previewImagePath = nil } }
        )) {
            if let path = previewImagePath {
                ImagePreviewView(path: path) { previewImagePath = nil }
            }
        }
        // 🔧 20-Jul-2026: NEW FEATURE (export) — pick a format, generate off-main, then share
        .confirmationDialog("Export note", isPresented: $showExportOptions, titleVisibility: .visible) {
            Button("Export as PDF") { runExport(.pdf) }
            Button("Export as Image") { runExport(.image) }
            Button("Export as Word (DOCX)") { runExport(.docx) }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Import from link", isPresented: $showLinkPrompt) {
            TextField("https://example.com/file.pdf", text: $importLink)
                .textInputAutocapitalization(.never).keyboardType(.URL)
            Button("Import") { noteEditorViewModel.importFromLink(importLink) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Downloads the file behind any URL. If the link has no file, you'll see “No file found”.")
        }
        .alert(isPresented: $errorField.showErrorAlert) {
            throwAlert()
        }
        .padding(.horizontal, 12)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                BackButton(action: {
                    dismiss()
                })
            }
            
            ToolbarItem(placement: .topBarTrailing) {
                HStack{
                    // 🔧 18-Jul-2026: NEW — open this note as a real page-curl book
                    ActionButtonWithoutBackground(iconName: "book",
                                                  enabled : noteEditorViewModel.isNoteValid(),
                                                  action: {
                        if let noteId = noteEditorViewModel.state.note?.id {
                            router.navigate(to: .NoteBookReader(noteId: noteId))
                        }
                    }, tint: Theme.Colors.secondary)
                    // 🔧 20-Jul-2026: NEW — export this note as PDF / Image / Word
                    ActionButtonWithoutBackground(iconName: "square.and.arrow.up.on.square", action: {
                        showExportOptions = true
                    }, tint: Theme.Colors.secondary)
                    ActionButtonWithoutBackground(iconName: "tray.and.arrow.down", action: {
                        saveNote()
                    }, tint :Theme.Colors.secondary)
                    ActionButtonWithoutBackground(iconName: "arrow.down.document",
                action: {},tint : Theme.Colors.secondary)
                        ActionButtonWithoutBackground(iconName: "trash", action: {
                            setAlert(message: "Are you sure you want to delete this note?", title: "Delete note", alertType: .DELETE )
                        }, tint: Color.red)
                }
            }
        }
        .onDisappear {saveNote() }
        // 📖 23-Jul-2026: re-read on return (e.g. back from the book reader) so document cards show
        //   the reading position the reader just saved. Without this the count stayed stale.
        .onAppear { noteEditorViewModel.refresh() }
    }
    
    
    @ViewBuilder
    func renderWidget(content : NoteContentModel, onUpdate :  @escaping (NoteContentModel)-> Void ) -> some View {
      
        switch content.type {
            case .text:
                let textContent = content as! NoteContentModel.TextContent
                 NoteTextFieldWrapper(
                    text: textContent.text,
                    onTextChange: {
                        newValue in
                        // 🔧 F2 (C2): immutable copy via withText — also stamps content updatedAt
                        onUpdate(textContent.withText(newText: newValue.string))
                    }
                )
            case .image :
                let contentImage = content as! NoteContentModel.MediaContent
            // 🔧 14-Jul-2026: BUGFIX — was alertType .DELETE, whose Confirm ran callDelete()
            //   → the WHOLE note was deleted. Now: remember the pressed item's id and raise
            //   the DELETE_CONTENT alert, which deletes only that item. (CardImageEditor UI untouched.)
            // 🔧 14-Jul-2026: NEW — actionSave saves the image silently to the Photos gallery.
            CardImageEditor(content: contentImage, actionClick: {
                // 🔧 20-Jul-2026: NEW — tap opens the full-screen zoomable image preview (was a no-op)
                previewImagePath = contentImage.getMediaUrl()
            },actionDelete: {
                askDeleteContent(contentId: contentImage.id, kind: "Image")
            }, actionSave: {
                saveMediaToDevice(media: contentImage)
            }
                 )

            case .video:
                let contentVideo = content as! NoteContentModel.MediaContent
                // 🔧 14-Jul-2026: NEW — video had no delete UI; long-press actions bar added
                //   in VideoCardPlayer (same pattern as CardImageEditor), wired here.
                //   actionSave saves the video silently to the Photos gallery.
                VideoCardPlayer(content: contentVideo, cardPadding: 0, actionDelete: {
                    askDeleteContent(contentId: contentVideo.id, kind: "Video")
                }, actionSave: {saveMediaToDevice(media: contentVideo)})
            case .audio:
                let contentAudio = content as! NoteContentModel.MediaContent

                 // 🔧 14-Jul-2026: NEW — trash button existed inside AudioPlayView but onDelete
                 //   was never passable through its init; now wired to the same confirm flow.
                 //   onSave exports the audio via the Files picker (default Documents).
                 AudioPlayView(mediaContent: contentAudio, onDelete: {
                    askDeleteContent(contentId: contentAudio.id, kind: "Audio")
                 }, onSave: {
                    saveMediaToDevice(media: contentAudio)
                 })

            // 🔧 19-Jul-2026: notebook widget — file's real pages flip inline, "n/total" at bottom
            //   (DocumentFileCardView kept for reuse elsewhere). Full open = ONLY this file (single).
            case .pdf, .docx, .epub, .txt, .md, .other:
                let contentDoc = content as! NoteContentModel.MediaContent
                InlineBookFileView(
                    media: contentDoc,
                    onOpenFull: {
                        // 📖 23-Jul-2026 FIX — save first so a JUST-ADDED file exists in the DB
                        //   before the reader reads it; without this the reader spun on the loader.
                        let cid = contentDoc.id
                        noteEditorViewModel.saveThenOpen {
                            if let noteId = noteEditorViewModel.state.note?.id {
                                router.navigate(to: .BookReader(noteId: noteId, bookId: cid))
                            }
                        }
                    },
                    onDelete: { askDeleteContent(contentId: contentDoc.id, kind: "File") },
                    onSave: { saveMediaToDevice(media: contentDoc) },
                    // 🔧 25-Jul-2026: share the document file via the system share sheet (reuses NoteExporter.share)
                    onShare: { shareMediaFile(media: contentDoc) }
                )
                .frame(width: UIScreen.main.bounds.width * 0.7, alignment: .leading)

            // 🔧 18-Jul-2026: GIF gets the image card (was falling into the text default)
            case .gif:
                let contentGif = content as! NoteContentModel.MediaContent
                CardImageEditor(content: contentGif, actionClick: {}, actionDelete: {
                    askDeleteContent(contentId: contentGif.id, kind: "Image")
                }, actionSave: {
                    saveMediaToDevice(media: contentGif)
                })

            default : NoteTextFieldWrapper()
        }

    }
    
    
    func shareNote(){
            // share note link via different apps
    }
    
    private func saveNote(){
        noteEditorViewModel.saveNote()   // 🔧 new VM API (guards deleted-note + materializes title/contents)
    }

    // 🔧 25-Jul-2026: NEW — share a document/media file via the system share sheet (ImageCardView-style
    //   actions on the inline document card). Reuses NoteExporter.share; resolves the container-safe path
    //   (stored absolute paths go stale across app updates).
    private func shareMediaFile(media: NoteContentModel.MediaContent) {
        guard let path = LocalFilePathResolver_iosKt.resolveLocalFilePath(path: media.localPath) ?? media.localPath,
              !path.isEmpty, FileManager.default.fileExists(atPath: path) else {
            noteEditorViewModel.state.errorMessage = "File not available to share."
            return
        }
        NoteExporter.share(url: URL(fileURLWithPath: path))
    }

    // 🔧 20-Jul-2026: NEW FEATURE (export) — generate the file off-main, then open the share sheet
    private func runExport(_ format: ExportFormat) {
        guard let note = noteEditorViewModel.state.note else { return }
        isExporting = true
        DispatchQueue.global(qos: .userInitiated).async {
            let url = NoteExporter.export(note: note, format: format)
            DispatchQueue.main.async {
                isExporting = false
                if let url { NoteExporter.share(url: url) }
            }
        }
    }

        // handler call wrappers
    private func callDelete() {
        // 🔧 new VM API: callback-based, no Task, no `as! NetworkError` crash cast.
        //   Loading spinner comes from state.isLoading; errors surface via state.errorMessage.
        noteEditorViewModel.deleteNote {
            dismiss()
        }
    }
    
    /**
     Render Alert on Screen
     */
    func throwAlert() -> Alert {
        switch errorField.alertType {
        case .DELETE_CONTENT: return (Alert(
            title: Text("\(errorField.errorMessageTitle)").font(.headline.weight(.heavy)).foregroundColor(.red),
            message: Text(errorField.errorMessage),
            primaryButton: Alert.Button.default(Text("Cancel"), action: { resetAlert() }),
            secondaryButton: Alert.Button.default(
                Text("Confirm"),
                action: {
                    // 🔧 14-Jul-2026: BUGFIX — Confirm did nothing (and image was routed to
                    //   .DELETE = whole note). Now deletes exactly the long-pressed item.
                    if let contentId = deleteContentId {
                        noteEditorViewModel.deleteContent(contentId: contentId)
                    }
                    resetAlert()
                }))) 
            case .DELETE:
                return Alert(
                    title: Text("\(errorField.errorMessageTitle)").font(.headline.weight(.heavy)).foregroundColor(.red),
                    message: Text(errorField.errorMessage),
                    primaryButton: Alert.Button.default(Text("Cancel"), action: { resetAlert() }),
                    secondaryButton: Alert.Button.default(
                        Text("Confirm"),
                        action: {
                            callDelete()  // 🔧 state.note
                            resetAlert()
                        }))
            case .CAMERA : return cameraPermission.showAlert { resetAlert() }
                
            case .MIC: return micPermission.showAlert { resetAlert() }
                
            case .LOCATION : return cameraPermission.showAlert { resetAlert() }
                
            default:
                return Alert(
                    title: Text("\(errorField.errorMessageTitle)").font(.headline.weight(.heavy)).foregroundColor(.red),
                    message: Text(errorField.errorMessage),
                    dismissButton: Alert.Button.default(Text("Cancel"), action: { resetAlert() }))
                
        }
        
    }
    /**
     Configure Alert According to condition
     */
    private func setAlert(message: String = "", title: String = "Error", alertType : AlertUCPermission) {
        errorField.alertType = alertType
        errorField.errorMessage = message
        errorField.showErrorAlert = true
        errorField.errorMessageTitle = title
    }
    
    // 🔧 14-Jul-2026: NEW — one entry point for every content-delete request (image/video/audio).
    //   Remembers WHICH item was long-pressed, then raises the DELETE_CONTENT confirm alert.
    //   Usage: askDeleteContent(contentId: content.id, kind: "Image")
    private func askDeleteContent(contentId: String, kind: String) {
        deleteContentId = contentId
        setAlert(message: "Are you sure you want to delete this \(kind)?",
                 title: "Delete \(kind)", alertType: .DELETE_CONTENT)
    }

    private func resetAlert() {
        errorField.alertType = AlertUCPermission.WARNING
        errorField.errorMessage = ""
        errorField.showErrorAlert = false
        errorField.errorMessageTitle = ""
        deleteContentId = nil   // 🔧 14-Jul-2026: clear selection on cancel/confirm alike
    }
    
}
struct NotebookStyleNoteView_Previews: PreviewProvider {
    static var previews: some View {
        NoteEditorView()
    }
}
