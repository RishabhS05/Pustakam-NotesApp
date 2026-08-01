import SwiftUI
import shared

// 📖 01-Aug-2026: renders ReaderBlock.Title produced by the shared engine.
struct TitleBlockView: View {
    let block: ReaderBlock.Title

    var body: some View {
        VStack(spacing: 6) {
            Text(block.text)
                .font(.system(.title2, design: .serif))
                .foregroundColor(BookPalette.ink)
                .multilineTextAlignment(.center)
            Rectangle().fill(BookPalette.cover.opacity(0.5)).frame(width: 60, height: 2)
            if let subtitle = block.subtitle {
                Text(subtitle).font(.caption).foregroundColor(BookPalette.ink.opacity(0.6))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}
