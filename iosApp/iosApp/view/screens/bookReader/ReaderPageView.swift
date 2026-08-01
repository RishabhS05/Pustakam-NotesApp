import SwiftUI
import shared

struct ReaderPageView: View {
    let page: ReaderPage
    let policy: PageLayoutPolicy
    var fillHeight: Bool = true
    var zoomEnabled: Bool = true
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenImage: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        let sheet = ZStack(alignment: .topLeading) {
            BookPalette.paper
            HStack(spacing: 0) {
                LinearGradient(
                    colors: [Color.black.opacity(0.18), .clear],
                    startPoint: .leading, endPoint: .trailing
                )
                .frame(width: 14)
                Spacer()
            }
            VStack(alignment: .leading, spacing: CGFloat(policy.blockGap)) {
                ForEach(Array(page.blocks.enumerated()), id: \.offset) { _, block in
                    ReaderBlockView(
                        block: block,
                        policy: policy,
                        onOpenDocument: onOpenDocument,
                        onOpenImage: onOpenImage
                    )
                }
                Spacer(minLength: 0)
            }
            .padding(.leading, CGFloat(policy.marginStart))
            .padding(.trailing, CGFloat(policy.marginEnd))
            .padding(.top, CGFloat(policy.marginTop))
            .padding(.bottom, CGFloat(policy.marginBottom))
        }
        .frame(maxWidth: .infinity)
        .frame(maxHeight: fillHeight ? .infinity : nil)
        .frame(height: fillHeight ? nil : CGFloat(policy.pageHeight))
        .clipShape(RoundedRectangle(cornerRadius: 6))

        Group {
            if zoomEnabled {
                ZoomableView { sheet }
            } else {
                sheet
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
    }
}
