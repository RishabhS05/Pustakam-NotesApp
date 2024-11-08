
import SwiftUI
import shared


class  NoteEditorHandler : ObservableObject {
    
}
struct NoteEditorView : View {
    @EnvironmentObject var router: Router
    @State private var title: String = ""
    @State private var rawNote: String = ""
    @State private var noteContent: String = ""
    
    let lineColor = Color.gray.opacity(0.5)
    let marginColor = Color.red
    let fontSize: CGFloat = 18
    let lineSpacing: CGFloat = 28
    var body: some View {
        ZStack(alignment: .topLeading) {
                // Draw ruled lines
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
            TextEditor(text: $rawNote)
                .frame(maxWidth: .infinity,
                       maxHeight: .infinity)
                .padding(.leading, 100)
                .scrollContentBackground(.hidden)
                .font(.system(size: fontSize))
                .lineSpacing(lineSpacing-fontSize)
                .foregroundColor(.black)
                .background(Color.clear)
                                    
        }.navigationBarBackButtonHidden(true)
    }
    
}
struct NotebookStyleNoteView_Previews: PreviewProvider {
    static var previews: some View {
        NoteEditorView()
    }
}
