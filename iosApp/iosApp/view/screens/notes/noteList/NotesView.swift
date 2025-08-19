import SwiftUI
import shared


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
                }.background(Theme.Colors.secondary)
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
            if notesHandler.notes.isEmpty { emptyNotesUI }
            else { staggeredGrid }
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
    var emptyNotesUI : some View {
        HStack{
            Image("emptyNotes").resizable()
                .frame(width: 400, height:400)
        }
    }
}
