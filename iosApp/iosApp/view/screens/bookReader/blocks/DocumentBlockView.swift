import SwiftUI
import PDFKit
import shared

private let pagesPerBatch = 10

// 📖 01-Aug-2026: the document is READ INSIDE the note — file title, then its pages rendered inline
//   in batches of 10 with a Load more button. A 1000-page PDF costs 10 renders, not 1000.
struct DocumentBlockView: View {
    let block: ReaderBlock.Document
    let policy: PageLayoutPolicy

    @State private var pageCount = 0
    @State private var loadedPages = pagesPerBatch

    private var media: NoteContentModel.MediaContent { block.item }
    private var shown: Int { min(loadedPages, pageCount) }
    // 🐛 23-Jul-2026: PDFKit aborts the process on a non-PDF path, so this stays gated
    private var readablePath: String? { BookPagesBuilder.readablePdfPath(media.localPath) }

    var body: some View {
        VStack(spacing: 10) {
            Text(media.title.isEmpty ? "Document" : media.title)
                .font(.system(.title3, design: .serif))
                .foregroundColor(BookPalette.ink)
                .frame(maxWidth: .infinity)
            if pageCount > 0 {
                Text("\(pageCount) pages")
                    .font(.caption2).foregroundColor(BookPalette.ink.opacity(0.6))
            }
            ForEach(0..<shown, id: \.self) { index in
                DocumentPageImage(path: readablePath, index: index, policy: policy)
            }
            if shown < pageCount {
                Button("Load more · \(pageCount - shown) left") { loadedPages += pagesPerBatch }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(maxWidth: .infinity)
        .task(id: media.id) {
            guard pageCount == 0, let path = readablePath else { return }
            pageCount = PDFDocument(url: URL(fileURLWithPath: path))?.pageCount ?? 0
        }
    }
}
