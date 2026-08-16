import SwiftUI
import shared

struct ReaderPageView: View {
    let page: ReaderPage
    let policy: PageLayoutPolicy
    var fillHeight: Bool = true
    var zoomEnabled: Bool = true
    var documents: InlineDocumentState = .disabled
    var onOpenDocument: (NoteContentModel.MediaContent) -> Void = { _ in }
    var onOpenImage: (NoteContentModel.MediaContent) -> Void = { _ in }

    var body: some View {
        // 📖 15-Aug-2026: a document sheet IS the page — edge to edge, no paper margins, no rounding
        let isDocument = page.isDocumentSheet
        let sheet = ZStack(alignment: .topLeading) {
            BookPalette.paper
            if !isDocument {
                // the spine shading is book paper, not part of a document sheet
                HStack(spacing: 0) {
                    LinearGradient(
                        colors: [Color.black.opacity(0.18), .clear],
                        startPoint: .leading, endPoint: .trailing
                    )
                    .frame(width: 14)
                    Spacer()
                }
            }
            VStack(alignment: .leading, spacing: isDocument ? 0 : CGFloat(policy.blockGap)) {
                ForEach(Array(page.blocks.enumerated()), id: \.offset) { _, block in
                    ReaderBlockView(
                        block: block,
                        policy: policy,
                        documents: documents,
                        zoomEnabled: zoomEnabled,
                        onOpenDocument: onOpenDocument,
                        onOpenImage: onOpenImage
                    )
                }
                Spacer(minLength: 0)
            }
            .padding(.leading, isDocument ? 0 : CGFloat(policy.marginStart))
            .padding(.trailing, isDocument ? 0 : CGFloat(policy.marginEnd))
            .padding(.top, isDocument ? 0 : CGFloat(policy.marginTop))
            .padding(.bottom, isDocument ? 0 : CGFloat(policy.marginBottom))
        }
        .frame(maxWidth: .infinity)
        .frame(maxHeight: fillHeight ? .infinity : nil)
        .frame(height: fillHeight ? nil : CGFloat(policy.pageHeight))
        .clipShape(RoundedRectangle(cornerRadius: isDocument ? 0 : 6))

        sheet
            .padding(.vertical, isDocument ? 0 : 6)
    }
}
