import SwiftUI
import shared
struct NoteView : View {
    let note: Note
    let onClick: () -> Void
    var body: some View {
        VStack(alignment: .leading){
            Text(note.title ?? "")
                .font(.system(size: 17, weight: .bold))
                .padding(4)
            Text(note.description_ ?? "")
                .font(.system(size: 14, weight: .regular))
                .padding(4)

        }.frame( maxWidth: .infinity,alignment: .top)
        .background(RoundedRectangle(cornerRadius: 10)
            .stroke(Color.gray, lineWidth:1))
        .onTapGesture {
             onClick()
        }
    }
}
