import SwiftUI
import shared

// 📖 01-Aug-2026: block -> widget. Pure dispatch; it never decides WHAT goes on a page, only how a
//   block already placed by the shared engine draws itself. Mirrors Android ReaderBlockContent.
struct ReaderBlockView: View {
    let block: ReaderBlock
    let policy: PageLayoutPolicy
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenMedia: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        switch block {
        case let title as ReaderBlock.Title:
            TitleBlockView(block: title)
        case let paragraph as ReaderBlock.Paragraph:
            ParagraphBlockView(block: paragraph)
        case let images as ReaderBlock.ImageGrid:
            ImageGridBlockView(block: images, policy: policy, onTap: onOpenMedia)
        case let videos as ReaderBlock.VideoGrid:
            VideoGridBlockView(block: videos, policy: policy, onTap: onOpenMedia)
        case let audio as ReaderBlock.Audio:
            AudioBlockView(block: audio)
        case let document as ReaderBlock.Document:
            DocumentBlockView(block: document, policy: policy)
        case let link as ReaderBlock.Link:
            LinkBlockView(block: link)
        case let location as ReaderBlock.Location:
            LocationBlockView(block: location)
        default:
            EmptyView()
        }
    }
}
