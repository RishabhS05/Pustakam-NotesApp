import SwiftUI
import shared

struct ReaderScrollView: View {
    let pages: [ReaderPage]
    let policy: PageLayoutPolicy
    let startIndex: Int
    var onPageChanged: (Int) -> Void = { _ in }
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenImage: (NoteContentModel.MediaContent) -> Void = { _ in }
    // 📖 15-Aug-2026: off keeps the shipped behaviour — documents stay cards, list never resizes
    var inlineDocumentsEnabled: Bool = false
    var inlineDocuments: InlineDocumentState = .disabled

    @State private var reportedIndex: Int = -1

    private var documents: InlineDocumentState {
        inlineDocumentsEnabled ? inlineDocuments : .disabled
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 8) {
                    // 📖 stableKey survives expand/collapse, so untouched pages are never rebuilt
                    ForEach(Array(pages.enumerated()), id: \.element.stableKey) { index, page in
                        ReaderPageView(
                            page: page,
                            policy: policy,
                            // 📖 15-Aug-2026: a page is a FIXED full-screen sheet (policy.pageHeight),
                            //   never content-sized — widgets are arranged inside that box
                            fillHeight: false,
                            zoomEnabled: true,
                            documents: documents,
                            onOpenDocument: onOpenDocument,
                            onOpenImage: onOpenImage
                        )
                        .id(page.stableKey)
                        .background(
                            GeometryReader { geometry in
                                Color.clear.preference(
                                    key: ReaderPageOffsetKey.self,
                                    value: [index: geometry.frame(in: .named("readerScroll")).minY]
                                )
                            }
                        )
                    }
                }
                .padding(.vertical, 8)
            }
            .coordinateSpace(name: "readerScroll")
            .onPreferenceChange(ReaderPageOffsetKey.self) { offsets in
                // topmost page whose frame has reached the viewport is the current page
                guard let current = offsets.filter({ $0.value <= 1 }).max(by: { $0.key < $1.key })?.key
                        ?? offsets.min(by: { $0.value < $1.value })?.key else { return }
                if current != reportedIndex {
                    reportedIndex = current
                    onPageChanged(current)
                }
            }
            .onAppear {
                guard startIndex > 0, startIndex < pages.count else { return }
                proxy.scrollTo(pages[startIndex].stableKey, anchor: .top)
            }
            // 📖 the caller moves the anchor deliberately (collapse lands back on the card)
            .task(id: "\(startIndex)-\(pages.count)") {
                guard inlineDocumentsEnabled, !pages.isEmpty else { return }
                let target = min(max(startIndex, 0), pages.count - 1)
                guard target != reportedIndex else { return }
                proxy.scrollTo(pages[target].stableKey, anchor: .top)
            }
        }
    }
}
