import SwiftUI
import PDFKit
import shared

// 📖 01-Aug-2026: renders ONE pdf page to an image, off the main thread. The document is opened per
//   page so a large file never holds more than a page of raster at a time.
struct DocumentPageImage: View {
    let path: String?
    let index: Int
    let policy: PageLayoutPolicy

    @State private var image: UIImage? = nil

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().scaledToFit()
            } else {
                Color.black.opacity(0.06)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: CGFloat(policy.usableWidth * PageLayoutPolicy.companion.a4Ratio()))
        .clipShape(RoundedRectangle(cornerRadius: 4))
        .task(id: index) { await render() }
    }

    private func render() async {
        guard image == nil, let path else { return }
        let rendered: UIImage? = await Task.detached(priority: .userInitiated) {
            guard let document = PDFDocument(url: URL(fileURLWithPath: path)),
                  index < document.pageCount,
                  let page = document.page(at: index) else { return nil }
            let bounds = page.bounds(for: .mediaBox)
            // cap the raster width at 1440pt — enough for a phone, cheap on memory
            let scale = min(2.0, 1440.0 / max(bounds.width, 1))
            let size = CGSize(width: bounds.width * scale, height: bounds.height * scale)
            return UIGraphicsImageRenderer(size: size).image { context in
                UIColor.white.set()
                context.fill(CGRect(origin: .zero, size: size))
                context.cgContext.translateBy(x: 0, y: size.height)
                context.cgContext.scaleBy(x: scale, y: -scale)
                page.draw(with: .mediaBox, to: context.cgContext)
            }
        }.value
        await MainActor.run { image = rendered }
    }
}
