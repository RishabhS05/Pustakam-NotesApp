import SwiftUI
import shared

// 📖 15-Aug-2026: a formatted paragraph on a reader page — drawn by the editor's own renderer,
//   re-inked for paper so it stays legible whatever theme the app is in.
//   Mirrors Android RichParagraphBlockView.
struct RichParagraphBlockView: View {
    let block: ReaderBlock.RichParagraph

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            SmartTextDisplay(block: block.block, palette: Self.readerPalette)
            // continuation marker only when the paragraph had to be cut across pages
            if block.chunkCount > 1 {
                Text("· \(block.chunkIndex) of \(block.chunkCount) ·")
                    .font(.caption2)
                    .foregroundColor(BookPalette.ink.opacity(0.5))
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private static let readerPalette: SmartTextPalette = {
        let base = SmartTextPalette.light
        return SmartTextPalette(
            page: BookPalette.paper,
            surface: BookPalette.paper,
            toolbar: base.toolbar,
            onSurface: BookPalette.ink,
            onSurfaceMuted: BookPalette.ink.opacity(0.6),
            accent: BookPalette.cover,
            onAccent: BookPalette.paper,
            accentSoft: base.accentSoft,
            highlight: base.highlight,
            divider: BookPalette.ink.opacity(0.2),
            codeBackground: base.codeBackground,
            searchHit: base.searchHit,
            searchHitActive: base.searchHitActive
        )
    }()
}
