import SwiftUI
import shared


struct DocumentPageBlockView: View {
    let block: ReaderBlock.DocumentPage
    var documents: InlineDocumentState = .disabled
    var zoomEnabled: Bool = true

    var body: some View {
        let ref = block.ref
        VStack(spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: "doc.text.fill")
                    .font(.caption2).foregroundColor(BookPalette.cover)
                Text(block.item.title.isEmpty ? "File" : block.item.title)
                    .font(.caption).foregroundColor(BookPalette.ink).lineLimit(1)
                Spacer(minLength: 0)
                Text("\(ref.pageNumber) of \(ref.pageCount)")
                    .font(.caption2).foregroundColor(BookPalette.ink.opacity(0.6))
                Image(systemName: "chevron.up")
                    .font(.caption2).foregroundColor(BookPalette.cover)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            .background(BookPalette.cover.opacity(0.07))
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .contentShape(Rectangle())
            .onTapGesture { documents.onToggle(block.item) }
            .accessibilityLabel(Text("Collapse document"))

            // 📖 pinch / double-tap the sheet — the page is a fixed full-screen box, so the
            //   zoom container has a real height to work in
            MaybeZoomable(enabled: zoomEnabled) { sheet(ref) }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            if ref.showsLoadMore {
                Button {
                    documents.onLoadMore(ref.contentId)
                } label: {
                    Text("Load more (\(ref.remaining) left)")
                        .font(.caption).foregroundColor(Theme.Colors.primary)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func sheet(_ ref: EmbeddedPageRef) -> some View {
        switch ref.kind {
        case .pdf:
            if let path = BookPagesBuilder.readablePdfPath(block.item.localPath) {
                PdfSheetView(path: path, pageIndex: Int(ref.pageIndex))
            } else {
                message("File not available offline")
            }

        case .text:
            Text(block.text ?? "")
                .font(.system(.body, design: .serif))
                .foregroundColor(BookPalette.ink)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)

        default:
            message("This file can only be opened outside the note")
        }
    }

    private func message(_ text: String) -> some View {
        Text(text)
            .font(.caption).foregroundColor(BookPalette.ink.opacity(0.6))
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
