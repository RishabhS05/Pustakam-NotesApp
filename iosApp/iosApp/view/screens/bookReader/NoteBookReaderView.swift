import SwiftUI
import shared

// 📖 01-Aug-2026: WHOLE-NOTE reader. Pages come from the SHARED PageLayoutEngine — the same engine
//   Android calls — so both platforms produce identical page boundaries. This file only renders.
struct NoteBookReaderView: View {
    let noteId: String
    var startContentId: String? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var adapter = NotesBridgeAdapter()
    @State private var pages: [ReaderPage] = []
    @State private var isLoading = true
    @State private var currentIndex = 0
    @State private var startIndex = 0
    @State private var readerPrefs = ReaderPrefsAdapter()
    @State private var readingMode: ReadingMode = .page
    /// the MediaContent whose reading progress this book represents
    @State private var progressContentId: String? = nil

    // the ONE policy pages are generated against; the views read it back so what they draw always
    // matches the heights the engine reserved
    private let policy = PageLayoutPolicy.companion.standard()

    var body: some View {
        ZStack {
            BookPalette.desk.ignoresSafeArea()
            if pages.isEmpty && isLoading {
                LoadingUI()
            } else if !pages.isEmpty {
                // 📖 both modes render the SAME pages — switching never rebuilds them
                switch readingMode {
                case .page:
                    ReaderPageCurlView(pages: pages, policy: policy, startIndex: startIndex) { index in
                        currentIndex = index
                        saveProgress()
                    }
                    .ignoresSafeArea(edges: .bottom)
                case .scroll:
                    ReaderScrollView(
                        pages: pages,
                        policy: policy,
                        startIndex: startIndex,
                        onPageChanged: { index in
                            currentIndex = index
                            saveProgress()
                        }
                    )
                    .ignoresSafeArea(edges: .bottom)
                }
                VStack {
                    Spacer()
                    Text("\(currentIndex + 1) / \(pages.count)")
                        .font(.caption).foregroundColor(BookPalette.paper)
                        .padding(.horizontal, 12).padding(.vertical, 4)
                        .background(Capsule().fill(Color.black.opacity(0.45)))
                        .padding(.bottom, 10)
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                BackButton(action: { dismiss() })
            }
            ToolbarItem(placement: .topBarTrailing) {
                if !pages.isEmpty {
                    Button {
                        let next = readingMode.toggled
                        readingMode = next
                        readerPrefs.setReadingMode(next)
                    } label: {
                        Image(systemName: readingMode.switchIcon)
                            .foregroundColor(BookPalette.paper)
                    }
                    .accessibilityLabel(Text(readingMode.switchLabel))
                }
            }
        }
        .onAppear {
            loadNote()
            readerPrefs.observeReadingMode { readingMode = $0 }
        }
        .onDisappear { saveProgress() }
    }

    private func saveProgress() {
        guard let contentId = progressContentId, !pages.isEmpty else { return }
        let page = min(max(currentIndex, 0), pages.count - 1)
        readerPrefs.saveProgress(contentId: contentId, page: page, totalPages: pages.count)
    }

    private func loadNote() {
        adapter.readNote(noteId: noteId) { result in
            switch result {
            case .loading: if pages.isEmpty { isLoading = true }
            case .success(let note):
                guard let note else { isLoading = false; return }
                let wantedId = startContentId
                let allContents = (note.contents as? [NoteContentModel]) ?? []
                // 🐛 23-Jul-2026: page building is heavy, so it stays off the main thread
                DispatchQueue.global(qos: .userInitiated).async { [self] in
                    let built = PageLayoutEngine.shared.buildPages(note: note, policy: policy)
                    // an explicit startContentId (tapped card) wins over the stored resume page
                    let jump = Int(PageLayoutEngine.shared.pageIndexOf(pages: built, contentId: wantedId))
                    let media = allContents
                        .compactMap { $0 as? NoteContentModel.MediaContent }
                        .first { media in built.contains { $0.contains(contentId: media.id) } }
                    let saved: Int? = {
                        guard let m = media, m.hasReadingProgress(), !built.isEmpty else { return nil }
                        return min(max(Int(m.progressPage), 0), built.count - 1)
                    }()
                    let resolvedStart = jump >= 0 ? jump : (saved ?? 0)

                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.pages = built
                        self.progressContentId = media?.id
                        self.startIndex = resolvedStart
                        self.currentIndex = resolvedStart
                        BookWidgetStore.saveLastBook(noteId: note.id, title: note.title ?? "Untitled note")
                    }
                }
            case .failure(let error):
                // 🔧 19-Jul-2026: a local-miss can arrive BEFORE the data; never flash it
                print("NoteBookReader read failed (suppressed for UX): \(error.message)")
            case .idle: break
            }
        }
    }
}
