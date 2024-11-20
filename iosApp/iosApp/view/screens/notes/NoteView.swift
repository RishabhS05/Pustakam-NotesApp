import SwiftUI
import shared
struct NoteView : View {
    let note: Note
    let onClick: () -> Void
    var body: some View {
        VStack(alignment: .leading){
            Text(note.title ?? "")
                .font(.system(size: 17, weight: .bold))
                .padding(8)
                .lineLimit(3)
            Text(note.description_ ?? "")
                .font(.system(size: 14, weight: .regular))
                .padding(4)
                .lineLimit(5)
        }.frame( maxWidth: .infinity,alignment: .top)
            .background(.orange.opacity(0.3))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        .onTapGesture {
             onClick()
        }
    }
}
