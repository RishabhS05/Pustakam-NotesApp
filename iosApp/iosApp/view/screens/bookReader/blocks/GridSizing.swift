import SwiftUI
import shared

extension NoteContentModel.MediaContent: Identifiable {}

struct GridSizing {
    let columns: Int
    let spacing: CGFloat
    let width: CGFloat
    let aspect: CGFloat
    let minHeight: CGFloat
    let maxHeight: CGFloat

    init(policy: PageLayoutPolicy, aspect: CGFloat) {
        self.columns = max(1, Int(policy.gridColumns))
        self.spacing = CGFloat(policy.gridSpacing)
        // computed Kotlin vals do not bridge as Swift properties, so derive from stored ones
        self.width = max(1, CGFloat(policy.pageWidth - policy.marginStart - policy.marginEnd))
        self.aspect = aspect
        self.minHeight = CGFloat(policy.imageMinHeight)
        self.maxHeight = CGFloat(policy.imageMaxHeight)
    }

    var cellWidth: CGFloat {
        max(1, (width - spacing * CGFloat(columns - 1)) / CGFloat(columns))
    }

    var cellHeight: CGFloat { max(1, cellWidth * aspect) }

    /// full-width height for a lone item, aspect-aware and clamped
    func singleHeight(for media: NoteContentModel.MediaContent) -> CGFloat {
        let w = CGFloat(media.width)
        let h = CGFloat(media.height)
        guard w > 0, h > 0 else { return maxHeight }
        return min(max(width * (h / w), minHeight), maxHeight)
    }

    /// full-width height at a fixed ratio (video)
    var singleAspectHeight: CGFloat { max(1, width * aspect) }
}
