import SwiftUI
import shared

// 📖 01-Aug-2026: inside a NOTE a document is a compact card, not N inline sheets — tapping it
//   opens the dedicated document reader. This is what stops a 1443-page PDF exploding the note.
struct DocumentBlockView: View {
    let block: ReaderBlock.Document
    var onOpen: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(BookPalette.cover.opacity(0.12)).frame(width: 46, height: 46)
                Image(systemName: "doc.text.fill").foregroundColor(BookPalette.cover)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(block.item.title.isEmpty ? "File" : block.item.title)
                    .font(.subheadline).foregroundColor(BookPalette.ink).lineLimit(2)
                Text(block.item.type.name)
                    .font(.caption2).foregroundColor(BookPalette.ink.opacity(0.6))
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(BookPalette.cover.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .contentShape(Rectangle())
        .onTapGesture { onOpen(block.item) }
    }
}
