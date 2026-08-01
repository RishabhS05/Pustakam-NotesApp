import SwiftUI
import shared

// 📖 01-Aug-2026: a grid is a LAYOUT, not a player. Every cell is the existing VideoCardPlayer,
//   which gates on MediaManager.currentPlaying, so A.mp4 can never render into B.mp4's cell.
//   Explicit frames stop a card overlapping its neighbour.
struct VideoGridBlockView: View {
    let block: ReaderBlock.VideoGrid
    let policy: PageLayoutPolicy
    var onTap: (NoteContentModel.MediaContent) -> Void = { _ in }

    private var columns: Int { Int(policy.gridColumns) }
    private var spacing: CGFloat { CGFloat(policy.gridSpacing) }

    var body: some View {
        if block.items.count == 1, let media = block.items.first {
            VideoCardPlayer(content: media)
                .frame(maxWidth: .infinity)
                .frame(height: CGFloat(policy.usableWidth * PageLayoutPolicy.companion.videoAspect()))
                .clipped()
        } else {
            VStack(spacing: spacing) {
                ForEach(Array(stride(from: 0, to: block.items.count, by: columns)), id: \.self) { start in
                    let end = min(start + columns, block.items.count)
                    HStack(spacing: spacing) {
                        ForEach(start..<end, id: \.self) { index in
                            VideoCardPlayer(content: block.items[index])
                                .frame(maxWidth: .infinity)
                                .clipped()
                        }
                        if end - start < columns { Color.clear.frame(maxWidth: .infinity) }
                    }
                    .frame(height: CGFloat(policy.videoCellHeight))
                }
            }
        }
    }
}
