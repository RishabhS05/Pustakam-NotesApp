// 🔧 18-Jul-2026: NEW FEATURE (file import) — inline card for imported document/file content
//   (pdf/docx/epub/txt/md/other). Tap = open in the book reader; long-press = delete/save actions.
//   QuickLookPreview is also used by the book reader's file pages.

import SwiftUI
import QuickLook
import shared

// 🔧 18-Jul-2026: ContentType → SF Symbol for the card leading icon
func iconNameForContentType(_ type: ContentType) -> String {
    switch type {
    case .pdf: return "doc.richtext"
    case .docx: return "doc.text"
    case .epub: return "books.vertical"
    case .txt, .md: return "doc.plaintext"
    case .image: return "photo"
    case .gif: return "photo.stack"
    case .video: return "film"
    case .audio: return "waveform"
    default: return "doc"
    }
}

// 🔧 18-Jul-2026: bytes → human size (Android readableSize parity)
func readableFileSize(_ bytes: Int64) -> String {
    guard bytes > 0 else { return "" }
    if bytes < 1024 { return "\(bytes) B" }
    if bytes < 1024 * 1024 { return "\(bytes / 1024) KB" }
    if bytes < 1024 * 1024 * 1024 { return String(format: "%.1f MB", Double(bytes) / 1_048_576.0) }
    return String(format: "%.1f GB", Double(bytes) / 1_073_741_824.0)
}

struct DocumentFileCardView: View {
    let media: NoteContentModel.MediaContent
    var onOpen: () -> Void = {}
    var onDelete: () -> Void = {}
    var onSave: () -> Void = {}

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(Theme.Colors.secondary.opacity(0.15))
                    .frame(width: 48, height: 48)
                Image(systemName: iconNameForContentType(media.type))
                    .font(.system(size: 20))
                    .foregroundColor(Theme.Colors.secondary)
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(media.title.isEmpty ? (media.localPath as NSString?)?.lastPathComponent ?? "File" : media.title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                let subtitle = [media.type.name, readableFileSize(media.sizeBytes)]
                    .filter { !$0.isEmpty }.joined(separator: " · ")
                Text(subtitle).font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "book").foregroundColor(Theme.Colors.secondary.opacity(0.7))
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.gray.opacity(0.12)))
        .contentShape(Rectangle())
        .onTapGesture { onOpen() }
        // 🔧 18-Jul-2026: same long-press action pattern as the media cards
        .contextMenu {
            Button { onOpen() } label: { Label("Open in book", systemImage: "book") }
            Button { onSave() } label: { Label("Save to device", systemImage: "square.and.arrow.down") }
            Button(role: .destructive) { onDelete() } label: { Label("Delete", systemImage: "trash") }
        }
    }
}

// 🔧 18-Jul-2026: QuickLook wrapper — system-quality preview for DOCX/EPUB/PDF/anything
struct QuickLookPreview: UIViewControllerRepresentable {
    let fileURL: URL

    func makeUIViewController(context: Context) -> QLPreviewController {
        let controller = QLPreviewController()
        controller.dataSource = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: QLPreviewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(fileURL: fileURL) }

    final class Coordinator: NSObject, QLPreviewControllerDataSource {
        let fileURL: URL
        init(fileURL: URL) { self.fileURL = fileURL }
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }
        func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
            fileURL as NSURL
        }
    }
}
