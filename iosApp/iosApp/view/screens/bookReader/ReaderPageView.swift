import SwiftUI
import shared

// 📖 01-Aug-2026: ONE A4 sheet = a VStack of blocks. The engine already decided which blocks land
//   here, so this only stacks them in order — the reading sequence is whatever the engine produced.
struct ReaderPageView: View {
    let page: ReaderPage
    let policy: PageLayoutPolicy
    var fillHeight: Bool = true
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenMedia: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        // page mode scrolls INSIDE the sheet, so an expanded document can grow without breaking
        // the page geometry the engine calculated
        ScrollView(fillHeight ? .vertical : []) {
        VStack(alignment: .leading, spacing: CGFloat(policy.blockGap)) {
            ForEach(Array(page.blocks.enumerated()), id: \.offset) { _, block in
                ReaderBlockView(
                    block: block,
                    policy: policy,
                    onOpenDocument: onOpenDocument,
                    onOpenMedia: onOpenMedia
                )
            }
            if fillHeight { Spacer(minLength: 0) }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity, alignment: .top)
        }
        .frame(maxWidth: .infinity, maxHeight: fillHeight ? .infinity : nil, alignment: .top)
        .background(BookPalette.paper)
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}
