import SwiftUI
import shared

// 📖 01-Aug-2026: 2-column grid showing EVERY image, wrapping downward. Sizes come straight from the
//   policy — policy units are screen units — so no GeometryReader is needed. That measurement is
//   exactly what collapsed to zero height inside a ScrollView and blanked the page.
struct ImageGridBlockView: View {
    let block: ReaderBlock.ImageGrid
    let policy: PageLayoutPolicy
    var onTap: (NoteContentModel.MediaContent) -> Void = { _ in }

    private var columns: Int { Int(policy.gridColumns) }
    private var spacing: CGFloat { CGFloat(policy.gridSpacing) }

    var body: some View {
        if block.items.count == 1, let media = block.items.first {
            ImageGridCell(media: media, onTap: { onTap(media) })
                .frame(maxWidth: .infinity)
                .frame(height: CGFloat(BlockHeightEstimator.shared.singleImageHeight(media: media, policy: policy)))
        } else {
            VStack(spacing: spacing) {
                ForEach(Array(stride(from: 0, to: block.items.count, by: columns)), id: \.self) { start in
                    let end = min(start + columns, block.items.count)
                    HStack(spacing: spacing) {
                        ForEach(start..<end, id: \.self) { index in
                            ImageGridCell(media: block.items[index], onTap: { onTap(block.items[index]) })
                                .frame(maxWidth: .infinity)
                        }
                        // keeps a lone trailing cell at column width instead of stretching it
                        if end - start < columns { Color.clear.frame(maxWidth: .infinity) }
                    }
                    .frame(height: CGFloat(policy.imageCellHeight))
                }
            }
        }
    }
}
