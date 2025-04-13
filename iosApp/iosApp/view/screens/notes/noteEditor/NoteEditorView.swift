import SwiftUI
import shared

struct NoteEditorView: View {
    @Environment(Router.self) var router: Router
    @Environment(\.dismiss) private var dismiss
    @State private var showRecorder = false
    @State private var errorField: ErrorField = ErrorField()
    @State private var title: String = ""
    @State private var noteContent: String = ""
    @State private var isRulledEnabled: Bool = false
    @State private var isLoading: Bool = false
    @State private var capturedData: CapturedMedia?
    
    @ObservedObject private  var noteEditorViewModel = NoteEditorViewModel()
    private var cameraPermission = CameraPermission()
    private var micPermission = MicPermission()
    
    
    init(note: Note? = nil) {
        noteEditorViewModel.setNote(note: note)
        if note != nil {
            _title = State(initialValue: note!.title ?? "")
        }
    }
    var body: some View {
        ZStack(alignment: .topLeading) {
            VStack(alignment: .leading) {
                NoteTextEditor(
                    text: $title,
                    placeholder: "Title : Keep your thoughts alive.",
                    fontSize: 22
                ).frame(minHeight: 20, maxHeight: 100)
                
                ForEach(noteEditorViewModel.noteContents){ noteContent in
                    renderWidget(content: noteContent){
                        updatedContent in
                        noteEditorViewModel.updateContent(content: updatedContent)
                    }
                }
            }.frame(maxHeight: .infinity, alignment: .top)
            if showRecorder {
                AudioRecorderView().frame(alignment : .topTrailing)}
            if isLoading {
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }
            OverlayEditorButtons(
                showDelete:noteEditorViewModel.note != nil,
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
                        capturedData = data
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
                        }, tint :.brown)
                        ActionButtonWithoutBackground(iconName: "arrow.down.document",
                                                      action: {
                            
                        },tint : .brown)
                        ActionButtonWithoutBackground(iconName: "square.and.arrow.up", action: {
                            shareNote()
                        },tint : .brown)
                    }
                }
            }.onDisappear {
                saveNote()
            }
    }

    func renderWidget(content : NoteContentModel, onUpdate :  @escaping (NoteContentModel)-> Void ) -> some View {
        switch content.type {
            case .text:
                let textContent = content as! NoteContentModel.TextContent
                return NoteTextFieldWrapper(
                    onTextChange: {
                        newValue in
                        textContent.text = newValue
                        onUpdate(textContent)
                    }
                )
                
            case .image :
                let contentImage = content as! NoteContentModel.MediaContent
                let path = contentImage.getMediaUrl()
                return NoteTextFieldWrapper()
            
        
            case .video:
                let contentVideo = content as! NoteContentModel.MediaContent
                    //                    VideoCard(contentVideo, onClick = onMediaPreview)
                return NoteTextFieldWrapper()
            
        
            case .audio:
                let contentAudio = content as! NoteContentModel.MediaContent
        
                return NoteTextFieldWrapper()
            
        
        
            case .link :
                let contentLink = content as! NoteContentModel.Link
                return NoteTextFieldWrapper()

        
            case .docx :
                let contentDoc = content as! NoteContentModel.MediaContent
                let path = contentDoc.getMediaUrl()
                return NoteTextFieldWrapper()
            
        
            case .location:
                let locationContent = content as! NoteContentModel.Location
                return NoteTextFieldWrapper()
            
        
            case .pdf :
                return NoteTextFieldWrapper()
            
            case .gif :
                return NoteTextFieldWrapper()
            
                       
            default : return NoteTextFieldWrapper()
        }
                
    }
    
    
    func shareNote(){
            // share note link via different apps
    }
    
    private func saveNote(){
        noteEditorViewModel.createorUpdateNoteCall()
    }
    
        // handler call wrappers
    private func callDelete(noteId: String?) {
        guard noteId.isNotNilOrEmpty() else { return }
        Task {
            isLoading = true
            let apiResponse = await noteEditorViewModel.deleteNoteCall(noteId: noteId!)
            isLoading = false
            if apiResponse.error != nil {
                errorField.errorMessage = (apiResponse.error as! NetworkError).getError()
                errorField.showErrorAlert = apiResponse.isSuccessful == false
            }
            if apiResponse.isSuccessful {
                dismiss()
            }
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
                            callDelete(noteId: noteEditorViewModel.note?.id)
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
