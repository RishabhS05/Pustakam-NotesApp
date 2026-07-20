// 🔧 20-Jul-2026: NEW FEATURE (export) — turns a note into a PDF / PNG / DOCX file on iOS.
//   Walks the SHARED NoteExportBuilder blocks; DOCX bytes come straight from the SHARED
//   DocxExporter (only image base64 is supplied here). PDF + PNG share ONE layout pass
//   (buildItems) so text/image placement isn't written twice — mirrors Android NoteExporter.

import Foundation
import UIKit
import shared

// 🔧 20-Jul-2026: KotlinByteArray → Data so the shared .docx bytes can be written to disk
extension KotlinByteArray {
    func toData() -> Data {
        var data = Data(count: Int(size))
        data.withUnsafeMutableBytes { raw in
            let base = raw.bindMemory(to: Int8.self).baseAddress!
            for i in 0..<size { base[Int(i)] = self.get(index: i) }
        }
        return data
    }
}

enum NoteExporter {

    // A4 @ 72dpi + a long-image width; margins shared by both renderers
    private static let pdfSize = CGSize(width: 595, height: 842)
    private static let imageWidth: CGFloat = 1080
    private static let margin: CGFloat = 40
    private static let blockGap: CGFloat = 14

    // one measured element to place (text or a scaled image)
    private enum Item {
        case text(NSAttributedString, CGFloat)
        case image(UIImage, CGFloat)
        var height: CGFloat { switch self { case .text(_, let h): return h; case .image(_, let h): return h } }
    }

    // 🔧 20-Jul-2026: PUBLIC entry — writes to Documents/exports and returns the file URL, or nil
    static func export(note: Note, format: ExportFormat) -> URL? {
        let blocks = NoteExportBuilder.shared.build(note: note)
        guard let folder = createFolder(named: "exports") else { return nil }
        let safeTitle = (note.title?.isEmpty == false ? note.title! : "note")
            .components(separatedBy: CharacterSet.alphanumerics.inverted).joined(separator: "_")
        let fileURL = folder.appendingPathComponent("\(safeTitle)-\(Int(Date().timeIntervalSince1970))\(format.ext)")
        do {
            // Kotlin enums bridge to Swift as classes (not Swift enums) → switch on .name
            switch format.name {
            case "PDF":   try exportPdf(blocks: blocks, to: fileURL)
            case "IMAGE": try exportImage(blocks: blocks, to: fileURL)
            case "DOCX":  try exportDocx(blocks: blocks, to: fileURL)
            default: return nil
            }
            return fileURL
        } catch {
            print("❌ export failed: \(error)")
            return nil
        }
    }

    // ---- DOCX: all logic is SHARED; here we only base64 the images + write bytes ----
    private static func exportDocx(blocks: [ExportBlock], to url: URL) throws {
        var images: [String: String] = [:]
        for path in NoteExportBuilder.shared.imagePaths(blocks: blocks) {
            // resolve the (possibly stale) container path to read bytes, but key by the ORIGINAL
            // path — that's what DocxExporter looks the image up by
            let resolved = LocalFilePathResolver_iosKt.resolveLocalFilePath(path: path) ?? path
            if let data = FileManager.default.contents(atPath: resolved) {
                images[path] = data.base64EncodedString()
            }
        }
        let bytes = DocxExporter.shared.export(blocks: blocks, imagesBase64: images)
        try bytes.toData().write(to: url)
    }

    // ---- shared layout pass used by BOTH pdf + image ----
    private static func buildItems(blocks: [ExportBlock], contentWidth: CGFloat) -> [Item] {
        var items: [Item] = []
        // Kotlin enums bridge to Swift as classes → switch on the flat `kind.name` + plain fields
        for block in blocks {
            switch block.kind.name {
            case "TITLE":
                items.append(textItem(block.text, font: .boldSystemFont(ofSize: 26), color: .black, width: contentWidth))
            case "PARAGRAPH":
                items.append(textItem(block.text, font: .systemFont(ofSize: 15), color: .black, width: contentWidth))
            case "IMAGE":
                if let image = loadImage(block.path) {
                    let scale = min(1, contentWidth / max(image.size.width, 1))
                    items.append(.image(image, image.size.height * scale))
                    if !block.caption.isEmpty {
                        items.append(textItem(block.caption, font: .italicSystemFont(ofSize: 12), color: .darkGray, width: contentWidth))
                    }
                } else {
                    items.append(textItem("🖼 \(block.caption.isEmpty ? "Image" : block.caption) (not available)", font: .systemFont(ofSize: 15), color: .black, width: contentWidth))
                }
            case "FILE":
                items.append(textItem("📎 \(block.name)  (\(block.typeLabel))", font: .systemFont(ofSize: 14), color: .systemBlue, width: contentWidth))
            case "LINK":
                items.append(textItem("🔗 \(block.url)", font: .systemFont(ofSize: 14), color: .systemBlue, width: contentWidth))
            case "LOCATION":
                items.append(textItem("📍 \(block.label)", font: .systemFont(ofSize: 14), color: .systemBlue, width: contentWidth))
            default: break
            }
        }
        return items
    }

    private static func textItem(_ text: String, font: UIFont, color: UIColor, width: CGFloat) -> Item {
        let attr = NSAttributedString(string: text, attributes: [.font: font, .foregroundColor: color])
        let rect = attr.boundingRect(with: CGSize(width: width, height: .greatestFiniteMagnitude),
                                     options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil)
        return .text(attr, ceil(rect.height))
    }

    // ---- PDF: paginate items across A4 pages ----
    private static func exportPdf(blocks: [ExportBlock], to url: URL) throws {
        let contentWidth = pdfSize.width - 2 * margin
        let items = buildItems(blocks: blocks, contentWidth: contentWidth)
        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(origin: .zero, size: pdfSize))
        let data = renderer.pdfData { ctx in
            ctx.beginPage()
            var y = margin
            let bottom = pdfSize.height - margin
            for item in items {
                if y + item.height > bottom && y > margin {
                    ctx.beginPage()
                    y = margin
                }
                draw(item: item, at: CGPoint(x: margin, y: y), width: contentWidth)
                y += item.height + blockGap
            }
        }
        try data.write(to: url)
    }

    // ---- IMAGE: one tall PNG ----
    private static func exportImage(blocks: [ExportBlock], to url: URL) throws {
        let contentWidth = imageWidth - 2 * margin
        let items = buildItems(blocks: blocks, contentWidth: contentWidth)
        let totalHeight = max(imageWidth, 2 * margin + items.reduce(0) { $0 + $1.height + blockGap })
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: imageWidth, height: totalHeight))
        let image = renderer.image { ctx in
            UIColor.white.setFill()
            ctx.fill(CGRect(x: 0, y: 0, width: imageWidth, height: totalHeight))
            var y = margin
            for item in items {
                draw(item: item, at: CGPoint(x: margin, y: y), width: contentWidth)
                y += item.height + blockGap
            }
        }
        guard let png = image.pngData() else { throw NSError(domain: "export", code: 1) }
        try png.write(to: url)
    }

    private static func draw(item: Item, at point: CGPoint, width: CGFloat) {
        switch item {
        case .text(let attr, let h):
            attr.draw(with: CGRect(x: point.x, y: point.y, width: width, height: h),
                      options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil)
        case .image(let image, let h):
            image.draw(in: CGRect(x: point.x, y: point.y, width: width, height: h))
        }
    }

    private static func loadImage(_ path: String) -> UIImage? {
        let resolved = LocalFilePathResolver_iosKt.resolveLocalFilePath(path: path) ?? path
        if FileManager.default.fileExists(atPath: resolved) { return UIImage(contentsOfFile: resolved) }
        return nil
    }

    // 🔧 20-Jul-2026: system share sheet for the exported file (reuses topMostViewController from FileOps)
    static func share(url: URL) {
        guard let top = topMostViewController() else { return }
        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        if let pop = activity.popoverPresentationController {   // iPad anchor
            pop.sourceView = top.view
            pop.sourceRect = CGRect(x: top.view.bounds.midX, y: top.view.bounds.maxY, width: 0, height: 0)
            pop.permittedArrowDirections = []
        }
        top.present(activity, animated: true)
    }
}
