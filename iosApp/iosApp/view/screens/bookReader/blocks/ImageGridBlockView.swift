import SwiftUI
import shared

struct ImageGridBlockView: View {
    let block: ReaderBlock.ImageGrid
    let policy: PageLayoutPolicy
    var onTap: (NoteContentModel.MediaContent) -> Void = { _ in }

    private var sizing: GridSizing {
        GridSizing(policy: policy, aspect: CGFloat(policy.gridCellAspect))
    }

    var body: some View {
        if block.items.count <= 1 {
            if let media = block.items.first {
                ImageGridCell(media: media, overflow: 0, onTap: onTap)
                    .frame(maxWidth: .infinity)
                    .frame(height: sizing.singleHeight(for: media))
                    .clipped()
            }
        } else {
            GenericGrid(items: block.items, columns: sizing.columns, spacing: sizing.spacing) { media in
                ImageGridCell(media: media, overflow: 0, onTap: onTap)
                    .frame(height: sizing.cellHeight)
                    .contentShape(RoundedRectangle(cornerRadius: 6))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }
            .frame(maxWidth: .infinity)
        }
    }
}
