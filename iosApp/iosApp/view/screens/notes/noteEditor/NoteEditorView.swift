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
            if noteEditorViewModel.state.isLoading {          // 🔧 spinner driven by VM state
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
                onArrowButton: {}
            )
            .frame(alignment: .bottomTrailing)
            .padding()
        }
        .alert(isPresented: $errorField.showErrorAlert) {
            throwAlert()
        }.padding(.horizontal, 12)
            .navigationBarBackButtonHidden(true)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    BackButton(action: {
                        dismiss()
                    })
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    HStack{
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
            }.onDisappear {
                saveNote()
            }
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
            CardImageEditor(content: contentImage, actionClick: {},actionDelete: {
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
                VideoCardPlayer(content: contentVideo, actionDelete: {
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

            default : NoteTextFieldWrapper()
        }
                
    }
    
    
    func shareNote(){
            // share note link via different apps
    }
    
    private func saveNote(){
        noteEditorViewModel.saveNote()   // 🔧 new VM API (guards deleted-note + materializes title/contents)
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
