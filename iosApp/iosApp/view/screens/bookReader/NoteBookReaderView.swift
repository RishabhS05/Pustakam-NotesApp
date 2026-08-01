import SwiftUI
import shared


struct NoteBookReaderView: View {
    let noteId: String
    var startContentId: String? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var adapter = NotesBridgeAdapter()
    @State private var pages: [BookPageItem] = []
    @State private var isLoading = true
    @State private var currentIndex = 0
    @State private var startIndex = 0
    @State private var readerPrefs = ReaderPrefsAdapter()
    @State private var readingMode: ReadingMode = .page
    /// the MediaContent whose reading progress this book represents
    @State private var progressContentId: String? = nil

    var body: some View {
        ZStack {
            BookPalette.desk.ignoresSafeArea()
            // 🔧 19-Jul-2026: FIX — loader ONLY while pages aren't built; never over loaded pages
            if pages.isEmpty && isLoading {
                LoadingUI()
            } else if !pages.isEmpty {
                switch readingMode {
                case .page:
                    BookPageCurlView(pages: pages, startIndex: startIndex) { index in
                        currentIndex = index
                        saveProgress()
                    }
                    .ignoresSafeArea(edges: .bottom)
                case .scroll:
            
                    BookScrollReader(pages: pages, startIndex: startIndex) { index in
                        currentIndex = index
                        saveProgress()
                    }
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
                DispatchQueue.global(qos: .userInitiated).async {
                    let built = BookPagesBuilder.build(note: note)
                  
                    let media = built.compactMap(\.sourceContentId).first.flatMap { firstId in
                        allContents.first { $0.id == firstId } as? NoteContentModel.MediaContent
                    }
                   
                    let jump = wantedId.flatMap { id in built.firstIndex { $0.sourceContentId == id } }
                    let saved: Int? = {
                        guard let m = media, m.hasReadingProgress(), !built.isEmpty else { return nil }
                        return min(max(Int(m.progressPage), 0), built.count - 1)
                    }()
                    let resolvedStart = jump ?? saved ?? 0

                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.pages = built
                        self.progressContentId = media?.id
                        self.startIndex = resolvedStart
                        self.currentIndex = resolvedStart
                        // 🔧 18-Jul-2026: remember this book for the home-screen widget
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
