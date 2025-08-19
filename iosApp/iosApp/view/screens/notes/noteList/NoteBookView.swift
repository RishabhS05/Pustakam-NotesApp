import SwiftUI
import shared
struct NoteBookView : View {
    let note: Note
    let onClick: () -> Void
    // todo if the title is missing then add first Ima
    var body: some View {
        GeometryReader{ geo in
            let width = geo.size.width
            let height = geo.size.height
            ZStack{
                VStack{
                    Text(note.title ?? "No Title ?")
                        .font(.system(size: 18, weight: .bold))
                        .lineLimit(5)
                        .padding(12)
                }
                .frame(maxWidth:width , maxHeight: height, alignment: .topLeading).padding(.horizontal,20)
                    .padding(.vertical,35)
                HStack{
                    Spacer()
                    Text("\(note.updatedAt?.toLocalFormat(showTime: false) ?? "")")
                        .font(.system(size: 14, weight: .regular))
                        .background(.gray.gradient)
                        .cornerRadius(4)
                        .foregroundColor(Theme.Colors.onSurface)
                }
                .frame(width :width,
                        height : height,
                        alignment: .topTrailing)
            }
            .background(Theme.Colors.primary)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .onTapGesture {
                    onClick()
                }
        }
    }
}

#Preview {
    NoteBookView(
        note: Note.init(
            id: "12345",
            title: "Hello World      jghghghggmgngngngggnnggnnvgngngnngngngng",
            updates: [],
            updatedAt : "24/03/2025",
            createdAt: "24/03/2025",
            categoryId: "24/03/2025",
            isSynced: KotlinBoolean?.none,
            contents: []
        ),
             onClick: {
})
}
