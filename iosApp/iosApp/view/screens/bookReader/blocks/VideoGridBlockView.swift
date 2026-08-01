import SwiftUI
import shared

let VIDEO_ASPECT : CGFloat = (16/9)
struct VideoGridBlockView: View {
    let block: ReaderBlock.VideoGrid
    let policy: PageLayoutPolicy

    private var sizing: GridSizing {
        GridSizing(policy: policy, aspect: VIDEO_ASPECT)
    }

    var body: some View {
        // no onTapGesture out here — it would swallow the tap the player needs to start playback
        if block.items.count <= 1 {
            if let media = block.items.first {
                VideoGridCell(
                    media: media,
                    overflow: 0,
                    width: sizing.width,
                    height: sizing.cellHeight,
                
                )
            }
        } else {
            GenericGrid(items: block.items, columns: sizing.columns, spacing: sizing.spacing) { media in
                VideoGridCell(
                    media: media,
                    overflow: 0,
                    width: sizing.cellWidth,
                    height: sizing.cellHeight,
                )
            }
            .frame(maxWidth: .infinity)
        }
    }
}
