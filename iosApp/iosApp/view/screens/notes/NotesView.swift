import SwiftUI
import shared


class NotesViewModel : BaseViewModel, ObservableObject {
    
    @Published var page: Int = 1
    @Published var notes = [Note]()
    @Published var isLoading : Bool = false
    
    
    override init(){
        super.init()
            NoteRepositoryHelper().noteListStateHelper{ [weak self] state in
                DispatchQueue.main.async {
                    self?.notes = state.notes as! [Note]
                }
            }
        getNotesCall()
    }
    func clear(){
        self.notes.removeAll()
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
                if response.isSuccessful {
                    let data =  response.data as? Notes
                    data?.notes
                        .forEach{note in
                            print(note)
                            self.notes.append(note as! Note)
                        }
                }
                else if response.error != nil { print("Error \(response.error!)") }
            }
        }
    }
}
struct NotesView: View {
    @StateObject private var notesHandler = NotesViewModel()
    @Environment(Router.self) var router: Router

    var loadingUi : some View {
        LoadingUI().frame(alignment: .center)
        return Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
    }
    //Quick Action button
    var fab : some View {
    VStack{
        Spacer()
        HStack{
            Spacer()
            Button(action: {
                router.navigate(to: .NoteEditor(note: nil))
            }) {
                HStack {
                    Image(systemName: "pencil")
                        .foregroundColor(.white)
                        .font(.system(size: 24))
                        .frame(height: 48,alignment: .leading).padding(.leading,12)
                    Text("Quick note ").font(.system(size: 16, weight: .semibold)).foregroundStyle(.white).padding(.trailing, 12)
                }.background(.brown)
            }
            .cornerRadius(24)
            .shadow(color: Color.black.opacity(0.3), radius: 5, x: 0, y: 5)
            .frame(alignment:.bottomTrailing)
        }
    }
        // Padding to keep the button away from screen edges
    }
    //List of not
    var staggeredGrid : some View {
        StaggeredGrid(columns: 2, items: notesHandler.notes, spacing: 12 ) {
            note in NoteBookView(note: note){
                router.navigate(to: .NoteEditor(note: note))
            }
        }
    }
    var body: some View {
        ZStack{
            staggeredGrid
            if notesHandler.isLoading {
                loadingUi
            }
         fab
        }// ZStack
        .navigationBarBackButtonHidden()
        .padding(8)
            .onAppear {
                notesHandler.getNotesCall()
            }.onDisappear{
                notesHandler.isLoading = false
            }
    }
}
