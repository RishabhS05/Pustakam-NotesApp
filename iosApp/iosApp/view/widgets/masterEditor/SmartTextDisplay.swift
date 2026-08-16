import SwiftUI
import UIKit
import shared

// 📖 15-Aug-2026: read-only twin of MasterTextWidget. It builds the SAME MasterTextState and hosts
//   the SAME MasterTextUITextView, so MasterTextRenderer.attributed() styles it and the view's own
//   draw() lays the bullets, numbers and checkboxes — editing, toolbars and the store left out.
//   Mirrors Android SmartTextDisplay.
struct SmartTextDisplay: View {
    let block: RichBlock
    var palette: SmartTextPalette
    var baseSize: CGFloat = SmartTextMetrics.baseFontSize * CGFloat(CanvasNode.companion.BASE_FONT_SCALE)

    var body: some View {
        switch block {
        case let text as RichBlock.Text:
            MasterTextParagraphView(block: text, palette: palette, baseSize: baseSize)
        case let code as RichBlock.Code:
            ScrollView(.horizontal, showsIndicators: false) {
                Text(code.code)
                    .font(.system(size: SmartTextMetrics.codeFontSize, design: .monospaced))
                    .foregroundColor(palette.onSurface)
                    .padding(10)
            }
            .background(palette.codeBackground)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.vertical, 4)
        case let table as RichBlock.Table:
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(table.data.rows.enumerated()), id: \.offset) { _, row in
                    HStack(alignment: .top, spacing: 0) {
                        ForEach(Array(row.cells.enumerated()), id: \.offset) { _, cell in
                            Text(cell.text)
                                .font(.system(size: baseSize))
                                .foregroundColor(palette.onSurface)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(4)
                        }
                    }
                }
            }
            .padding(6)
            .background(palette.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.vertical, 4)
        default:
            Rectangle()
                .fill(palette.divider)
                .frame(height: 1)
                .padding(.vertical, 8)
        }
    }
}

private struct MasterTextParagraphView: UIViewRepresentable {
    let block: RichBlock.Text
    let palette: SmartTextPalette
    let baseSize: CGFloat

    private var state: MasterTextState {
        MasterTextState.companion.of(document: RichDocument(blocks: [block]))
    }

    func makeUIView(context: Context) -> MasterTextUITextView {
        let view = MasterTextUITextView()
        view.isEditable = false
        view.isSelectable = false
        view.isScrollEnabled = false
        view.backgroundColor = .clear
        view.textContainerInset = .zero
        view.textContainer.lineFragmentPadding = 0
        view.setContentCompressionResistancePriority(.required, for: .vertical)
        view.setContentHuggingPriority(.required, for: .vertical)
        return view
    }

    func updateUIView(_ uiView: MasterTextUITextView, context: Context) {
        let current = state
        uiView.masterState = current
        uiView.configure(palette: palette, baseSize: baseSize)
        uiView.attributedText = MasterTextRenderer.attributed(
            state: current, palette: palette, baseSize: baseSize
        )
        uiView.setNeedsDisplay()
    }

    // the page is a fixed box, so the paragraph reports the height it actually needs inside it
    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView: MasterTextUITextView,
        context: Context
    ) -> CGSize? {
        let width = proposal.width ?? UIScreen.main.bounds.width
        let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: ceil(fitted.height))
    }
}
