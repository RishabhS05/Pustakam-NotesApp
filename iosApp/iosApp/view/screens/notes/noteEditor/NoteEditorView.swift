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
//                                onDelete()
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
                 CardImageEditor(content: contentImage, actionClick: {})
            case .video:
                let contentVideo = content as! NoteContentModel.MediaContent
                VideoCardPlayer(content: contentVideo)
            case .audio:
                let contentAudio = content as! NoteContentModel.MediaContent

                 AudioPlayView(mediaContent: contentAudio)
                       
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
    private func callDelete(noteId: String?) {
        guard noteId.isNotNilOrEmpty() else { return }
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
            case .DELETE:
                return Alert(
                    title: Text("\(errorField.errorMessageTitle)").font(.headline.weight(.heavy)).foregroundColor(.red),
                    message: Text(errorField.errorMessage),
                    primaryButton: Alert.Button.default(Text("Cancel"), action: { resetAlert() }),
                    secondaryButton: Alert.Button.default(
                        Text("Confirm"),
                        action: {
                            callDelete(noteId: noteEditorViewModel.state.note?.id)  // 🔧 state.note
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
    
    private func resetAlert() {
        errorField.alertType = AlertUCPermission.WARNING
        errorField.errorMessage = ""
        errorField.showErrorAlert = false
        errorField.errorMessageTitle = ""
    }
    
}
struct NotebookStyleNoteView_Previews: PreviewProvider {
    static var previews: some View {
        NoteEditorView()
    }
}
