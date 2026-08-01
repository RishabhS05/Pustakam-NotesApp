import SwiftUI
import shared

// 📖 01-Aug-2026: scroll mode over the SAME pages the curl mode renders — no second pagination.
//   Sheets wrap their content here (fillHeight false), which removes the empty space the old
//   fixed-height sheet forced onto every page. Page tracking uses measured offsets, never a fixed
//   stride: with variable page heights a stride would report the wrong page and corrupt progress.
struct ReaderScrollView: View {
    let pages: [ReaderPage]
    let policy: PageLayoutPolicy
    let startIndex: Int
    var onPageChanged: (Int) -> Void = { _ in }
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenMedia: (NoteContentModel.MediaContent) -> Void = { _ in }

    @State private var reportedIndex: Int = -1

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(Array(pages.enumerated()), id: \.offset) { index, page in
                        ReaderPageView(
                            page: page,
                            policy: policy,
                            fillHeight: false,
                            onOpenDocument: onOpenDocument,
                            onOpenMedia: onOpenMedia
                        )
                        .id(index)
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
                proxy.scrollTo(startIndex, anchor: .top)
            }
        }
    }
}
