import SwiftUI
import AVFoundation


enum CapturedMedia {
    case image(UIImage)
    case video(URL)
}
struct CameraCaptureView: UIViewControllerRepresentable {
    
    @Binding var isPresented: Bool
    @Binding var capturedMedia: CapturedMedia?

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .camera
        picker.mediaTypes = ["public.image", "public.movie"] // Support both photos and videos
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {
        // No updates needed for the picker
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraCaptureView

        init(parent: CameraCaptureView) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            // Handle image capture
            if let image = info[.originalImage] as? UIImage {
                parent.saveMedia(.image(image))
            }

            // Handle video capture
            if let videoURL = info[.mediaURL] as? URL {
                parent.saveMedia(.video(videoURL))
            }

            parent.isPresented = false
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.isPresented = false
        }
    }

    private func saveMedia(_ media: CapturedMedia) {
        // Save the media reference to the binding property
        
        capturedMedia = media

    // Save media to a temporary location
        switch media {
        case .image(let image):
            if let data = image.jpegData(compressionQuality: 0.8) {
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".jpg")
                try? data.write(to: url)
                print("Image saved to: \(url)")
            }
        case .video(let url):
            let destination = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".mov")
            try? FileManager.default.copyItem(at: url, to: destination)
            print("Video saved to: \(destination)")
        }
    }
}
