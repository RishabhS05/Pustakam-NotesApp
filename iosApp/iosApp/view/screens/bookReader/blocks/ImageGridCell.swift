import SwiftUI
import shared

// 📖 01-Aug-2026: one grid cell — reuses the existing CardImageEditor so image handling stays in
//   one place; the "+N" veil is drawn on the last visible cell only.
struct ImageGridCell: View {
    let media: NoteContentModel.MediaContent
    let overflow: Int
    var onTap: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        ZStack {
            CardImageEditor(content: media, actionClick: { onTap(media) })
            if overflow > 0 {
                Color.black.opacity(0.45)
                Text("+\(overflow)").font(.title2).foregroundColor(.white)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}
