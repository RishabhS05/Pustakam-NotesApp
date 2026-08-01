import SwiftUI
import shared

struct LinkBlockView: View {
    let block: ReaderBlock.Link

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: "arrow.up.right.square").foregroundColor(BookPalette.cover)
            Text(block.url)
                .font(.subheadline)
                .foregroundColor(Color(red: 0.10, green: 0.33, blue: 0.46))
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(BookPalette.cover.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .contentShape(Rectangle())
        .onTapGesture {
            if let url = URL(string: block.url) { UIApplication.shared.open(url) }
        }
    }
}
