
import SwiftUI
import shared


class  NoteEditorHandler : BaseHandler ,ObservableObject {
    private func createNoteCall(noteRequest : NoteRequest)  async ->  BaseResult<BaseResponse<Note>?> {
          return await apiHandler (apiCall: {
              try await base.addNewNote(note:noteRequest)})
    }
    
    private func updateNoteCall(oldNote :Note, updatedNoteRequest : NoteRequest) async -> BaseResult<BaseResponse<Note>?> {
        return await apiHandler (apiCall:{ try await base.updateNote(note: updatedNoteRequest) })
    }
    // making decision call update or create api
    func createOrUpdate(noteRequest : NoteRequest, note : Note? = nil ) async -> BaseResult<BaseResponse<Note>?>?{
        guard noteRequest.title.isNotNilOrEmpty()  && noteRequest.description_.isNotNilOrEmpty()  else { return nil }
        if !noteRequest._id.isNotNilOrEmpty() {
            return await createNoteCall(noteRequest: noteRequest)
        }else {
            guard note != nil && !FieldValidationKt.checkAnyUpdateOnNote(new: noteRequest,
                                                                    old: note!) else { return nil }
            return await updateNoteCall(oldNote: note!, updatedNoteRequest: noteRequest)
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
                RulledPage(lineColor: lineColor, marginColor: marginColor, fontSize: fontSize, lineSpacing: lineSpacing, leftpadding: leftpadding)
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
        }
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
