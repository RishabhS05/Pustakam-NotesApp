import SwiftUI
import shared

// 📖 01-Aug-2026: 2-column grid. Cell count and the "+N" overflow come from the SHARED estimator,
//   so what is drawn always matches the height the engine reserved for this block.
struct ImageGridBlockView: View {
    let block: ReaderBlock.ImageGrid
    let policy: PageLayoutPolicy
    var onTap: (NoteContentModel.MediaContent) -> Void = { _ in }

    private var visible: Int {
        Int(BlockHeightEstimator.shared.visibleCells(total: Int32(block.items.count), policy: policy))
    }
    private var overflow: Int {
        Int(BlockHeightEstimator.shared.overflowCount(total: Int32(block.items.count), policy: policy))
    }
    private var columns: Int { Int(policy.gridColumns) }

    var body: some View {
        let shown = Array(block.items.prefix(visible))
        if visible == 1, let media = shown.first {
            ImageGridCell(media: media, overflow: 0, onTap: onTap)
                .frame(maxWidth: .infinity, maxHeight: CGFloat(policy.imageMaxHeight))
        } else {
            VStack(spacing: CGFloat(policy.gridSpacing)) {
                ForEach(Array(stride(from: 0, to: shown.count, by: columns)), id: \.self) { start in
                    let end = min(start + columns, shown.count)
                    HStack(spacing: CGFloat(policy.gridSpacing)) {
                        ForEach(start..<end, id: \.self) { index in
                            ImageGridCell(
                                media: shown[index],
                                overflow: index == visible - 1 ? overflow : 0,
                                onTap: onTap
                            )
                            .aspectRatio(1 / CGFloat(policy.gridCellAspect), contentMode: .fit)
                        }
                        // keeps a lone trailing cell at column width instead of stretching it
                        if end - start < columns { Color.clear.frame(maxWidth: .infinity) }
                    }
                }
            }
        }
    }
}
