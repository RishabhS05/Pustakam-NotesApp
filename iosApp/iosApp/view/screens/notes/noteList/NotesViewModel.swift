import shared
import Combine
import SwiftUI
enum NotesViewUIState {
   case showTagSheet
   case showColorSelector
   case none
}

class NotesViewModel : BaseViewModel, ObservableObject {
    
    @Published var page: Int = 1
    @Published var notes = [Note]()
    @Published var tags = [Tag]()
    @Published var isLoading : Bool = false
    @Published var showSheet = false
    @Published var showColorPalette = false
    @Published var color = Color.red
    @Published var tagName = ""
    
    override init(){
        super.init()
//        getNotesCall()
        NoteRepositoryHelper().noteListStateHelper{ [weak self] state in
                DispatchQueue.main.async {
                    print("NotesViewModel new List \(state.notes)\n count =  \(state.notes.count)")
                    self?.notes = state.notes as! [Note]
                }
            }
        NoteRepositoryHelper().getTagsHelper{  [weak self] state in
            DispatchQueue.main.async {
                self?.clear()
                self?.tags = state
            }
        }
    }

    func clear(){
        self.notes.removeAll()
    }
    
    func createTag(){
        guard !tagName.isEmpty else { return }
        let tag = Tag(label: tagName, color: color.tohexColor())
        Task {
            let respose  = await apiHandler(apiCall: {
                try await noteRepositary.createTagOnDB(tag: tag)
            })
            DispatchQueue.main.async {
                self.tagName = "" }
        }
    }
    
    func getNotesCall() {
        Task {
            DispatchQueue.main.async { self.isLoading = true }
            let response = await apiHandler( apiCall: {
                    try await noteRepositary.getAllNotes(
                        page: Int32(page)
                    )
                })
            
            DispatchQueue.main.async {
                self.isLoading = false
                 if response.error != nil { print("Error \(response.error!)") }
            }
        }
    }
}
