import SwiftUI
import shared

class NoteEditorHandler: BaseViewModel, ObservableObject {
    private func createNoteCall(note: Note) async -> BaseResult<BaseResponse<Note>?> {
        return await apiHandler(apiCall: {
            try await noteRepositary.insertOrUpdateNote(note : note)
        })
    }
    func deleteNoteCall(noteId: String) async -> BaseResult<BaseResponse<DeleteDataModel>?> {
        return await apiHandler(apiCall: { try await noteRepositary.deleteNote(id: noteId) })
    }
}









struct NoteEditorView: View {
    @Environment(Router.self) var router: Router
    @Environment(\.dismiss) private var dismiss
    private var noteEditorHandler = NoteEditorHandler()
    @State private var showRecorder = false
    private var note: Note? = nil
    @State private var errorField: ErrorField = ErrorField()
    @State private var title: String = ""
    @State private var noteContent: String = ""
    @State private var isRulledEnabled: Bool = false
    @State private var isLoading: Bool = false
    private var cameraPermission = CameraPermission()
    private var micPermission = MicPermission()
    @State private var capturedData: CapturedMedia?

    let lineColor = Color.gray.opacity(0.5)
    let marginColor = Color.red
    let fontSize: CGFloat = 18
    let lineSpacing: CGFloat = 28

    init(note: Note? = nil) {
        self.note = note
        if note != nil {
            _title = State(initialValue: note!.title ?? "")
    
        }
    }
    var body: some View {
        let leftpadding: CGFloat = isRulledEnabled ? 100 : 0
        ZStack(alignment: .topLeading) {
                // Draw ruled lines
            if isRulledEnabled {
                RulledPage(
                    lineColor: lineColor,
                    marginColor:
                        marginColor, fontSize: fontSize,
                    lineSpacing: lineSpacing,
                    leftpadding: leftpadding
                )
                .background(Color.white)
                .ignoresSafeArea()
            }
            VStack(alignment: .leading) {
                NoteTextEditor(
                    text: $title,
                    placeholder: "Title : Keep your thoughts alive.",
                    fontSize: 22
                ).frame(minHeight: 20, maxHeight: 100)
                NoteTextEditor(
                    text: $noteContent, placeholder: "Hi, whats in your mind take a quick note, before it get lost.",
                    fontSize: 18
                )

            }.frame(maxHeight: .infinity, alignment: .top)
            if isLoading {
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }
            OverlayEditorButtons(
                showDelete: note != nil,
                onRecordVideo: {
                    guard cameraPermission.checkCameraPermission() else {
                        setAlert(title: "Camera Permission Required")
                        return
                    }
                    router.navigate(to: .Camera(){ data in
                        capturedData = data
                    })
                },
                onAddImage: { print("Photo from galaxy action") },
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
        }.sheet(isPresented: $showRecorder){
          AudioRecorderView()

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
            }.onDisappear {
//                callCreateOrUpdate(action: {})
            }
    }

        // handler call wrappers
    private func callDelete(noteId: String?) {
        guard noteId.isNotNilOrEmpty() else { return }
        Task {
            isLoading = true
            let apiResponse = await noteEditorHandler.deleteNoteCall(noteId: noteId!)
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
                            callDelete(noteId: note?.id)
                            resetAlert()
                        }))
            case "Camera Permission Required": return cameraPermission.showAlert { resetAlert() }

            case "Microphone Permission Required":
                return micPermission.showAlert { resetAlert() }

            case "Location Permission Required":
                return cameraPermission.showAlert { resetAlert() }

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
