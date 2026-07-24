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

// 📖 23-Jul-2026: NEW — reading mode. `page` keeps the native .pageCurl book feel; `scroll` lays the
//   same pages out as one continuous document (better for long PDFs/text). Mirrors Android
//   ReadingMode.kt — both platforms persist the same raw strings in the shared DataStore.
enum ReadingMode: String {
    case page, scroll

    var toggled: ReadingMode { self == .page ? .scroll : .page }

    // toolbar affordance shows what you'll GET, matching Android's toggle
    var switchIcon: String { self == .page ? "doc.plaintext" : "book" }
    var switchLabel: String { self == .page ? "Switch to scrolling" : "Switch to page curl" }

    static func from(_ raw: String?) -> ReadingMode { ReadingMode(rawValue: raw ?? "") ?? .page }
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

    // 🐛 23-Jul-2026 CRASH FIX (PDF): CoreGraphics aborted the process with
    //   `assert(lexer->buffer != NULL) failed in lex_grow_buffer` whenever PDFKit was handed a path
    //   that isn't a readable PDF. resolveLocalFilePath() NEVER returns nil — when the file is
    //   missing it returns the stale path unchanged — so `resolved()` alone is not proof the bytes
    //   exist. That happens constantly: a file still downloading/copying, a stale container path
    //   after an app update, or a just-added PDF whose write hasn't landed yet.
    //   This gates every PDFKit call on the file actually existing AND being non-empty AND starting
    //   with the %PDF- magic header, so a bad path degrades to the QuickLook page instead of
    //   crashing the app. CG's lexer assert is fatal — it cannot be caught, only avoided.
    static func readablePdfPath(_ path: String?) -> String? {
        guard let path = resolved(path), !path.isEmpty else { return nil }
        let fm = FileManager.default
        var isDirectory: ObjCBool = false
        guard fm.fileExists(atPath: path, isDirectory: &isDirectory), !isDirectory.boolValue,
              fm.isReadableFile(atPath: path) else { return nil }
        // a zero-byte or partially-written file is exactly what trips the lexer assert
        let attributes = try? fm.attributesOfItem(atPath: path)
        let size = (attributes?[.size] as? NSNumber)?.int64Value ?? 0
        guard size > 0 else { return nil }
        // cheap magic-header check — reads only the first bytes, never the whole document
        guard let handle = FileHandle(forReadingAtPath: path) else { return nil }
        defer { try? handle.close() }
        let header = handle.readData(ofLength: 5)
        guard header.count == 5, [UInt8](header) == Array("%PDF-".utf8) else { return nil }
        return path
    }

    static func build(note: Note) -> [BookPageItem] {
        var pages: [BookPageItem] = []
        let contents = (note.contents as? [NoteContentModel] ?? []).sorted { $0.position < $1.position }
        let title = (note.title?.isEmpty == false ? note.title! : "Untitled note")
        pages.append(.cover(title: title, subtitle: "\(contents.count) entries"))
        // 🔧 19-Jul-2026: DRY — whole-note book delegates to the single-content builder
        //   (Self. avoids shadowing by the local `pages` array)
        for content in contents { pages.append(contentsOf: Self.pages(for: content)) }
        return pages
    }

    // 🔧 19-Jul-2026: single-file book — ONLY the tapped document's pages (fix: opening the 2nd
    //   file no longer flips through the 1st file first). Reused by the inline editor widget too.
    static func buildForContent(_ content: NoteContentModel) -> [BookPageItem] { pages(for: content) }

    // 🔧 19-Jul-2026: ONE content → its pages; the shared core every builder calls (DRY)
    static func pages(for content: NoteContentModel) -> [BookPageItem] {
        switch content {
        case let text as NoteContentModel.TextContent:
            return paginate(text.text, sourceId: text.id)

        case let media as NoteContentModel.MediaContent:
            switch media.type {
            case .image, .gif:
                return [.image(path: resolved(media.localPath) ?? media.url,
                               title: media.title, sourceId: media.id)]
            case .video, .audio:
                return [.media(media)]
            case .pdf:
                return pdfSheets(media)
            case .txt, .md:
                return textFilePages(media)
            default:
                return [.docFile(media)]   // docx / epub / other → QuickLook page
            }

        case let link as NoteContentModel.Link:
            return [.link(url: link.url, sourceId: link.id)]

        case let loc as NoteContentModel.Location:
            return [.location(lat: loc.latitude, lon: loc.longitude,
                              address: loc.address, sourceId: loc.id)]
        default: return []
        }
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
        // 🐛 23-Jul-2026: readablePdfPath (not resolved) — a missing/partial file used to reach
        //   PDFKit here and abort the process in CoreGraphics' lexer.
        guard let path = readablePdfPath(media.localPath) else {
            // 🐛 23-Jul-2026: leaves a breadcrumb naming the file we refused to open
            CrashBreadcrumb.rejectedUnreadablePdf(path: media.localPath)
            return [.docFile(media)]
        }
        guard let document = PDFDocument(url: URL(fileURLWithPath: path)),
              document.pageCount > 0 else {
            CrashBreadcrumb.rejectedUnreadablePdf(path: path)
            return [.docFile(media)]
        }
        CrashBreadcrumb.openingDocument(contentId: media.id, path: path, pageCount: Int(document.pageCount))
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
    var singleContent: Bool = false   // 🔧 19-Jul-2026: open ONLY the tapped file as a book

    @Environment(\.dismiss) private var dismiss
    @State private var adapter = NotesBridgeAdapter()
    @State private var pages: [BookPageItem] = []
    @State private var isLoading = true
    @State private var currentIndex = 0
    @State private var startIndex = 0
    // 📖 23-Jul-2026: reading mode is a preference; the resume page lives on the document's own
    //   MediaContent row (progressPage/totalPages), so it travels with the content.
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
                // 📖 23-Jul-2026: page-curl or continuous scroll, same pages either way
                switch readingMode {
                case .page:
                    // 📖 the page is tracked in memory and written on exit — saving on every turn
                    //   would hammer the DB while reading
                    BookPageCurlView(pages: pages, startIndex: startIndex) { index in
                        currentIndex = index
                        // 📖 23-Jul-2026: save on EVERY page turn, not just on exit. onDisappear is
                        //   not guaranteed on a navigation pop and never runs if the app is killed,
                        //   which is why the count kept coming back stale.
                        saveProgress()
                    }
                    .ignoresSafeArea(edges: .bottom)
                case .scroll:
                    // 📖 25-Jul-2026: when the book IS a single PDF, scroll mode uses Apple's native
                    //   in-app PDFView (traditional reader): it loads the file ONCE and scrolls/zooms/
                    //   counts pages lazily itself — no 1,443 hand-built sheets, no burst renders, low
                    //   memory. Page-curl mode still uses the paper-curl sheets. Mixed-content notes
                    //   (text+images+pdf) keep the stacked-sheet scroll below.
                    if let pdfPath = singlePdfPath {
                        NativePdfScrollView(path: pdfPath, startPageIndex: startIndex) { index in
                            currentIndex = index
                            saveProgress()
                        }
                        .ignoresSafeArea(edges: .bottom)
                    } else {
                        BookScrollReader(pages: pages, startIndex: startIndex) { index in
                            currentIndex = index
                            saveProgress()   // 📖 same for scrolling
                        }
                        .ignoresSafeArea(edges: .bottom)
                    }
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
            // 📖 23-Jul-2026: reading-mode toggle, right where it's used (mirrored in Settings)
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
            // 📖 the mode is global and can change from Settings while the reader is open
            readerPrefs.observeReadingMode { readingMode = $0 }
        }
        // 📖 23-Jul-2026: persist the position when the reader closes (Android saves in onDispose)
        .onDisappear { saveProgress() }
    }

    // 📖 25-Jul-2026: the book is a single PDF when every page is a .pdf sheet from ONE file (i.e. a
    //   tapped PDF opened as its own book). In that case scroll mode hands the file to a native
    //   PDFView instead of the hand-built sheet stack. Mixed notes return nil and keep the stack.
    private var singlePdfPath: String? {
        // require EVERY page to be a .pdf sheet from the SAME file — no cover, no other content — so
        // page index i in `pages` maps 1:1 to PDF page i (progress/counter stay correct).
        guard !pages.isEmpty else { return nil }
        var path: String? = nil
        for page in pages {
            guard case .pdf(let p, _, _, _, _) = page else { return nil }
            if path == nil { path = p } else if path != p { return nil }
        }
        return path
    }

    // 📖 23-Jul-2026: write the reading position onto the document's MediaContent row
    private func saveProgress() {
        guard let contentId = progressContentId, !pages.isEmpty else { return }
        let page = min(max(currentIndex, 0), pages.count - 1)
        readerPrefs.saveProgress(contentId: contentId, page: page, totalPages: pages.count)
    }

    // 📖 23-Jul-2026: resume comes from the media row itself — progressPage/totalPages arrive with
    //   the note, so there is no async preference read to wait on. Resolved inside loadNote's
    //   background build (it needs the built page count to clamp against).

    private func loadNote() {
        adapter.readNote(noteId: noteId) { result in
            switch result {
            // 🔧 19-Jul-2026: FIX — a late Loading emission must not bring the loader back
            case .loading: if pages.isEmpty { isLoading = true }
            case .success(let note):
                guard let note else { isLoading = false; return }
                // 🐛 23-Jul-2026 CRASH FIX (watchdog on opening the reader): page building opens the
                //   PDF and materialises one item PER PAGE — 1443 items for a big book — and it was
                //   doing that ON THE MAIN THREAD inside this callback. The UI froze long enough for
                //   iOS to kill the app (0x8badf00d), which reads as "crashes when I open a PDF".
                //   Build off-main, then publish the result back on main.
                let isSingle = singleContent
                let wantedId = startContentId
                let allContents = (note.contents as? [NoteContentModel]) ?? []
                DispatchQueue.global(qos: .userInitiated).async { [self] in
                    let built: [BookPageItem]
                    let media: NoteContentModel.MediaContent?
                    if isSingle, let id = wantedId,
                       let content = allContents.first(where: { $0.id == id }) {
                        built = BookPagesBuilder.buildForContent(content)
                        media = content as? NoteContentModel.MediaContent
                    } else {
                        built = BookPagesBuilder.build(note: note)
                        // the whole-note book tracks the first paged document — that's what the
                        // page indices line up with (Android parity)
                        media = built.compactMap(\.sourceContentId).first.flatMap { firstId in
                            allContents.first { $0.id == firstId } as? NoteContentModel.MediaContent
                        }
                    }
                    // an explicit startContentId (deep link / tapped card) is an intentional jump
                    // and always wins over the stored resume page
                    let jump = (isSingle ? nil : wantedId).flatMap { id in
                        built.firstIndex { $0.sourceContentId == id }
                    }
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
                // 🔧 19-Jul-2026: FIX (first-load "No record found") — the read flow can emit a
                //   local-miss failure BEFORE data arrives; never flash it. Keep the loader when
                //   nothing is built yet, ignore once pages exist.
                print("BookReader read failed (suppressed for UX): \(error.message)")
            case .idle: break
            }
        }
    }
}

// MARK: - Continuous scroll reader

// 📖 23-Jul-2026: NEW — continuous reading mode. Reuses the SAME BookPageContentView sheets as the
//   curl reader (nothing duplicated), stacked vertically so long PDFs read as one document.
//   Android's BookScrollReader is the mirror of this.
// 📖 23-Jul-2026: DOCUMENT-LEVEL zoom. Pinching used to zoom ONE sheet in isolation, so you'd
//   magnify a single page while the rest of the document stayed small. This wraps the stack in a
//   UIScrollView with native zooming — the standard document-reader behaviour on iOS: pinch to zoom
//   the whole document, pan while zoomed, double-tap to toggle, keep scrolling at that zoom.
struct BookScrollReader: UIViewRepresentable {
    let pages: [BookPageItem]
    let startIndex: Int
    let onPageChanged: (Int) -> Void

    private static let pageHeight: CGFloat = 560
    private static let spacing: CGFloat = 12
    private static let maxZoom: CGFloat = 5
    private static let doubleTapZoom: CGFloat = 2.5

    func makeUIView(context: Context) -> UIScrollView {
        // 📖 23-Jul-2026: a plain UIScrollView only gets a resume retry when SwiftUI happens to call
        //   updateUIView. This subclass retries on every layout pass, so the jump lands as soon as
        //   the document has real measurements — no polling, no fixed delay.
        let scrollView = ResumeAwareScrollView()
        scrollView.onLayout = { [weak coordinator = context.coordinator] view in
            coordinator?.applyPendingStartIfReady(view)
        }
        scrollView.delegate = context.coordinator
        scrollView.backgroundColor = .clear
        scrollView.minimumZoomScale = 1
        scrollView.maximumZoomScale = Self.maxZoom
        // pinch/pan/scroll all come from UIScrollView itself — no custom gesture maths
        scrollView.bouncesZoom = true
        scrollView.showsVerticalScrollIndicator = true

        let host = UIHostingController(rootView: content)
        host.view.backgroundColor = .clear
        scrollView.addSubview(host.view)
        context.coordinator.hostingController = host
        context.coordinator.contentView = host.view
        context.coordinator.pageCount = pages.count
        context.coordinator.contentSignature = pages.count

        // double-tap toggles 1x ↔ 2.5x, centred on the tap
        let doubleTap = UITapGestureRecognizer(target: context.coordinator,
                                               action: #selector(Coordinator.handleDoubleTap(_:)))
        doubleTap.numberOfTapsRequired = 2
        scrollView.addGestureRecognizer(doubleTap)

        layout(scrollView, coordinator: context.coordinator)
        // 📖 23-Jul-2026: jump to the resumed page ONLY once the document is actually laid out.
        //   This used to fire on the next runloop turn, before contentSize was set, so scrolling to
        //   page 100 of a not-yet-measured document landed nowhere and got clamped back to page 1.
        //   pendingStartIndex is consumed by the coordinator after layout settles (see
        //   applyPendingStartIfReady), i.e. "load the full doc first, then go to the page".
        context.coordinator.pendingStartIndex = min(max(startIndex, 0), max(pages.count - 1, 0))
        context.coordinator.applyPendingStartIfReady(scrollView)
        return scrollView
    }

    func updateUIView(_ scrollView: UIScrollView, context: Context) {
        context.coordinator.onPageChanged = onPageChanged
        context.coordinator.pageStride = Self.pageHeight + Self.spacing
        context.coordinator.pageCount = pages.count
        // 🐛 23-Jul-2026 CRASH FIX: only rebuild when the document actually changed. This used to
        //   reassign rootView and re-lay-out on EVERY SwiftUI update — including the one caused by
        //   our own page callback — so scrolling fed itself an endless update loop.
        let signature = pages.count
        if context.coordinator.contentSignature != signature {
            context.coordinator.contentSignature = signature
            context.coordinator.hostingController?.rootView = content
            layout(scrollView, coordinator: context.coordinator)
            // a rebuild can change the page count — re-target the pending resume against it
            context.coordinator.pendingStartIndex = min(max(startIndex, 0), max(pages.count - 1, 0))
        }
        // 📖 retry the resume jump until the document is measured and the offset actually sticks
        context.coordinator.applyPendingStartIfReady(scrollView)
    }

    private var content: AnyView {
        AnyView(LazyVStack(spacing: Self.spacing) {
            ForEach(Array(pages.enumerated()), id: \.element.id) { _, page in
                // a sheet is tall but finite — it scrolls with the document, and never zooms on
                // its own: the scroll view zooms the whole surface together
                BookPageContentView(page: page, allowPageZoom: false)
                    .frame(height: Self.pageHeight)
            }
        }
        .padding(.vertical, Self.spacing))
    }

    private func layout(_ scrollView: UIScrollView, coordinator: Coordinator) {
        guard let contentView = coordinator.contentView else { return }
        let width = scrollView.bounds.width > 0 ? scrollView.bounds.width : UIScreen.main.bounds.width
        let height = CGFloat(pages.count) * (Self.pageHeight + Self.spacing) + Self.spacing
        contentView.frame = CGRect(x: 0, y: 0, width: width, height: height)
        scrollView.contentSize = CGSize(width: width, height: height)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onPageChanged: onPageChanged, pageStride: Self.pageHeight + Self.spacing,
                    doubleTapZoom: Self.doubleTapZoom)
    }

    final class Coordinator: NSObject, UIScrollViewDelegate {
        var hostingController: UIHostingController<AnyView>?
        var contentView: UIView?
        var onPageChanged: (Int) -> Void
        var pageStride: CGFloat
        let doubleTapZoom: CGFloat
        // 🐛 23-Jul-2026: page count + a signature so updateUIView only rebuilds on real changes
        var pageCount = 0
        var contentSignature = -1
        // 📖 23-Jul-2026: the resume target, held until the document is laid out enough to honour it
        var pendingStartIndex: Int?
        private var lastReported = -1

        /// 📖 Scrolls to the pending resume page once the document is genuinely ready — i.e. the
        ///   scroll view has a real height and contentSize covers the target offset. Until then the
        ///   request is kept and retried, so a big PDF finishes loading BEFORE we jump into it.
        func applyPendingStartIfReady(_ scrollView: UIScrollView) {
            guard let target = pendingStartIndex, target > 0, pageStride > 0 else {
                if pendingStartIndex == 0 { pendingStartIndex = nil }
                return
            }
            let wanted = CGFloat(target) * pageStride
            let maxOffset = scrollView.contentSize.height - scrollView.bounds.height
            guard scrollView.bounds.height > 0, maxOffset > 0 else { return }   // not laid out yet
            // only jump when the document is tall enough that the target is a real position
            guard wanted <= maxOffset else { return }
            pendingStartIndex = nil
            lastReported = target
            scrollView.setContentOffset(CGPoint(x: 0, y: wanted), animated: false)
        }

        init(onPageChanged: @escaping (Int) -> Void, pageStride: CGFloat, doubleTapZoom: CGFloat) {
            self.onPageChanged = onPageChanged
            self.pageStride = pageStride
            self.doubleTapZoom = doubleTapZoom
        }

        // the zoomed subview is the whole page stack — that's what makes it a DOCUMENT zoom
        func viewForZooming(in scrollView: UIScrollView) -> UIView? { contentView }

        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            guard pageStride > 0, pageCount > 0 else { return }
            // report the sheet currently at the top, in unzoomed document coordinates
            let raw = Int((scrollView.contentOffset.y / max(scrollView.zoomScale, 0.01)) / pageStride)
            let index = min(max(raw, 0), pageCount - 1)
            guard index != lastReported else { return }
            lastReported = index
            // 🐛 23-Jul-2026 CRASH FIX: this runs DURING UIKit's scroll/layout pass, and the callback
            //   writes a SwiftUI @State. Mutating state inside a view update re-enters
            //   updateUIView -> layout -> scroll and blows the stack ("Modifying state during view
            //   update"). Hopping to the next runloop turn takes the write out of that pass.
            let callback = onPageChanged
            DispatchQueue.main.async { callback(index) }
        }

        @objc func handleDoubleTap(_ gesture: UITapGestureRecognizer) {
            guard let scrollView = gesture.view as? UIScrollView else { return }
            if scrollView.zoomScale > scrollView.minimumZoomScale {
                scrollView.setZoomScale(scrollView.minimumZoomScale, animated: true)
            } else {
                // zoom in around the tapped point, like Preview/Books
                let point = gesture.location(in: contentView)
                let size = scrollView.bounds.size
                let width = size.width / doubleTapZoom
                let height = size.height / doubleTapZoom
                scrollView.zoom(to: CGRect(x: point.x - width / 2, y: point.y - height / 2,
                                           width: width, height: height), animated: true)
            }
        }
    }
}

// 📖 23-Jul-2026: scroll view that reports each layout pass, so the "load fully, then jump to the
//   saved page" resume can be applied the moment the content is measured.
final class ResumeAwareScrollView: UIScrollView {
    var onLayout: ((UIScrollView) -> Void)?

    override func layoutSubviews() {
        super.layoutSubviews()
        onLayout?(self)
    }
}

// MARK: - Native PDF reader (single-PDF scroll mode)

// 📖 25-Jul-2026: for a book that is ONE PDF, scroll mode uses Apple's native in-app PDFView instead
//   of the hand-built sheet stack. PDFView opens the file ONCE and does continuous scroll, pinch-zoom,
//   page counting and lazy page rendering itself — so a 1,443-page book loads instantly and stays
//   low-memory (this is the "traditional PDF reader" that used to be fast). It renders INSIDE the app;
//   no external viewer is launched. Reports the current page for progress and resumes to startPageIndex.
struct NativePdfScrollView: UIViewRepresentable {
    let path: String
    let startPageIndex: Int
    let onPageChanged: (Int) -> Void

    func makeUIView(context: Context) -> PDFView {
        let pdfView = PDFView()
        pdfView.backgroundColor = .clear
        pdfView.displayMode = .singlePageContinuous   // traditional vertical scroll
        pdfView.displayDirection = .vertical
        pdfView.autoScales = true                      // fit-to-width, pinch to zoom from there
        pdfView.usePageViewController(false)
        // load the document ONCE, off the main thread, then attach on main
        let target = path
        DispatchQueue.global(qos: .userInitiated).async {
            let document = PDFDocument(url: URL(fileURLWithPath: target))
            DispatchQueue.main.async {
                guard let document else { return }
                pdfView.document = document
                // resume to the saved page once the document is attached
                let clamped = max(0, min(startPageIndex, document.pageCount - 1))
                if let page = document.page(at: clamped) { pdfView.go(to: page) }
            }
        }
        // observe page changes for the progress counter
        context.coordinator.observe(pdfView)
        return pdfView
    }

    func updateUIView(_ pdfView: PDFView, context: Context) {
        context.coordinator.onPageChanged = onPageChanged
    }

    func makeCoordinator() -> Coordinator { Coordinator(onPageChanged: onPageChanged) }

    final class Coordinator: NSObject {
        var onPageChanged: (Int) -> Void
        private weak var pdfView: PDFView?
        init(onPageChanged: @escaping (Int) -> Void) { self.onPageChanged = onPageChanged }

        func observe(_ pdfView: PDFView) {
            self.pdfView = pdfView
            NotificationCenter.default.addObserver(
                self, selector: #selector(pageChanged),
                name: .PDFViewPageChanged, object: pdfView)
        }

        @objc private func pageChanged() {
            guard let pdfView, let current = pdfView.currentPage,
                  let document = pdfView.document else { return }
            let index = document.index(for: current)
            onPageChanged(index)
        }

        deinit { NotificationCenter.default.removeObserver(self) }
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

// 📖 23-Jul-2026: wraps a page in ZoomableView only in page-curl mode. In scroll mode the enclosing
//   UIScrollView zooms the whole document, so per-page zoom must be off or the two gestures fight.
struct MaybeZoomable<Content: View>: View {
    let enabled: Bool
    @ViewBuilder var content: Content

    var body: some View {
        if enabled {
            ZoomableView { content }
        } else {
            content
        }
    }
}


struct BookPageContentView: View {
    let page: BookPageItem
    // 📖 23-Jul-2026: false in scroll mode — the WHOLE document zooms there, so an individual sheet
    //   must not also pinch-zoom on its own (that was the old one-page-only behaviour).
    var allowPageZoom: Bool = true
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
            pageBody.padding(.horizontal, 4).padding(.vertical, 4)
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
                // 🔧 19-Jul-2026: pinch/double-tap zoom; scaledToFit — image never fills/crops
                MaybeZoomable(enabled: allowPageZoom) {
                    if FileManager.default.fileExists(atPath: path), let ui = UIImage(contentsOfFile: path) {
                        Image(uiImage: ui).resizable().scaledToFit()
                    } else {
                        AsyncImage(url: URL(string: path)) { img in img.resizable().scaledToFit() }
                            placeholder: { ProgressView() }
                    }
                }
                if !title.isEmpty {
                    Text(title).font(.caption.italic()).foregroundColor(BookPalette.ink.opacity(0.7)).lineLimit(1)
                }
            }

        case .pdf(let path, let pageIndex, _, _, _):
            VStack(spacing: 6) {
                // 🔧 19-Jul-2026: pinch/double-tap zoom on PDF sheets
                MaybeZoomable(enabled: allowPageZoom) {
                    PdfSheetView(path: path, pageIndex: pageIndex)
                }
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

// 📖 25-Jul-2026: process-wide cache of rendered PDF sheets, keyed by "path#pageIndex". Switching
//   page-curl ↔ scroll tears down and rebuilds every sheet; without a cache each toggle (and every
//   re-scroll past a page) re-opened the PDFDocument and re-rendered the SAME bitmap — the "multiple
//   instances of the document" waste. NSCache auto-evicts under memory pressure, so big books stay safe.
enum PdfSheetCache {
    private static let cache: NSCache<NSString, UIImage> = {
        let c = NSCache<NSString, UIImage>()
        c.countLimit = 24   // keep a small working set of recently-seen pages
        return c
    }()
    static func key(path: String, pageIndex: Int) -> NSString { "\(path)#\(pageIndex)" as NSString }
    static func image(path: String, pageIndex: Int) -> UIImage? { cache.object(forKey: key(path: path, pageIndex: pageIndex)) }
    static func store(_ image: UIImage, path: String, pageIndex: Int) { cache.setObject(image, forKey: key(path: path, pageIndex: pageIndex)) }
}

// 🐛 25-Jul-2026 iOS-16 CRASH/LAG FIX ("CGBitmapContextInfoCreate: unable to allocate 8029312 bytes"
//   repeated across ~10 threads at once): scroll mode mounts many PdfSheetViews together and EACH one
//   fired its own full-res render concurrently — a dozen simultaneous ~8 MB bitmap allocations exhaust
//   memory on a real device (esp. iOS 16), which is the burst of allocation failures + the scroll lag.
//   This gate caps how many PDF pages render AT ONCE, so memory stays bounded and scrolling stays smooth.
actor PdfRenderGate {
    static let shared = PdfRenderGate(limit: 2)   // at most 2 pages rendering concurrently
    private let limit: Int
    private var active = 0
    private var waiters: [CheckedContinuation<Void, Never>] = []
    init(limit: Int) { self.limit = limit }

    /// Returns true once a render slot is held. Returns false if the task was cancelled while waiting
    /// (a page scrolled past), so the caller does NOT need to release — the slot was never granted.
    func acquire() async -> Bool {
        if Task.isCancelled { return false }
        if active < limit { active += 1; return true }
        await withCheckedContinuation { waiters.append($0) }
        // resumed either by a release (slot is ours) — honour cancellation but the slot IS granted here
        active += 1
        return true
    }
    func release() {
        active -= 1
        if !waiters.isEmpty { waiters.removeFirst().resume() }
    }
}

// 🔧 18-Jul-2026: one PDF page rendered as an image via PDFKit thumbnails (crisp + cheap)
struct PdfSheetView: View {
    let path: String
    let pageIndex: Int
    @State private var image: UIImage?
    // 🐛 23-Jul-2026 FIX (infinite loader): a null image was indistinguishable from "still loading",
    //   so if the render ever produced nothing the sheet span forever. Track a done flag and show a
    //   clear state instead of a permanent spinner.
    @State private var didFinish = false

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFit()
            } else if didFinish {
                // rendered but empty — better than an endless spinner
                Color.clear
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task(id: "\(path)#\(pageIndex)") {
            // 🐛 23-Jul-2026: .task (not .onAppear) so it re-runs when the sheet is reused for a new
            //   page, and is tied to the view lifecycle. Renders off the main actor.
            if image != nil { return }
            // 📖 25-Jul-2026: reuse the already-rendered page across mode switches / re-scrolls
            if let cached = PdfSheetCache.image(path: path, pageIndex: pageIndex) {
                image = cached; didFinish = true; return
            }
            // 🐛 25-Jul-2026 iOS-16: throttle concurrent renders so a fast scroll never fires a burst
            //   of huge bitmap allocations at once. A page scrolled past before its turn is skipped so
            //   the gate is never held for work no longer needed.
            let acquired = await PdfRenderGate.shared.acquire()
            guard acquired, !Task.isCancelled else {
                if acquired { await PdfRenderGate.shared.release() }
                didFinish = true
                return
            }
            let rendered = await Self.render(path: path, pageIndex: pageIndex)
            await PdfRenderGate.shared.release()
            if Task.isCancelled { return }
            await MainActor.run {
                if let rendered { PdfSheetCache.store(rendered, path: path, pageIndex: pageIndex) }
                image = rendered
                didFinish = true
            }
        }
    }

    // 🐛 23-Jul-2026: renders one sheet off-main. `path` is already the verified path the builder
    //   opened, so PDFDocument here mirrors the builder exactly.
    // 🐛 25-Jul-2026 CRASH FIX (scroll mode: "CGBitmapContextCreateImage: invalid context 0x0"):
    //   scroll mode eagerly renders EVERY PDF sheet, so it reaches pages page-curl never did. A page
    //   whose .mediaBox reports a zero/negative/non-finite width or height produced a 0-size (or NaN)
    //   CGSize, and PDFKit's thumbnail(of:) then asked CoreGraphics for a 0x0 bitmap context → the
    //   invalid-context abort. This clamps the render size to a finite, positive box (falling back to
    //   .cropBox, then a sane default) so a malformed page degrades to a blank sheet, never a crash.
    private static let fallbackPageSize = CGSize(width: 612, height: 792) // US-Letter @72dpi
    // 🐛 25-Jul-2026 iOS-16 MEMORY FIX: was 1600 (@scale 2 → ~8 MB/page, the "unable to allocate
    //   8029312 bytes" failures). A book sheet is ~560pt tall on screen, so ~1100px longest side is
    //   crisp on a 2–3× display while roughly HALVING each bitmap (~3–4 MB), which — with the render
    //   gate above — keeps peak memory bounded during a fast scroll on a real device.
    private static let maxRenderDimension: CGFloat = 1100

    private static func render(path: String, pageIndex: Int) async -> UIImage? {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                guard let doc = PDFDocument(url: URL(fileURLWithPath: path)),
                      pageIndex >= 0, pageIndex < doc.pageCount,
                      let pdfPage = doc.page(at: pageIndex) else {
                    continuation.resume(returning: nil)
                    return
                }
                // pick a usable box: mediaBox, else cropBox, else a sane default — never 0/NaN
                var bounds = pdfPage.bounds(for: .mediaBox)
                if !bounds.width.isFinite || !bounds.height.isFinite ||
                    bounds.width <= 1 || bounds.height <= 1 {
                    bounds = pdfPage.bounds(for: .cropBox)
                }
                let width = (bounds.width.isFinite && bounds.width > 1) ? bounds.width : Self.fallbackPageSize.width
                let height = (bounds.height.isFinite && bounds.height > 1) ? bounds.height : Self.fallbackPageSize.height
                // longest-side clamp (handles portrait AND landscape pages) at ≤1.5× native
                let longest = max(width, height)
                let scale = min(1.5, Self.maxRenderDimension / max(longest, 1))
                let targetSize = CGSize(width: max(width * scale, 1), height: max(height * scale, 1))
                // final guard: a non-finite/degenerate size must never reach CoreGraphics
                guard targetSize.width >= 1, targetSize.height >= 1,
                      targetSize.width.isFinite, targetSize.height.isFinite else {
                    continuation.resume(returning: nil)
                    return
                }
                let rendered = pdfPage.thumbnail(of: targetSize, for: bounds.width > 1 ? .mediaBox : .cropBox)
                continuation.resume(returning: rendered)
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
