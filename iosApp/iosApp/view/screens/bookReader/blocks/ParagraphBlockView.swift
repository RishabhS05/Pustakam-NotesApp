import SwiftUI
import shared

struct ParagraphBlockView: View {
    let block: ReaderBlock.Paragraph

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(block.text)
                .font(.system(.body, design: .serif))
                .foregroundColor(BookPalette.ink)
                .fixedSize(horizontal: false, vertical: true)
            // continuation marker only when the engine had to split this paragraph
            if block.chunkCount > 1 {
                Text("· \(block.chunkIndex) of \(block.chunkCount) ·")
                    .font(.caption2)
                    .foregroundColor(BookPalette.ink.opacity(0.5))
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
