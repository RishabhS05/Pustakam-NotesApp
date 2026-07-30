import SwiftUI
import AVFoundation


enum CapturedMedia {
    case image(UIImage)
    case video(URL)
    case audio(URL)
}
struct CameraCaptureView: UIViewControllerRepresentable {
    
    @Binding var isPresented: Bool
    @Binding var capturedMedia: CapturedMedia?

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .camera
        picker.mediaTypes = ["public.image", "public.movie"]
        picker.modalPresentationStyle = .fullScreen
        picker.cameraCaptureMode = .photo
                picker.videoQuality = .typeHigh

                // Optional
                picker.allowsEditing = false
                picker.showsCameraControls = true
            // Support both photos and videos
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
        capturedMedia = media
    }
}
