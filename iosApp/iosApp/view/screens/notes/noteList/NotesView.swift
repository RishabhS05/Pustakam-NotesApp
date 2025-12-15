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
        StaggeredGrid(columns: 2, items: notesHandler.notes, spacing: 12 ) {
            note in NoteBookView(note: note){
                router.navigate(to: .NoteEditor(note: note))
            }
        }
    }
    //List of tags
    var tagsUi : some View {
        ScrollView(.horizontal,showsIndicators: false){
            LazyHStack(alignment:.center,spacing: 8){
                ForEach(notesHandler.tags) { tag in
                    TagView(tag: tag){}
                }
                TagView (tag: Tag(label: "+Tag", color: Color.secondary.tohexColor())){
                    notesHandler.showSheet = true
                }
            }
        }.frame(height: 40)
    }
    
    var createTag : some View {
        VStack{
            HStack{
                Image(systemName: "paintbrush.fill")
                    .foregroundColor(notesHandler.color)
                    .onTapGesture {
                        notesHandler.showColorPalette = true
                    }
                TextField(
                    "Tag name" ,
                    text: $notesHandler.tagName
                ).submitLabel(.next)
                .padding(.horizontal,4)
            }.padding(6)
            .overlay(){
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Theme.Colors.secondary, lineWidth: 1)
                }.padding(12)
            if notesHandler.showColorPalette { colorSelector }
            Button("Done"){
                notesHandler.showSheet = false
                notesHandler.showColorPalette = false
                notesHandler.createTag()
            }.buttonStyle(SigninButtonStyle())
        }
    }
    var colorSelector : some View {
        StatefulPreviewWrapper(value: Color.blue) { binding in
//        ColorSelector(selectedColor: binding, showAlpha: true)
            HSVColorPicker(selectedColor: $notesHandler.color)
    }
    }
    var body: some View {
            ZStack(alignment: .center){
                if notesHandler.notes.isEmpty { emptyNotesUI
                }
                else {
                    ScrollView{
                        LazyVStack(alignment:.center,spacing: 0){
                            tagsUi
                            staggeredGrid
                        }
                    }
                }
                if notesHandler.isLoading {
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
        .sheet(isPresented: $notesHandler.showSheet){
            createTag
        }
        .padding(8)
            .onAppear {
                notesHandler.getNotesCall()
            }.onDisappear{
                notesHandler.isLoading = false
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
