import SwiftUI
import shared

// 📖 01-Aug-2026: inside a NOTE a document is a compact card, not N inline sheets — tapping it
//   opens the dedicated document reader. This is what stops a 1443-page PDF exploding the note.
// 📖 15-Aug-2026: with InlineDocumentState.enabled the card also reads in place — Read opens the
//   first 10 sheets under this card, the arrow still opens the dedicated reader.
struct DocumentBlockView: View {
    let block: ReaderBlock.Document
    var documents: InlineDocumentState = .disabled
    var onOpen: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        let media = block.item
        let readsInline = documents.readsInline(media.id)
        let expanded = documents.isExpanded(media.id)
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(BookPalette.cover.opacity(0.12)).frame(width: 46, height: 46)
                Image(systemName: "doc.text.fill").foregroundColor(BookPalette.cover)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(media.title.isEmpty ? "File" : media.title)
                    .font(.subheadline).foregroundColor(BookPalette.ink).lineLimit(2)
                Text(media.type.name)
                    .font(.caption2).foregroundColor(BookPalette.ink.opacity(0.6))
            }
            Spacer(minLength: 0)
            if readsInline {
                if documents.isBusy(media.id) {
                    ProgressView().scaleEffect(0.7).tint(BookPalette.cover)
                } else {
                    Button {
                        documents.onToggle(media)
                    } label: {
                        HStack(spacing: 2) {
                            Text(expanded ? "Close" : "Read").font(.caption)
                            Image(systemName: expanded ? "chevron.up" : "chevron.down").font(.caption2)
                        }
                        .foregroundColor(BookPalette.cover)
                    }
                    .buttonStyle(.plain)
                }
            }
            if documents.enabled {
                Button {
                    onOpen(media)
                } label: {
                    Image(systemName: "arrow.up.forward.square")
                        .font(.caption).foregroundColor(BookPalette.cover.opacity(0.7))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("Open full document"))
            }
        }
        .padding(14)
        .background(BookPalette.cover.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .contentShape(Rectangle())
        .onTapGesture {
            if readsInline { documents.onToggle(media) } else { onOpen(media) }
        }
    }
}
