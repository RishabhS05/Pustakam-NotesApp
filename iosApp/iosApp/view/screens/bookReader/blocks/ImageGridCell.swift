import SwiftUI
import shared

// 📖 01-Aug-2026: one grid cell — fills its slot and crops, so mixed aspect ratios stay aligned.
struct ImageGridCell: View {
    let media: NoteContentModel.MediaContent
    var onTap: () -> Void = {}

    var body: some View {
        Group {
            if let image = UIImage(contentsOfFile: media.getMediaUrl()) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                AsyncImage(url: URL(string: media.getMediaUrl())) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.black.opacity(0.08)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
    }
}
