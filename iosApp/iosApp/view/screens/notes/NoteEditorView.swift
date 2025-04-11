import SwiftUI
import shared

    // note viewmodels are basically ui logic handlers only
class NoteEditorViewModel: BaseViewModel, ObservableObject {
    var note: Note? = nil
    
    @Published var noteContents = [NoteContentModel]()
    override init() {}
    func setNote(note : Note?){
        if note == nil {
            Task{
           let value =
                await apiHandler(apiCall: {
                    try await noteRepositary.getANote(id: nil)
                })
                
                print("value \(value.data as? Note)")
                self.note = value.data as? Note
            
                print()
            }
        }else { self.note = note } 
    }
    
    private func  createUpdateNoteCall() async -> BaseResult<BaseResponse<Note>?> {
        
        return await apiHandler(apiCall: {
            try await noteRepositary.insertOrUpdateNote(note : self.note!)
        })
    }
    func createorUpdateNoteCall() {
        Task {
         let data =  await createUpdateNoteCall()
        }
    }
        // delete a note
    func deleteNoteCall(noteId: String) async -> BaseResult<BaseResponse<DeleteDataModel>?> {
        return await apiHandler(apiCall: { try await noteRepositary.deleteNote(id: noteId) })
    }
    
    
    func saveMedia(mediaPath : String){
        
    }

    
    func createNoteContent () {}
    /**
     Handling note content
     **/
        // add new note content
    func addNewContent(){}
    
    
    
        // remove note content from list, db , server and stroage
    func removeContent(){}
    
        //
    func addContentData() {}
    
    
    func shareNote(){}
    
}

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
    
    private var noteEditorViewModel = NoteEditorViewModel()
    private var cameraPermission = CameraPermission()
    private var micPermission = MicPermission()
    
    
    init(note: Note? = nil) {
        noteEditorViewModel.setNote(note: note)
        if note != nil {
            _title = State(initialValue: note!.title ?? "")
        }
    }
    var body: some View {
        let _: CGFloat = isRulledEnabled ? 100 : 0
        ZStack(alignment: .topLeading) {
            VStack(alignment: .leading) {
                NoteTextEditor(
                    text: $title,
                    placeholder: "Title : Keep your thoughts alive.",
                    fontSize: 22
                ).frame(minHeight: 20, maxHeight: 100)
                ForEach(noteEditorViewModel.noteContents){ noteContent in
                        //                    renderWidget(content: noteContent)
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
                        setAlert(title: "Camera Permission Required")
                        return
                    }
                    guard micPermission.checkMicPermission() else {
                        setAlert(title: "Microphone Permission Required")
                        return
                    }
                    router.navigate(to: .Camera(){ data in
                        capturedData = data
                    })
                },
                onShare: { print("Share action") },
                onRecordMic: {
                    guard micPermission.checkMicPermission() else {
                        setAlert(title: "Microphone Permission Required")
                        return
                    }
                    showRecorder = true
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
    
    func addText() {
        
    }
        //    func renderWidget(content : NoteContentModel) -> some View {
        //        switch content.type {
        //            case .text: {
        //              return
        //                }
        //
        //            case .image : {
        //                let contentImage = content as! NoteContentModel.MediaContent
        //              let path = contentImage.getMediaUrl()
        //                return
        //                }
        //
        //            case .video: {
        //                    let contentVideo = content as! NoteContentModel.MediaContent
        ////                    VideoCard(contentVideo, onClick = onMediaPreview)
        //                return
        //                }
        //
        //            case .audio:  {
        //                    let contentAudio = content as! NoteContentModel.MediaContent
        //
        //                return
        //                }
        //
        //
        //            case .link : {
        //                    let contentLink = content as! NoteContentModel.Link
        //                return
        //
        //                }
        //
        //            case .docx :  {
        //                    let contentDoc = content as! NoteContentModel.MediaContent
        //                    let path = contentDoc.getMediaUrl()
        //                return
        //                }
        //
        //            case .location:  {
        //                    let locationContent = content as! NoteContentModel.Location
        //                return
        //                }
        //
        //            case .pdf :  {
        //                return
        //            }
        //        case .gif :  {
        //            return
        //        }
        //        }
        //    }
    
    
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
    
        // Alert
    func throwAlert() -> Alert {
        switch errorField.errorMessageTitle {
            case "Warning deleting note confirmation":
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
            case "Camera Permission Required": return cameraPermission.showAlert { resetAlert() }
                
            case "Microphone Permission Required": return micPermission.showAlert { resetAlert() }
                
            case "Location Permission Required": return cameraPermission.showAlert { resetAlert() }
                
            default:
                return Alert(
                    title: Text("\(errorField.errorMessageTitle)").font(.headline.weight(.heavy)).foregroundColor(.red),
                    message: Text(errorField.errorMessage),
                    dismissButton: Alert.Button.default(Text("Cancel"), action: { resetAlert() }))
                
        }
        
    }
    
    private func setAlert(message: String = "", title: String = "Error") {
        errorField.errorMessage = message
        errorField.showErrorAlert = true
        errorField.errorMessageTitle = title
    }
    private func resetAlert() {
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
