// 🔧 19-Jul-2026: NEW FEATURE — spiral-notebook widget for files INSIDE the note editor
//   (inspired by the handbook-page reference). Renders the file's REAL pages inline using the
//   SAME BookPagesBuilder + BookPageCurlView + BookPageContentView as the full reader (DRY).
//   Bottom shows "<current>/<total>" (e.g. 2/20) instead of dots. DocumentFileCardView is KEPT
//   for reuse elsewhere — this widget replaces it only in the editor list.

import SwiftUI
import shared

// 🔧 19-Jul-2026: notebook palette — matches the reader's paper/leather look
private enum NotebookPalette {
    static let paper = Color(red: 0.98, green: 0.95, blue: 0.89)
    static let ink = Color(red: 0.24, green: 0.18, blue: 0.11)
    static let cover = Color(red: 0.29, green: 0.21, blue: 0.15)
    static let spiral = Color(red: 0.55, green: 0.52, blue: 0.47)
}

struct InlineBookFileView: View {
    let media: NoteContentModel.MediaContent
    var onOpenFull: () -> Void = {}
    var onDelete: () -> Void = {}
    var onSave: () -> Void = {}

    @State private var pages: [BookPageItem] = []
    @State private var building = true
    @State private var currentIndex = 0

    var body: some View {
        HStack(spacing: 0) {
            spiralBinding
            VStack(spacing: 0) {
                header
                Rectangle().fill(NotebookPalette.ink.opacity(0.15)).frame(height: 1)
                ZStack {
                    if building {
                        ProgressView().tint(NotebookPalette.cover)
                    } else if !pages.isEmpty {
                        // 🔧 19-Jul-2026: SAME page-curl + renderers as the full reader (DRY)
                        BookPageCurlView(pages: pages, startIndex: 0) { index in
                            currentIndex = index
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                // 🔧 19-Jul-2026: "<selected>/<total>" instead of dots — e.g. 2/20
                if !pages.isEmpty {
                    Text("\(currentIndex + 1)/\(pages.count)")
                        .font(.caption2.weight(.medium))
                        .foregroundColor(NotebookPalette.ink.opacity(0.65))
                        .padding(.vertical, 3)
                }
            }
            .background(NotebookPalette.paper)
            .clipShape(UnevenRoundedRectangle(topLeadingRadius: 0, bottomLeadingRadius: 0,
                                              bottomTrailingRadius: 12, topTrailingRadius: 12))
        }
        .frame(height: 380)
        .padding(6)
        .background(RoundedRectangle(cornerRadius: 16).fill(NotebookPalette.cover))
        .padding(.vertical, 6)
        .padding(.horizontal,12)
        .onAppear(perform: buildPages)
    }

    // header — file name; tap/expand opens the full book, long-press menu = actions
    private var header: some View {
        HStack(spacing: 8) {
            Image(systemName: iconNameForContentType(media.type))
                .font(.system(size: 13)).foregroundColor(NotebookPalette.cover)
            Text(media.title.isEmpty ? "File" : media.title)
                .font(.system(.caption, design: .serif).weight(.semibold))
                .foregroundColor(NotebookPalette.ink).lineLimit(1)
            Spacer()
            Button(action: onOpenFull) {
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.system(size: 11)).foregroundColor(NotebookPalette.cover)
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 6)
        .contentShape(Rectangle())
        .onTapGesture { onOpenFull() }
        .contextMenu {
            Button { onOpenFull() } label: { Label("Open full book", systemImage: "book") }
            Button { onSave() } label: { Label("Save to device", systemImage: "square.and.arrow.down") }
            Button(role: .destructive) { onDelete() } label: { Label("Delete", systemImage: "trash") }
        }
    }

    // 🔧 19-Jul-2026: binder rings column like the reference image
    private var spiralBinding: some View {
        VStack(spacing: 16) {
            ForEach(0..<9, id: \.self) { _ in
                Circle()
                    .strokeBorder(NotebookPalette.spiral, lineWidth: 2.5)
                    .frame(width: 13, height: 13)
            }
        }
        .frame(width: 26)
        .frame(maxHeight: .infinity)
    }

    // 🔧 19-Jul-2026: pages built by the SHARED builder, off the main thread
    private func buildPages() {
        guard pages.isEmpty else { return }
        building = true
        DispatchQueue.global(qos: .userInitiated).async {
            let built = BookPagesBuilder.buildForContent(media)
            DispatchQueue.main.async {
                pages = built
                building = false
            }
        }
    }
}
