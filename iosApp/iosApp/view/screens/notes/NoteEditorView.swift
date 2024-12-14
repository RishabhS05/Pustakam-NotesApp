import SwiftUI
import shared

class NoteEditorHandler: BaseHandler, ObservableObject {
    private func createNoteCall(noteRequest: NoteRequest) async -> BaseResult<BaseResponse<Note>?> {
        return await apiHandler(apiCall: {
            try await base.addNewNote(note: noteRequest)
        })
    }

    private func updateNoteCall(oldNote: Note, updatedNoteRequest: NoteRequest) async -> BaseResult<BaseResponse<Note>?> {
        return await apiHandler(apiCall: { try await base.updateNote(note: updatedNoteRequest) })
    }
        // making decision call update or create api
    func createOrUpdate(noteRequest: NoteRequest, note: Note? = nil) async -> BaseResult<BaseResponse<Note>?>? {
        guard noteRequest.title.isNotNilOrEmpty() && noteRequest.description_.isNotNilOrEmpty() else { return nil }
        if !noteRequest._id.isNotNilOrEmpty() {
            return await createNoteCall(noteRequest: noteRequest)
        } else {
            guard let note, FieldValidationKt.checkAnyUpdateOnNote(new: noteRequest, old: note) else { return nil }

            return await updateNoteCall(oldNote: note, updatedNoteRequest: noteRequest)
        }
    }

    func deleteNoteCall(noteId: String) async -> BaseResult<BaseResponse<DeleteDataModel>?> {
        return await apiHandler(apiCall: { try await base.deleteNote(noteId: noteId) })
    }
}
struct NoteEditorView: View {
    @EnvironmentObject var router: Router
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
            _noteContent = State(initialValue: note!.description_ ?? "")
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
                onSave: { callCreateOrUpdate(action: {}) },
                onSaveAs: { print("Save As action") },
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
                onDelete: {
                    setAlert(message: "Do you want to delete this note? This action cannot be undone.", title: "Warning deleting note confirmation")
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
                callCreateOrUpdate(action: {})
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

    private func callCreateOrUpdate(action: @escaping () -> Void) {
        let noteRequest = NoteRequest(title: self.title, description: self.noteContent, _id: note?._id ?? "")
        Task {
            isLoading = true
            let apiResponse = await noteEditorHandler.createOrUpdate(noteRequest: noteRequest, note: self.note)
            isLoading = false
            if apiResponse?.error != nil {
                errorField.errorMessage = (apiResponse?.error as! NetworkError).getError()
                errorField.showErrorAlert = apiResponse?.isSuccessful == false
            }
            action()
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
                            callDelete(noteId: note?._id)
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
