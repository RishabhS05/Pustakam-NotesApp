import SwiftUI
import shared

class NotesHandler : IBaseHandler, ObservableObject {
    var base: BaseRepository = BaseRepositoryHelper.shared.baseRepository
    @State var page: Int = 1
    @State var notes = [Note]()
    func clear(){
        self.page = 1
    }
    func getNotesCall() {
      
        Task {
 
            print(
                "user Id \(String(describing: try? await base.getUserFromPrefId()))"
            )
            let response = await apiHandler(
                apiCall: {
                    try await base.getNotesForUser(
                        userId: try await base.getUserFromPrefId() ?? "",
                        page: Int32(page)
                    )
                })
            print("Response \(response)")
            if response.isSuccessful {
                let data =  response.data as! Notes
                data.notes?.forEach{note in
                    notes.append(note as! Note)
                }
            } else if response.error != nil {
                print("Error \(response.error!)")
            }
        }
    }
}
struct NotesView: View {
    private var notesHandler = NotesHandler()
    private let columns  = [GridItem(.flexible()), GridItem(.flexible())]
    var body: some View {
        ScrollView {
                   LazyVGrid(columns: columns , spacing: 20) {
                       ForEach(notesHandler.$notes, id: \.self) { note in
                           Text( "\(note.title)")
                       }
                   }
                   .padding(.horizontal)
               }
            .navigationBarBackButtonHidden().padding(20)
            .onAppear{
                 notesHandler.getNotesCall()
            }.onDisappear{
                
            }
    }
}

#Preview {
    NotesView()
}
