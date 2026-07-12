import SwiftUI
import shared


struct NotesView: View {
    @StateObject private var notesViewModel = NotesViewModel()
    
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
                    Text("Quick note")
                        .font(.system(size: 16, weight:.semibold))
                        .foregroundStyle(.white)
                        .padding(.trailing, 12)
                }.background(Theme.Colors.secondary)
            }
            .cornerRadius(24)
            .shadow(color: Color.black.opacity(0.3), radius: 5, x: 0, y: 5)
            .frame(alignment:.bottomTrailing)
        }
    }.padding(12)
    // Padding to keep the button away from screen edges
    }
    //List of notes
    var staggeredGrid : some View {
        StaggeredGrid(columns: 2, items: notesViewModel.state.notes, spacing: 12 ) {
            note in NoteBookView(note: note){
                router.navigate(to: .NoteEditor(note: note))
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
                if notesViewModel.state.notes.isEmpty { emptyNotesUI
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
            }
        // ZStack
        .sheet(isPresented: $notesViewModel.state.showSheet){
            createTag
        }
        .padding(8)
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
