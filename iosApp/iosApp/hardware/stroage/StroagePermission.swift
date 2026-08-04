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
