import SwiftUI
import shared

struct VideoGridCell: View {
    let media: NoteContentModel.MediaContent
    let overflow: Int
    let width: CGFloat
    let height: CGFloat


    var body: some View {
        ZStack {
            Color.black
            // the card now takes the cell's size; its own onTapGesture starts playback
            VideoCardPlayer(
                content: media,
                cardWidth: width,
                cardHeight: height,
                cardPadding: 0,
            )
            if overflow > 0 {
                Color.black
                    .contentShape(Rectangle())
                Text("+\(overflow)").font(.title2).foregroundColor(.white)
            }
        }
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
