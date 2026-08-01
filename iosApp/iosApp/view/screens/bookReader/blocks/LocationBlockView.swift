import SwiftUI
import shared

struct LocationBlockView: View {
    let block: ReaderBlock.Location

    private var coordinates: String {
        String(format: "%.5f, %.5f", block.latitude, block.longitude)
    }

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "mappin.circle.fill")
                .font(.title2).foregroundColor(BookPalette.cover)
            VStack(alignment: .leading, spacing: 2) {
                Text(block.address ?? coordinates)
                    .font(.subheadline).foregroundColor(BookPalette.ink)
                if block.address != nil {
                    Text(coordinates).font(.caption2).foregroundColor(BookPalette.ink.opacity(0.6))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(BookPalette.cover.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .contentShape(Rectangle())
        .onTapGesture {
            if let url = URL(string: "maps://?ll=\(block.latitude),\(block.longitude)") {
                UIApplication.shared.open(url)
            }
        }
    }
}
