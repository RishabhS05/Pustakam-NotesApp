import SwiftUI
import shared

// 📖 01-Aug-2026: a grid is a LAYOUT, not a player. Every cell is the existing VideoCardPlayer,
//   which already gates on MediaManager.currentPlaying, so A.mp4 can never render into B.mp4's cell.
struct VideoGridBlockView: View {
    let block: ReaderBlock.VideoGrid
    let policy: PageLayoutPolicy

    private var visible: Int {
        Int(BlockHeightEstimator.shared.visibleCells(total: Int32(block.items.count), policy: policy))
    }
    private var columns: Int { Int(policy.gridColumns) }

    var body: some View {
        let shown = Array(block.items.prefix(visible))
        if visible == 1, let media = shown.first {
            VideoCardPlayer(content: media).frame(maxWidth: .infinity)
        } else {
            VStack(spacing: CGFloat(policy.gridSpacing)) {
                ForEach(Array(stride(from: 0, to: shown.count, by: columns)), id: \.self) { start in
                    let end = min(start + columns, shown.count)
                    HStack(spacing: CGFloat(policy.gridSpacing)) {
                        ForEach(start..<end, id: \.self) { index in
                            VideoCardPlayer(content: shown[index]).frame(maxWidth: .infinity)
                        }
                        if end - start < columns { Color.clear.frame(maxWidth: .infinity) }
                    }
                }
            }
        }
    }
}
