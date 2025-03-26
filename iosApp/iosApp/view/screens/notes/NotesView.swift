import SwiftUI
import shared


class NotesViewModel : BaseViewModel, ObservableObject {
    
    @Published var page: Int = 1
    @Published var notes = [Note]()
    @Published var isLoading : Bool = false
    func clear(){
        self.page = 1
    }
    func getNotesCall() {
        Task {
            DispatchQueue.main.async { self.isLoading = true }
            let response = await apiHandler(
                apiCall: {
                    try await noteRepositary.getAllNotes(
                        page: Int32(page)
                    )
                })
            DispatchQueue.main.async {
                print("Response \(response)")
                self.isLoading = false
                if response.isSuccessful {
                    let data =  response.data as! BaseResponse<Notes>
                    data.data?.notes
                        .forEach{note in
                            print(note)
                            self.notes.append(note as! Note)
                        }
                }
                else if response.error != nil {
                    print("Error \(response.error!)")
                }
            }
        }
    }
}
struct NotesView: View {
    @StateObject private var notesViewModel = NotesViewModel()
    @Environment(Router.self) var router: Router
    var body: some View {
        ZStack{
            VStack {
                StaggeredGrid(columns: 2, items: notesViewModel.notes, spacing:8) {
                            note in NoteBookView(note: note){
                                router.navigate(to: .NoteEditor(note: note))
                            }
                        }
            }.padding(.trailing,12)
            .navigationBarBackButtonHidden().padding(8)
            .onAppear {
                notesViewModel.getNotesCall()
            }.onDisappear{
                notesViewModel.isLoading = false
            }
            if notesViewModel.isLoading {
                LoadingUI().frame(alignment: .center)
                Color.black.opacity(0.4).edgesIgnoringSafeArea(.all)
            }

            VStack {
                Spacer()
                HStack {
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
                    .padding(.horizontal,12).padding(.vertical,16) // Padding to keep the button away from screen edges
                }
            }
        }
    }
}

#Preview {
    NotesView()
}
