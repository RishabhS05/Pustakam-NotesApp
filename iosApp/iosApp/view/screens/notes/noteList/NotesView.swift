import SwiftUI
import shared


struct NotesView: View {
    @StateObject private var notesViewModel = NotesViewModel()
    
    @Environment(Router.self) private var router: Router
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
                router.navigate(to: .NoteEditor(noteId: nil))
            }) {
                HStack {
                    Image(systemName: "pencil")
                        .foregroundColor(.white)
                        .font(.system(size: 24))
                        .frame(height: 48,alignment: .leading).padding(.leading,12)
                    Text("Quick note")
                        .font(.system(size: 16, weight:.semibold))
                        .foregroundStyle(.white)
                        .padding(.trailing, 12)
                }
                // 🎨 20-Jul-2026 — FAB carries the signature saffron→copper accent gradient (spec §2.1 / §5)
                .background(Theme.Colors.accentGradient)
            }
            .cornerRadius(24)
            // 🎨 20-Jul-2026 — spec §4 shadow-fab (warm copper glow)
            .shadow(color: Theme.Elevation.fab, radius: Theme.Elevation.fabRadius, x: 0, y: Theme.Elevation.fabY)
            .frame(alignment:.bottomTrailing)
        }
    }.padding(12)
    // Padding to keep the button away from screen edges
    }
    //List of notes
    // 🔧 15-Jul-2026 iOS parity (summary query + paging): the grid renders NoteSummary cards —
    //   contents never load for the list. Navigation passes a contents-less stub; the editor
    //   re-reads the full note by id. The last card's onAppear pulls the next page.
    var staggeredGrid : some View {
        StaggeredGrid(columns: 2, items: notesViewModel.state.summaries, spacing: 12 ) {
            summary in NoteBookView(summary: summary){
                router.navigate(to: .NoteEditor(noteId: summary.id))
            }
            .onAppear {
                if summary.id == notesViewModel.state.summaries.last?.id {
                    notesViewModel.loadNextPage()
                }
            }
        }
    }
    //List of tags
    var tagsUi : some View {
        ScrollView(.horizontal,showsIndicators: false){
            LazyHStack(alignment:.center,spacing: 8){
                ForEach(notesViewModel.state.tags) { tag in
                    TagView(tag: tag){}
                }
                TagView (tag: Tag(label: "+Tag", color: Color.secondary.tohexColor())){
                    notesViewModel.state.showSheet = true
                }
            }
        }.frame(height: 40)
    }
    
    var createTag : some View {
        VStack{
            HStack{
                Image(systemName: "paintbrush.fill")
                    .foregroundColor(notesViewModel.state.color)
                    .onTapGesture {
                        notesViewModel.state.showColorPalette = true
                    }
                TextField(
                    "Tag name" ,
                    text: $notesViewModel.state.tagName
                ).submitLabel(.next)
                .padding(.horizontal,4)
            }.padding(6)
            .overlay(){
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Theme.Colors.secondary, lineWidth: 1)
                }.padding(12)
            if notesViewModel.state.showColorPalette { colorSelector }
            Button("Done"){
                notesViewModel.state.showSheet = false
                notesViewModel.state.showColorPalette = false
                notesViewModel.createTag()
            }.buttonStyle(SigninButtonStyle())
        }
    }
    var colorSelector : some View {
        HSVColorPicker(selectedColor: $notesViewModel.state.color)
    }
    var body: some View {
            ZStack(alignment: .center){
                // 🎨 20-Jul-2026 — warm parchment page background (spec §2.2 `bg`) so the notes list is not
                //   a stark white page behind the cards. Sits under everything, ignores safe area.
                if notesViewModel.state.summaries.isEmpty { emptyNotesUI
                }
                else {
                    ScrollView{
                        LazyVStack(alignment:.center,spacing: 0){
                            tagsUi
                            staggeredGrid
                        }
                    }
                }
                if notesViewModel.state.isLoading {
                    loadingUi
                }
                fab
            }
            .toolbar{
                ToolbarItem(placement: .topBarLeading){
                    HStack{
                        Text("Notes")
                    }
                }
                // 🔧 15-Jul-2026 iOS parity (FTS search): entry point to the search screen
                ToolbarItem(placement: .topBarTrailing){
                    Button {
                        router.navigate(to: .Search)
                    } label: {
                        Image(systemName: "magnifyingglass")
                    }
                }
            }
           
        // ZStack
        .sheet(isPresented: $notesViewModel.state.showSheet){
            createTag
        }
        .padding(.vertical,8)
            .onAppear {
                notesViewModel.getNotesCall()
            }.onDisappear{
                notesViewModel.state.isLoading = false
            }
            .navigationBarBackButtonHidden(true)
    }
    var emptyNotesUI : some View {
        HStack{
            Image("emptyNotes").resizable()
                .frame(width: 300, height:300)
        }
    }
}
