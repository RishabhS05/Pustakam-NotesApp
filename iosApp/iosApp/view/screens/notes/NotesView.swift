import SwiftUI
import shared

class NotesHandler : IBaseHandler, ObservableObject {
    var base: BaseRepository = KoinHelper().getBaseRepository()
    @Published var page: Int = 1
    @Published var notes = [Note]()
    func clear(){
        self.page = 1
    }
    func getNotesCall() {
        Task {
            let response = await apiHandler(
                apiCall: {
                    try await base.getNotesForUser(
                        page: Int32(page)
                    )
                })
            print("Response \(response)")
            if response.isSuccessful {
                let data =  response.data as! BaseResponse<Notes>
                DispatchQueue.main.async {
                    data.data?.notes?
                        .forEach{note in
                            print(note)
                            self.notes.append(note as! Note)
                        }
                }
            } else if response.error != nil {
                print("Error \(response.error!)")
            }
        }
    }
}
struct NotesView: View {
    @StateObject private var notesHandler = NotesHandler()
    private let columns  = [GridItem(.flexible()), GridItem(.flexible())]
    var body: some View {
        ScrollView {
                   LazyVGrid(columns: columns , spacing: 20) {
                       ForEach(notesHandler.notes, id: \.self) { note in
                           Text(note.title ?? "")
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
