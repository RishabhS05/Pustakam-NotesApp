
import SwiftUI
import shared

class NoteEditorHandler : BaseHandler ,ObservableObject {
    private func createNoteCall(noteRequest : NoteRequest)  async ->  BaseResult<BaseResponse<Note>?> {
          return await apiHandler (apiCall: {
              try await base.addNewNote(note:noteRequest)})
    }
    
    private func updateNoteCall(oldNote :Note, updatedNoteRequest : NoteRequest) async -> BaseResult<BaseResponse<Note>?> {
        return await apiHandler (apiCall:{ try await base.updateNote(note: updatedNoteRequest) })
    }
    // making decision call update or create api
    func createOrUpdate(noteRequest : NoteRequest, note : Note? = nil ) async -> BaseResult<BaseResponse<Note>?>?{
        guard noteRequest.title.isNotNilOrEmpty() && noteRequest.description_.isNotNilOrEmpty()  else { return nil }
        if !noteRequest._id.isNotNilOrEmpty() {
            return await createNoteCall(noteRequest: noteRequest)
        } else {
            guard let note else { return nil }
            
            if FieldValidationKt.checkAnyUpdateOnNote(new: noteRequest ,old: note) {
               print("equals")
                return nil
            }
            
            else {
                print(" not equals")
                return await updateNoteCall(oldNote: note, updatedNoteRequest: noteRequest)
            }
        }
    }
    
    func deleteNoteCall(noteId: String)  async -> BaseResult<BaseResponse<DeleteDataModel>?> {
   return await apiHandler (apiCall: { try await base.deleteNote(noteId: noteId) })
    }
}
struct NoteEditorView : View {
    @EnvironmentObject var router: Router
    @Environment(\.dismiss) private var dismiss
    private var noteEditorHandler = NoteEditorHandler()
    private var note: Note? = nil
    @State private var errorField : ErrorField = ErrorField()
    @State private var title: String = ""
    @State private var noteContent: String = ""
    @State private var isRulledEnabled: Bool = false
    @State private var isLoading : Bool = false
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
        let leftpadding : CGFloat = isRulledEnabled ? 100 : 0
        ZStack(alignment: .topLeading) {
                // Draw ruled lines
            if isRulledEnabled {
                RulledPage(lineColor: lineColor, marginColor:
                            marginColor, fontSize: fontSize,
                           lineSpacing: lineSpacing,
                           leftpadding: leftpadding)
                .background(Color.white)
                .ignoresSafeArea()
            }
            VStack(alignment: .leading){
                NoteTextEditor(text: $title,
                               placeholder: "Title : Keep your thoughts alive.",
                               fontSize: 22
                ).frame(minHeight: 20, maxHeight: 100)
                NoteTextEditor(text: $noteContent, placeholder: "Hi, whats in your mind take a quick note, before it get lost.",
                               fontSize: 18
                )
                
            }.frame(maxHeight: .infinity, alignment: .top)
            if isLoading {
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }
            OverlayEditorButtons(
                showDelete: note != nil,
                      onSave: {  callCreateOrUpdate(action: {}) },
                      onSaveAs: { print("Save As action") },
                onRecordVideo: { router.navigate(to: .Camera) },
                      onAddImage: { print("Add Image action") },
                      onShare: { print("Share action") },
                      onRecordMic: { print("Record Mic action") },
                onDelete: { setAlert(message: "Do you want to delete this note? This action cannot be undone.") },
                      onArrowButton: {}
            ).frame(alignment: .bottomTrailing)
                  .padding()
        }
        .alert(isPresented: $errorField.showErrorAlert, content: {
            return Alert(title: Text("").font(.headline.weight(.heavy)).foregroundColor(.red),
                         message: Text(errorField.errorMessage),
                         primaryButton: Alert.Button.default(Text("Cancel"), action: {
                resetAlert()
            }), secondaryButton: Alert.Button.default(Text("Confirm"), action: {
                callDelete(noteId: note?._id)
                resetAlert()
            }))
        })
        .padding(.horizontal,12)
        .navigationBarBackButtonHidden(true)
        .toolbar{
            ToolbarItem(placement: .topBarLeading){
             BackButton(action: {
                    dismiss()
             })
            }
        }.onDisappear(){
            callCreateOrUpdate(action: {})
        }
    }
    
    private func setAlert(message : String) {
        errorField.errorMessage = message
        errorField.showErrorAlert = true
    }
    private func resetAlert(){
        errorField.errorMessage = ""
        errorField.showErrorAlert = false
    }

    private func callDelete(noteId : String?){
        guard noteId.isNotNilOrEmpty() else {
            print("noteId is nil")
            return }
        Task {
            isLoading = true
          let apiResponse = await noteEditorHandler.deleteNoteCall(noteId: noteId!)
            isLoading = false
            if (apiResponse.error != nil ){
                errorField.errorMessage = (apiResponse.error as! NetworkError).getError()
                errorField.showErrorAlert = apiResponse.isSuccessful == false
            }
            if apiResponse.isSuccessful{
                dismiss()
            }
        }
    }
    private func callCreateOrUpdate(action : @escaping () -> Void) {
        let noteRequest = NoteRequest(title: self.title, description: self.noteContent, _id : note?._id ?? "" )
        Task{
            isLoading = true
            let apiResponse = await noteEditorHandler.createOrUpdate(noteRequest: noteRequest, note: self.note)
            isLoading = false
            if (apiResponse?.error != nil ){
                errorField.errorMessage = (apiResponse?.error as! NetworkError).getError()
                errorField.showErrorAlert = apiResponse?.isSuccessful == false
            }
            action()
        }
    }
}
struct NotebookStyleNoteView_Previews: PreviewProvider {
    static var previews: some View {
        NoteEditorView()
    }
}
