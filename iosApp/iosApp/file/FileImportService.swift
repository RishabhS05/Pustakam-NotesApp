// 🔧 18-Jul-2026: NEW FEATURE (file import) — iOS side of multi-file + link import.
//   Mirrors Android fileimport/FileImportManager: files land in Documents/imported/<noteId>/,
//   MediaContent is built by the SHARED FileImportHelper (single source of type resolution).

import Foundation
import SwiftUI
import UniformTypeIdentifiers
import shared

// 🔧 18-Jul-2026: outcome of a link import — noFileFound drives the exact "No file found" UX
enum LinkImportResult {
    case success([NoteContentModel.MediaContent])
    case noFileFound
    case failed(String)
}

// 🔧 18-Jul-2026: SwiftUI wrapper over UIDocumentPickerViewController — ANY type, multi-select
struct MultiFilePicker: UIViewControllerRepresentable {
    let onPicked: ([URL]) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        // asCopy gives sandbox-safe temporary copies — no security-scope juggling needed
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.item], asCopy: true)
        picker.allowsMultipleSelection = true
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onPicked: onPicked) }

    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onPicked: ([URL]) -> Void
        init(onPicked: @escaping ([URL]) -> Void) { self.onPicked = onPicked }
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            onPicked(urls)
        }
    }
}

enum FileImportService {

    // 🔧 18-Jul-2026: same folder convention as captured media — imported/<noteId>/<name>
    private static func destinationFolder(noteId: String) -> String { "imported/\(noteId)" }

    // 🔧 18-Jul-2026: unique name only when a twin already exists (Android parity)
    private static func uniqueFileName(_ name: String, inFolder folder: URL) -> String {
        let safe = name.replacingOccurrences(of: "/", with: "_")
        let candidate = folder.appendingPathComponent(safe)
        guard FileManager.default.fileExists(atPath: candidate.path) else { return safe }
        return "\(Int(Date().timeIntervalSince1970 * 1000))_\(safe)"
    }

    /// Copies picked files into app storage and returns one MediaContent per file.
    static func importPicked(urls: [URL], noteId: String, startPosition: Double) -> [NoteContentModel.MediaContent] {
        guard let folderURL = createFolder(named: destinationFolder(noteId: noteId)) else { return [] }
        var position = startPosition
        return urls.compactMap { url in
            let name = uniqueFileName(url.lastPathComponent, inFolder: folderURL)
            let dest = folderURL.appendingPathComponent(name)
            do {
                try FileManager.default.copyItem(at: url, to: dest)
            } catch {
                print("❌ import copy failed: \(error)")
                return nil
            }
            let size = (try? FileManager.default.attributesOfItem(atPath: dest.path)[.size] as? Int64) ?? 0
            // 🔧 18-Jul-2026: SHARED factory — ext → mime → OTHER fallback, same as Android
            let media = FileImportHelper.shared.createImportedMedia(
                noteId: noteId, positionedAt: position, localPath: dest.path,
                fileName: url.lastPathComponent, mime: nil, sizeBytes: size ?? 0, sourceUrl: ""
            )
            position += 1.0
            return media
        }
    }

    /// Imports from ANY link: downloads when the response is a real file, else .noFileFound.
    static func importFromLink(_ rawUrl: String, noteId: String, startPosition: Double,
                               completion: @escaping (LinkImportResult) -> Void) {
        // 🔧 18-Jul-2026: forgiving input — default to https like Android
        let text = rawUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        let urlText = text.hasPrefix("http") ? text : "https://\(text)"
        guard let url = URL(string: urlText) else {
            DispatchQueue.main.async { completion(.noFileFound) }
            return
        }
        let task = URLSession.shared.downloadTask(with: url) { tempURL, response, error in
            let finish: (LinkImportResult) -> Void = { r in DispatchQueue.main.async { completion(r) } }
            if error != nil { return finish(.failed("Couldn't download from this link. Check the URL and your connection.")) }
            guard let tempURL, let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                return finish(.noFileFound)
            }
            let mime = http.mimeType
            let fileName = response?.suggestedFilename
                ?? FileImportHelper.shared.fileNameFromUrl(url: urlText)
            // 🔧 18-Jul-2026: html pages are NOT files → the requested "no file found" case
            guard FileImportHelper.shared.isDownloadableFile(mime: mime, fileName: fileName) else {
                return finish(.noFileFound)
            }
            guard let folderURL = createFolder(named: destinationFolder(noteId: noteId)) else {
                return finish(.failed("Couldn't prepare storage for the download."))
            }
            let dest = folderURL.appendingPathComponent(uniqueFileName(fileName, inFolder: folderURL))
            do {
                try FileManager.default.moveItem(at: tempURL, to: dest)
            } catch {
                return finish(.failed("Couldn't save the downloaded file."))
            }
            let size = (try? FileManager.default.attributesOfItem(atPath: dest.path)[.size] as? Int64) ?? 0
            guard (size ?? 0) > 0 else {
                try? FileManager.default.removeItem(at: dest)
                return finish(.noFileFound)
            }
            let media = FileImportHelper.shared.createImportedMedia(
                noteId: noteId, positionedAt: startPosition, localPath: dest.path,
                fileName: fileName, mime: mime, sizeBytes: size ?? 0, sourceUrl: urlText
            )
            finish(.success([media]))
        }
        task.resume()
    }
}
