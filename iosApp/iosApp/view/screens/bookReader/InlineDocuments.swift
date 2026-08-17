import SwiftUI
import PDFKit
import shared


struct InlineDocumentState {
    var enabled: Bool = false
    var expandedIds: Set<String> = []
    var busyIds: Set<String> = []
    var unreadableIds: Set<String> = []
    var onToggle: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onLoadMore: (String) -> Void = { _ in }

    static let disabled = InlineDocumentState()

    func isExpanded(_ contentId: String) -> Bool { enabled && expandedIds.contains(contentId) }

    func isBusy(_ contentId: String) -> Bool { enabled && busyIds.contains(contentId) }

    /// False once a probe proved the file has no sheets we can draw — the card stays a card.
    func isReadable(_ contentId: String) -> Bool { !unreadableIds.contains(contentId) }

    func readsInline(_ contentId: String) -> Bool { enabled && isReadable(contentId) }
}

enum EmbeddedDocumentProbe {

    static func probe(_ media: NoteContentModel.MediaContent) -> EmbeddedDocumentSource {
        let unreadable = EmbeddedDocumentSource.companion.unsupported(contentId: media.id)
        switch media.type {
        case .pdf:
            guard let path = BookPagesBuilder.readablePdfPath(media.localPath),
                  let document = PDFDocument(url: URL(fileURLWithPath: path)),
                  document.pageCount > 0 else { return unreadable }
            return EmbeddedDocumentSource.companion.pdf(
                contentId: media.id, pageCount: Int32(document.pageCount)
            )

        case .txt, .md:
            guard let text = BookPagesBuilder.cappedText(media) else { return unreadable }
            let chunks = BookPagesBuilder.textChunks(text)
            guard !chunks.isEmpty else { return unreadable }
            return EmbeddedDocumentSource.companion.text(contentId: media.id, pages: chunks)

        default:
            return unreadable
        }
    }
}
