//
//  StroagePermission.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//



import SwiftUI
import UIKit

struct DocumentPicker: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.item], asCopy: true)
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    class Coordinator: NSObject, UIDocumentPickerDelegate {
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            print("Selected files: \(urls)")
        }
    }
}

// 🔧 14-Jul-2026: NEW FEATURE — "Save media to device" (non-gallery types).
//   Exports a file to a user-chosen location via the Files document picker. Used for
//   AUDIO/PDF/DOCX/GIF which can't be stored in the Photos gallery. The picker opens
//   at Documents by default; iOS copies the file into the folder the user selects.
//   Usage:  presentDocumentExporter(fileURL: url)
func presentDocumentExporter(fileURL: URL) {
    let picker = UIDocumentPickerViewController(forExporting: [fileURL], asCopy: true)
    picker.directoryURL = getDocumentsDirectory()   // default suggested location (Documents)
    picker.shouldShowFileExtensions = true
    guard let top = topMostViewController() else {
        print("❌ No view controller available to present the exporter")
        return
    }
    top.present(picker, animated: true)
}

// 🔧 14-Jul-2026: Helper — the currently visible view controller to present from.
func topMostViewController() -> UIViewController? {
    let windowScene = UIApplication.shared.connectedScenes
        .first { $0.activationState == .foregroundActive } as? UIWindowScene
    let keyWindow = windowScene?.windows.first(where: { $0.isKeyWindow }) ?? windowScene?.windows.first
    var top = keyWindow?.rootViewController
    while let presented = top?.presentedViewController {
        top = presented
    }
    return top
}
