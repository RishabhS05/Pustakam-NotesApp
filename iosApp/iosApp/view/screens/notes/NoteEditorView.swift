
import SwiftUI
import shared


class  NoteEditorHandler : ObservableObject {
    
}
struct NoteEditorView : View {
    @EnvironmentObject var router: Router
    @State private var title: String = ""
    @State private var rawNote: String = ""
    @State private var noteContent: String = ""
    @State private var isRulledEnabled: Bool = false
    let lineColor = Color.gray.opacity(0.5)
    let marginColor = Color.red
    let fontSize: CGFloat = 18
    let lineSpacing: CGFloat = 28
    var body: some View {
        let leftpadding : CGFloat = isRulledEnabled ? 100 : 0
        ZStack(alignment: .topLeading) {
                // Draw ruled lines
            if isRulledEnabled {
                Canvas { context, size in
                    let startX: CGFloat = 80
                    var y: CGFloat = lineSpacing
                    
                        // Draw horizontal lines across the page
                    while y < size.height {
                        context.stroke(
                            Path { path in
                                path.move(to: CGPoint(x: 0, y: y))
                                path.addLine(to: CGPoint(x: size.width, y: y))
                            },
                            with: .color(lineColor),
                            lineWidth: 1
                        )
                        y += lineSpacing
                    }
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: startX + 6, y: 0))
                            path.addLine(to: CGPoint(x: startX + 6, y: size.height))
                        },
                        with: .color(marginColor),
                        lineWidth: 2
                    )
                    
                        // Draw vertical margin line
                    context.stroke(
                        Path { path in
                            path.move(to: CGPoint(x: startX, y: 0))
                            path.addLine(to: CGPoint(x: startX, y: size.height))
                        },
                        with: .color(marginColor),
                        lineWidth: 2
                    )
                }
                .background(Color.white)
                .ignoresSafeArea()
            }
            if title.isEmpty && rawNote.isEmpty {
                VStack {
                    Text("Title : Keep your thoughts alive.\n Hi, whats in your mind take a quick note, before it get lost. ")
                        .frame(maxWidth:.infinity,maxHeight: 100)
                        .padding(.leading, leftpadding)
                        .lineSpacing(10)
                        .font(.system(size: fontSize, weight: .light))
                        .foregroundColor(.gray)

                    }
            }
            VStack{
                 TextEditor(text: $rawNote)
                    .frame(maxWidth: .infinity,
                           minHeight: 100,
                           maxHeight: .infinity)
                    .padding(.leading, leftpadding)
                    .accentColor(.brown)
                    .scrollContentBackground(.hidden)
                    .font(.system(size: fontSize))
                    .lineSpacing(lineSpacing-fontSize)
                    .foregroundColor(.black)

            }
        }.navigationBarBackButtonHidden(true)
            .padding(.horizontal,12)
    }
    
}
struct NotebookStyleNoteView_Previews: PreviewProvider {
    static var previews: some View {
        NoteEditorView()
    }
}
