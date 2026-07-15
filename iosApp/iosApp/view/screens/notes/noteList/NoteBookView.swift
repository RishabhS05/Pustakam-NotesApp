import SwiftUI
import shared

// 🔧 15-Jul-2026 iOS parity (summary query): StaggeredGrid needs Identifiable items;
//   NoteSummary already exposes `id: String`, so the conformance is free.
extension NoteSummary: Identifiable {}

// 🔧 15-Jul-2026 iOS parity (summary query): the card renders a NoteSummary (was a full Note
//   whose contents it never used). Top to bottom — Android NoteCardView parity:
//   • title + updated date (unchanged look)
//   • the text snippet when the note HAS text…
//   • …otherwise media/doc COUNT BADGES ("3 photos · 1 audio") — media/doc-only notes are
//     first-class, not blank cards
//   • a thumbnail strip when the note has a visual media block
//   Usage: NoteBookView(summary: summary) { onOpen(summary) }
struct NoteBookView : View {
    let summary: NoteSummary
    let onClick: () -> Void

    var body: some View {
        GeometryReader{ geo in
            let width = geo.size.width
            let height = geo.size.height
            ZStack{
                VStack(alignment: .leading, spacing: 6){
                    Text(summary.title?.isEmpty == false ? summary.title! : "No Title ?")
                        .font(.system(size: 18, weight: .bold))
                        .lineLimit(2)
                        .padding(.top, 12)
                        .padding(.horizontal, 12)
                    if let snippet = summary.snippet, !snippet.isEmpty {
                        Text(snippet)
                            .font(.system(size: 13, weight: .regular))
                            .lineLimit(4)
                            .padding(.horizontal, 12)
                    } else {
                        mediaCountBadges
                            .padding(.horizontal, 12)
                    }
                    if let thumb = summary.thumbnailPath, !thumb.isEmpty,
                       let image = UIImage(contentsOfFile: thumb) {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: width - 16, maxHeight: 70)
                            .clipped()
                            .cornerRadius(6)
                            .padding(.horizontal, 8)
                    }
                    Spacer(minLength: 8)
                }
                .frame(maxWidth: width, maxHeight: height, alignment: .topLeading)
                    .padding(.vertical, 23)
                HStack{
                    Spacer()
                    Text("\(summary.updatedAt?.toLocalFormat(showTime: false) ?? "")")
                        .font(.system(size: 14, weight: .regular))
                        .background(.gray.gradient)
                        .cornerRadius(4)
                        .foregroundColor(Theme.Colors.onSurface)
                }
                .frame(width :width,
                        height : height,
                        alignment: .topTrailing)
            }
            .background(Theme.Colors.primary)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .onTapGesture {
                    onClick()
                }
        }
    }

    // 🔧 15-Jul-2026 iOS parity: compact "what's inside" row for notes without text.
    private var mediaCountBadges: some View {
        HStack(spacing: 10) {
            countBadge(systemName: "photo", count: Int(summary.imageCount))
            countBadge(systemName: "video.fill", count: Int(summary.videoCount))
            countBadge(systemName: "mic.fill", count: Int(summary.audioCount))
            countBadge(systemName: "doc.fill", count: Int(summary.docCount))
        }
    }

    @ViewBuilder
    private func countBadge(systemName: String, count: Int) -> some View {
        if count > 0 {
            HStack(spacing: 2) {
                Image(systemName: systemName).font(.system(size: 11))
                Text("\(count)").font(.system(size: 12, weight: .medium))
            }
        }
    }
}

#Preview {
    NoteBookView(
        summary: NoteSummary(
            id: "12345",
            title: "Hello World.",
            categoryId: "",
            createdAt: "24/03/2025",
            updatedAt: "24/03/2025",
            snippet: nil,
            contentCount: 4,
            imageCount: 3,
            videoCount: 0,
            audioCount: 1,
            docCount: 0,
            thumbnailPath: nil
        ),
             onClick: {
})
}
