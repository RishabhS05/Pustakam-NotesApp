// 🔧 18-Jul-2026: NEW FEATURE (book reader) — opens a note as a REAL book using
//   UIPageViewController's native .pageCurl (true paper-curl physics on drag).
//   Every content type becomes pages: text chunks, images, one page per PDF page,
//   audio/video players, DOCX/EPUB/other via QuickLook, links and locations.

import SwiftUI
import PDFKit
import AVKit
import WidgetKit
import shared

// MARK: - Page model (Android BookPage parity)

enum BookPageItem: Identifiable {
    case cover(title: String, subtitle: String)
    case text(body: String, chunk: Int, chunkCount: Int, sourceId: String?)
    case image(path: String, title: String, sourceId: String?)
    case pdf(path: String, pageIndex: Int, pageCount: Int, title: String, sourceId: String?)
    case media(NoteContentModel.MediaContent)
    case docFile(NoteContentModel.MediaContent)
    case link(url: String, sourceId: String?)
    case location(lat: Double, lon: Double, address: String?, sourceId: String?)

    var id: String {
        switch self {
        case .cover: return "cover"
        case .text(_, let chunk, _, let s): return "text-\(s ?? "")-\(chunk)"
        case .image(let p, _, let s): return "img-\(s ?? p)"
        case .pdf(let p, let i, _, _, let s): return "pdf-\(s ?? p)-\(i)"
        case .media(let m): return "media-\(m.id)"
        case .docFile(let m): return "doc-\(m.id)"
        case .link(let u, let s): return "link-\(s ?? u)"
        case .location(let la, let lo, _, let s): return "loc-\(s ?? "\(la),\(lo)")"
        }
    }

    var sourceContentId: String? {
        switch self {
        case .cover: return nil
        case .text(_, _, _, let s), .image(_, _, let s), .pdf(_, _, _, _, let s),
             .link(_, let s), .location(_, _, _, let s): return s
        case .media(let m), .docFile(let m): return m.id
        }
    }
}

// 🔧 18-Jul-2026: paper palette — a book stays paper-colored in any app theme
enum BookPalette {
    static let paper = Color(red: 0.98, green: 0.95, blue: 0.89)
    static let ink = Color(red: 0.24, green: 0.18, blue: 0.11)
    static let cover = Color(red: 0.36, green: 0.25, blue: 0.20)
    static let desk = Color(red: 0.14, green: 0.11, blue: 0.08)
}

// MARK: - Pages builder

enum BookPagesBuilder {
    private static let charsPerPage = 700
    private static let maxTextFileBytes = 2 * 1024 * 1024

    // 🔧 18-Jul-2026: container-safe path (stored absolute paths go stale across app updates)
    private static func resolved(_ path: String?) -> String? {
        guard let path, !path.isEmpty else { return nil }
        return LocalFilePathResolver_iosKt.resolveLocalFilePath(path: path) ?? path
    }

    static func build(note: Note) -> [BookPageItem] {
        var pages: [BookPageItem] = []
        let contents = (note.contents as? [NoteContentModel] ?? []).sorted { $0.position < $1.position }
        let title = (note.title?.isEmpty == false ? note.title! : "Untitled note")
        pages.append(.cover(title: title, subtitle: "\(contents.count) entries"))

        for content in contents {
            switch content {
            case let text as NoteContentModel.TextContent:
                pages.append(contentsOf: paginate(text.text, sourceId: text.id))

            case let media as NoteContentModel.MediaContent:
                switch media.type {
                case .image, .gif:
                    pages.append(.image(path: resolved(media.localPath) ?? media.url,
                                        title: media.title, sourceId: media.id))
                case .video, .audio:
                    pages.append(.media(media))
                case .pdf:
                    pages.append(contentsOf: pdfSheets(media))
                case .txt, .md:
                    pages.append(contentsOf: textFilePages(media))
                default:
                    pages.append(.docFile(media))   // docx / epub / other → QuickLook page
                }

            case let link as NoteContentModel.Link:
                pages.append(.link(url: link.url, sourceId: link.id))

            case let loc as NoteContentModel.Location:
                pages.append(.location(lat: loc.latitude, lon: loc.longitude,
                                       address: loc.address, sourceId: loc.id))
            default: break
            }
        }
        return pages
    }

    // 🔧 18-Jul-2026: word-boundary pagination (Android parity)
    private static func paginate(_ text: String, sourceId: String?) -> [BookPageItem] {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return [] }
        var chunks: [String] = []
        var remaining = Substring(text)
        while remaining.count > charsPerPage {
            let window = remaining.prefix(charsPerPage)
            let cutIndex = window.lastIndex(of: "\n").flatMap { idx -> Substring.Index? in
                window.distance(from: window.startIndex, to: idx) > charsPerPage / 2 ? idx : nil
            } ?? window.lastIndex(of: " ").flatMap { idx -> Substring.Index? in
                window.distance(from: window.startIndex, to: idx) > charsPerPage / 2 ? idx : nil
            } ?? window.endIndex
            chunks.append(String(remaining[remaining.startIndex..<cutIndex]))
            remaining = remaining[cutIndex...].drop { $0 == "\n" || $0 == " " }
        }
        if !remaining.isEmpty { chunks.append(String(remaining)) }
        return chunks.enumerated().map { .text(body: $1, chunk: $0 + 1, chunkCount: chunks.count, sourceId: sourceId) }
    }

    // 🔧 18-Jul-2026: one book sheet per PDF page (PDFKit); unreadable → QuickLook fallback page
    private static func pdfSheets(_ media: NoteContentModel.MediaContent) -> [BookPageItem] {
        guard let path = resolved(media.localPath),
              let document = PDFDocument(url: URL(fileURLWithPath: path)),
              document.pageCount > 0 else { return [.docFile(media)] }
        return (0..<document.pageCount).map {
            .pdf(path: path, pageIndex: $0, pageCount: document.pageCount, title: media.title, sourceId: media.id)
        }
    }

    // 🔧 18-Jul-2026: txt/md file → paginated text pages (md shown as plain text v1)
    private static func textFilePages(_ media: NoteContentModel.MediaContent) -> [BookPageItem] {
        guard let path = resolved(media.localPath),
              let data = FileManager.default.contents(atPath: path) else { return [.docFile(media)] }
        let capped = data.count > maxTextFileBytes ? data.prefix(maxTextFileBytes) : data[...]
        guard let text = String(data: Data(capped), encoding: .utf8) else { return [.docFile(media)] }
        let pages = paginate(text, sourceId: media.id)
        return pages.isEmpty ? [.docFile(media)] : pages
    }
}

// MARK: - Screen

struct BookReaderView: View {
    let noteId: String
    var startContentId: String? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var adapter = NotesBridgeAdapter()
    @State private var pages: [BookPageItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String? = nil
    @State private var currentIndex = 0
    @State private var startIndex = 0

    var body: some View {
        ZStack {
            BookPalette.desk.ignoresSafeArea()
            if isLoading {
                LoadingUI()
            } else if !pages.isEmpty {
                BookPageCurlView(pages: pages, startIndex: startIndex) { index in
                    currentIndex = index
                }
                .ignoresSafeArea(edges: .bottom)
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
        }
        .alert("Book", isPresented: .constant(errorMessage != nil)) {
            Button("OK") { errorMessage = nil; dismiss() }
        } message: { Text(errorMessage ?? "") }
        .onAppear { loadNote() }
    }

    private func loadNote() {
        adapter.readNote(noteId: noteId) { result in
            switch result {
            case .loading: isLoading = true
            case .success(let note):
                isLoading = false
                if let note {
                    pages = BookPagesBuilder.build(note: note)
                    startIndex = startContentId.flatMap { id in
                        pages.firstIndex { $0.sourceContentId == id }
                    } ?? 0
                    currentIndex = startIndex
                    // 🔧 18-Jul-2026: remember this book for the home-screen widget
                    BookWidgetStore.saveLastBook(noteId: note.id, title: note.title ?? "Untitled note")
                }
            case .failure(let error):
                isLoading = false
                errorMessage = error.message
            case .idle: break
            }
        }
    }
}

// MARK: - Native page-curl container

// 🔧 18-Jul-2026: UIPageViewController(.pageCurl) — the REAL paper-curl transition, wrapped for SwiftUI
struct BookPageCurlView: UIViewControllerRepresentable {
    let pages: [BookPageItem]
    let startIndex: Int
    let onPageChanged: (Int) -> Void

    func makeUIViewController(context: Context) -> UIPageViewController {
        let controller = UIPageViewController(transitionStyle: .pageCurl, navigationOrientation: .horizontal)
        controller.dataSource = context.coordinator
        controller.delegate = context.coordinator
        controller.view.backgroundColor = .clear
        let start = min(max(startIndex, 0), pages.count - 1)
        controller.setViewControllers([context.coordinator.pageController(at: start)],
                                      direction: .forward, animated: false)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIPageViewController, context: Context) {
        context.coordinator.pages = pages
    }

    func makeCoordinator() -> Coordinator { Coordinator(pages: pages, onPageChanged: onPageChanged) }

    // 🔧 18-Jul-2026: index travels on the hosting controller — no tag hacks
    final class BookPageHost: UIHostingController<BookPageContentView> {
        let index: Int
        init(index: Int, page: BookPageItem) {
            self.index = index
            super.init(rootView: BookPageContentView(page: page))
            view.backgroundColor = .clear
        }
        @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError("not used") }
    }

    final class Coordinator: NSObject, UIPageViewControllerDataSource, UIPageViewControllerDelegate {
        var pages: [BookPageItem]
        let onPageChanged: (Int) -> Void
        init(pages: [BookPageItem], onPageChanged: @escaping (Int) -> Void) {
            self.pages = pages
            self.onPageChanged = onPageChanged
        }

        func pageController(at index: Int) -> UIViewController {
            BookPageHost(index: index, page: pages[index])
        }

        func pageViewController(_ pvc: UIPageViewController, viewControllerBefore vc: UIViewController) -> UIViewController? {
            guard let host = vc as? BookPageHost, host.index > 0 else { return nil }
            return pageController(at: host.index - 1)
        }

        func pageViewController(_ pvc: UIPageViewController, viewControllerAfter vc: UIViewController) -> UIViewController? {
            guard let host = vc as? BookPageHost, host.index < pages.count - 1 else { return nil }
            return pageController(at: host.index + 1)
        }

        func pageViewController(_ pvc: UIPageViewController, didFinishAnimating finished: Bool,
                                previousViewControllers: [UIViewController], transitionCompleted completed: Bool) {
            guard completed, let host = pvc.viewControllers?.first as? BookPageHost else { return }
            onPageChanged(host.index)
        }
    }
}

// MARK: - Page faces

struct BookPageContentView: View {
    let page: BookPageItem
    @State private var showPreview = false

    var body: some View {
        ZStack {
            // paper sheet with a soft spine shadow on the left
            (isCover ? BookPalette.cover : BookPalette.paper)
            HStack(spacing: 0) {
                LinearGradient(colors: [Color.black.opacity(0.18), .clear],
                               startPoint: .leading, endPoint: .trailing)
                    .frame(width: 14)
                Spacer()
            }
            pageBody.padding(.horizontal, 22).padding(.vertical, 26)
        }
    }

    private var isCover: Bool { if case .cover = page { return true }; return false }

    @ViewBuilder
    private var pageBody: some View {
        switch page {
        case .cover(let title, let subtitle):
            VStack(spacing: 14) {
                Text(title).font(.system(.title, design: .serif).weight(.semibold))
                    .foregroundColor(BookPalette.paper).multilineTextAlignment(.center)
                Rectangle().fill(BookPalette.paper.opacity(0.6)).frame(width: 60, height: 2)
                Text(subtitle).font(.subheadline).foregroundColor(BookPalette.paper.opacity(0.8))
            }

        case .text(let body, let chunk, let chunkCount, _):
            VStack {
                ScrollView {
                    Text(body)
                        .font(.system(.body, design: .serif))
                        .lineSpacing(6)
                        .foregroundColor(BookPalette.ink)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                if chunkCount > 1 {
                    Text("· \(chunk) of \(chunkCount) ·").font(.caption2)
                        .foregroundColor(BookPalette.ink.opacity(0.5))
                }
            }

        case .image(let path, let title, _):
            VStack(spacing: 8) {
                if FileManager.default.fileExists(atPath: path), let ui = UIImage(contentsOfFile: path) {
                    Image(uiImage: ui).resizable().scaledToFit()
                } else {
                    AsyncImage(url: URL(string: path)) { img in img.resizable().scaledToFit() }
                        placeholder: { ProgressView() }
                }
                if !title.isEmpty {
                    Text(title).font(.caption.italic()).foregroundColor(BookPalette.ink.opacity(0.7)).lineLimit(1)
                }
            }

        case .pdf(let path, let pageIndex, let pageCount, let title, _):
            VStack(spacing: 6) {
                PdfSheetView(path: path, pageIndex: pageIndex)
                Text("\(title) — \(pageIndex + 1)/\(pageCount)")
                    .font(.caption2).foregroundColor(BookPalette.ink.opacity(0.6)).lineLimit(1)
            }

        case .media(let media):
            MediaBookPageView(media: media)

        case .docFile(let media):
            VStack(spacing: 12) {
                ZStack {
                    Circle().fill(BookPalette.cover.opacity(0.12)).frame(width: 74, height: 74)
                    Image(systemName: iconNameForContentType(media.type))
                        .font(.system(size: 30)).foregroundColor(BookPalette.cover)
                }
                Text(media.title.isEmpty ? "File" : media.title)
                    .font(.subheadline.weight(.semibold)).foregroundColor(BookPalette.ink)
                    .multilineTextAlignment(.center)
                Text([media.type.name, readableFileSize(media.sizeBytes)].filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(.caption).foregroundColor(BookPalette.ink.opacity(0.6))
                Button { showPreview = true } label: {
                    Label("Open", systemImage: "arrow.up.forward.square")
                }.buttonStyle(.borderedProminent).tint(BookPalette.cover)
            }
            .sheet(isPresented: $showPreview) {
                if let path = LocalFilePathResolver_iosKt.resolveLocalFilePath(path: media.localPath) ?? media.localPath {
                    QuickLookPreview(fileURL: URL(fileURLWithPath: path))
                }
            }

        case .link(let url, _):
            VStack(spacing: 12) {
                Text("A link lives on this page").font(.headline).foregroundColor(BookPalette.ink)
                Text(url).font(.footnote).foregroundColor(.blue).multilineTextAlignment(.center)
                Button("Open link") {
                    if let u = URL(string: url) { UIApplication.shared.open(u) }
                }.buttonStyle(.borderedProminent).tint(BookPalette.cover)
            }

        case .location(let lat, let lon, let address, _):
            VStack(spacing: 10) {
                Image(systemName: "mappin.and.ellipse").font(.system(size: 36)).foregroundColor(BookPalette.cover)
                Text(address ?? String(format: "%.5f, %.5f", lat, lon))
                    .font(.subheadline).foregroundColor(BookPalette.ink).multilineTextAlignment(.center)
                Button("Open in Maps") {
                    if let u = URL(string: "http://maps.apple.com/?ll=\(lat),\(lon)&q=\(lat),\(lon)") {
                        UIApplication.shared.open(u)
                    }
                }.buttonStyle(.borderedProminent).tint(BookPalette.cover)
            }
        }
    }
}

// 🔧 18-Jul-2026: one PDF page rendered as an image via PDFKit thumbnails (crisp + cheap)
struct PdfSheetView: View {
    let path: String
    let pageIndex: Int
    @State private var image: UIImage? = nil

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFit()
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .onAppear {
            guard image == nil else { return }
            DispatchQueue.global(qos: .userInitiated).async {
                guard let doc = PDFDocument(url: URL(fileURLWithPath: path)),
                      let pdfPage = doc.page(at: pageIndex) else { return }
                let bounds = pdfPage.bounds(for: .mediaBox)
                let scale = min(2.0, 1600.0 / max(bounds.width, 1))
                let rendered = pdfPage.thumbnail(of: CGSize(width: bounds.width * scale,
                                                            height: bounds.height * scale), for: .mediaBox)
                DispatchQueue.main.async { image = rendered }
            }
        }
    }
}

// 🔧 18-Jul-2026: audio/video page — self-contained AVPlayer; paused when the page disappears
struct MediaBookPageView: View {
    let media: NoteContentModel.MediaContent
    @State private var player: AVPlayer? = nil

    private var sourceURL: URL? {
        if let local = LocalFilePathResolver_iosKt.resolveLocalFilePath(path: media.localPath) ?? media.localPath,
           !local.isEmpty, FileManager.default.fileExists(atPath: local) {
            return URL(fileURLWithPath: local)
        }
        return URL(string: media.url)
    }

    var body: some View {
        VStack(spacing: 10) {
            Text(media.title.isEmpty ? media.type.name : media.title)
                .font(.subheadline.weight(.semibold)).foregroundColor(BookPalette.ink).lineLimit(1)
            if media.type == ContentType.audio {   // 🔧 explicit enum — matches codebase style
                Image(systemName: "waveform.circle.fill")
                    .font(.system(size: 64)).foregroundColor(BookPalette.cover)
            }
            if let player {
                VideoPlayer(player: player)
                    .frame(maxWidth: .infinity, maxHeight: media.type == ContentType.audio ? 80 : .infinity)
                    .cornerRadius(8)
            } else {
                Text("Media not available").font(.caption).foregroundColor(BookPalette.ink.opacity(0.6))
            }
        }
        .onAppear { if player == nil, let url = sourceURL { player = AVPlayer(url: url) } }
        .onDisappear { player?.pause() }
    }
}

// MARK: - Widget bridge

// 🔧 18-Jul-2026: shared store the home-screen widget reads (App Group; falls back to standard)
enum BookWidgetStore {
    static let appGroupId = "group.com.app.pustakam"
    static let lastIdKey = "book.last.noteId"
    static let lastTitleKey = "book.last.title"

    static func saveLastBook(noteId: String, title: String) {
        let defaults = UserDefaults(suiteName: appGroupId) ?? .standard
        defaults.set(noteId, forKey: lastIdKey)
        defaults.set(title.isEmpty ? "Untitled note" : title, forKey: lastTitleKey)
        WidgetCenter.shared.reloadAllTimelines()   // refresh the book widget cover
    }
}
