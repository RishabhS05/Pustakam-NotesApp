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
    // 🔧 25-Jul-2026: NEW — share callback (ImageCardView-style actions on the document card)
    var onShare: () -> Void = {}

    @State private var pages: [BookPageItem] = []
    @State private var building = true
    @State private var currentIndex = 0
    // 📖 25-Jul-2026: the page to open the inline preview at — the LAST page the reader left off on
    //   (media.progressPage), so the card shows e.g. 847/1443 instead of always 1/1443.
    @State private var startIndex = 0
    // 🔧 25-Jul-2026: long-press actions overlay (delete / share / save), ImageCardView pattern
    @State private var showActions = false

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
                        // 📖 25-Jul-2026: open at the saved reading page, not always page 1
                        BookPageCurlView(pages: pages, startIndex: startIndex) { index in
                            currentIndex = index
                        }
                    }
                    // 🔧 25-Jul-2026: ImageCardView-style long-press actions (delete / share / save).
                    //   Overlaid on the preview so it mirrors the image card without touching ImageCard.
                    if showActions && !pages.isEmpty { actionsOverlay }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
                .onLongPressGesture {
                    withAnimation(.easeInOut(duration: 0.25)) { showActions = true }
                    // auto-hide after 2.5s — SAME timing as ImageCardView
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation(.easeInOut(duration: 0.5)) { showActions = false }
                    }
                }
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
        // 🔧 20-Jul-2026: size to the device — ~42% of screen height, clamped to a sane range
        .frame(height: min(max(UIScreen.main.bounds.height * 0.42, 260), 400))
        .padding(6)
        .background(RoundedRectangle(cornerRadius: 16).fill(NotebookPalette.cover))
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

    // 🔧 25-Jul-2026: bottom actions bar — delete / share / save. SAME icons/layout/behavior as
    //   CardImageEditor's overlay (mirrored, not shared — ImageCardView is left untouched).
    private var actionsOverlay: some View {
        VStack {
            Spacer()
            ZStack(alignment: .bottom) {
                LinearGradient(
                    colors: [.clear, .black.opacity(0.05), .black.opacity(0.35), .black.opacity(0.55)],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(height: 90)
                HStack(spacing: 40) {
                    Button { onDelete() } label: {
                        Image(systemName: "trash.fill").font(.title2).foregroundColor(.red)
                    }
                    Button { onSave() } label: {   // download to device
                        Image(systemName: "square.and.arrow.down.fill").font(.title2).foregroundColor(.white)
                    }
                    Button { onShare() } label: {  // share sheet
                        Image(systemName: "square.and.arrow.up.fill").font(.title2).foregroundColor(.white)
                    }
                }
                .padding(.bottom, 16)
            }
            .frame(height: 70)
            .transition(.opacity)
        }
    }

    // 🔧 19-Jul-2026: pages built by the SHARED builder, off the main thread
    private func buildPages() {
        guard pages.isEmpty else { return }
        building = true
        DispatchQueue.global(qos: .userInitiated).async {
            let built = BookPagesBuilder.buildForContent(media)
            // 📖 25-Jul-2026: resume the inline preview at the last-read page (clamped to the count)
            let resume = media.hasReadingProgress()
                ? min(max(Int(media.progressPage), 0), max(built.count - 1, 0)) : 0
            DispatchQueue.main.async {
                pages = built
                startIndex = resume
                currentIndex = resume
                building = false
            }
        }
    }
}
