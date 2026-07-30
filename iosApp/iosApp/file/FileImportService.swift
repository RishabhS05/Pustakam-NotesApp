//
//  LinkImportResult.swift
//  iosApp
//
//  Created by Rishabh on 19/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//


// 🔧 18-Jul-2026: NEW FEATURE (file import) — iOS side of multi-file + link import.
//   Mirrors Android fileimport/FileImportManager: files land in Documents/imported/<noteId>/,
// 🔧 30-Jul-2026 Phase 4 — planning (validate → name → dedupe → destination) and MediaContent
//   construction now come from the SHARED ImportCoordinator. This file performs IO only.

import Foundation
import SwiftUI
import UniformTypeIdentifiers
import shared

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
    // 🔧 30-Jul-2026 Phase 4 — destination, sanitising, duplicate resolution and type detection now
    //   come from the SHARED ImportCoordinator, the same one Android uses. This function only does
    //   the copying. `taken` carries the paths already used earlier in THIS batch, so two picked
    //   files with the same name can no longer resolve to the same destination — nothing is on disk
    //   yet at plan time, so only in-batch reservation catches that.
    static func importPicked(urls: [URL], noteId: String, startPosition: Double) -> [NoteContentModel.MediaContent] {
        var position = startPosition
        var taken: [String] = []

        return urls.compactMap { url in
            let size = ((try? FileManager.default.attributesOfItem(atPath: url.path)[.size]) as? Int64) ?? 0
            let decision = ImportCoordinator.shared.planImportForPlatform(
                noteId: noteId,
                requestedName: url.lastPathComponent,
                mime: nil,
                // 0 means "unknown" to the picker, not "empty" — pass 1 so it is not rejected.
                sizeBytes: size > 0 ? size : 1,
                sourceUrl: "",
                timestamp: DateTimeUtilsKt.getCurrentTimestamp(),
                takenPaths: taken
            )
            guard let accepted = decision as? ImportDecisionAccepted else { return nil }
            let plan = accepted.plan
            taken.append(plan.relativePath)

            guard let folderURL = createFolder(named: plan.destination.folder) else { return nil }
            let dest = folderURL.appendingPathComponent(plan.destination.fileName)
            do {
                try FileManager.default.copyItem(at: url, to: dest)
            } catch {
                print("❌ import copy failed: \(error)")
                return nil
            }
            let written = ((try? FileManager.default.attributesOfItem(atPath: dest.path)[.size]) as? Int64) ?? 0
            // SHARED factory — same construction path Android uses.
            let media = ImportCoordinator.shared.mediaFromPlan(
                plan: plan,
                noteId: noteId,
                positionedAt: position,
                localPath: dest.path,
                sizeBytes: written
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
                ?? FileNameGenerator.shared.fromUrl(
                    url: urlText, mime: mime, timestamp: DateTimeUtilsKt.getCurrentTimestamp())

            // 🔧 30-Jul-2026 Phase 4 — validation, naming and destination now come from the SHARED
            //   ImportCoordinator (same rules as Android). The DOWNLOAD ITSELF IS UNCHANGED.
            //   A web page still lands on .noFileFound, exactly as before.
            let decision = ImportCoordinator.shared.planImportForPlatform(
                noteId: noteId,
                requestedName: fileName,
                mime: mime,
                sizeBytes: 1,   // real size is read back after the move
                sourceUrl: urlText,
                timestamp: DateTimeUtilsKt.getCurrentTimestamp(),
                takenPaths: []
            )
            guard let accepted = decision as? ImportDecisionAccepted else {
                return finish(.noFileFound)
            }
            let plan = accepted.plan

            guard let folderURL = createFolder(named: plan.destination.folder) else {
                return finish(.failed("Couldn't prepare storage for the download."))
            }
            let dest = folderURL.appendingPathComponent(
                uniqueFileName(plan.destination.fileName, inFolder: folderURL))
            do {
                try FileManager.default.moveItem(at: tempURL, to: dest)
            } catch {
                return finish(.failed("Couldn't save the downloaded file."))
            }
            let size = ((try? FileManager.default.attributesOfItem(atPath: dest.path)[.size]) as? Int64) ?? 0
            guard size > 0 else {
                try? FileManager.default.removeItem(at: dest)
                return finish(.noFileFound)
            }
            let media = ImportCoordinator.shared.mediaFromPlan(
                plan: plan, noteId: noteId, positionedAt: startPosition,
                localPath: dest.path, sizeBytes: size
            )
            finish(.success([media]))
        }
        task.resume()
    }
}


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


