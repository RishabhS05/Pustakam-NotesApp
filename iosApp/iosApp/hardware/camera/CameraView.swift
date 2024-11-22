//
//  CameraView.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 22/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import UIKit

// A UIViewControllerRepresentable wrapper for UIImagePickerController
struct CameraView: UIViewControllerRepresentable {
    @Environment(\.presentationMode) var presentationMode
      @Binding var isPresented: Bool
      @Binding var capturedMedia: URL? // For video
      @Binding var capturedImage: UIImage? // For images
      let mediaTypes: [String]

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: CameraView

        init(parent: CameraView) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let mediaType = info[.mediaType] as? String {
                           if mediaType == "public.image",
                              let selectedImage = info[.originalImage] as? UIImage {
                               parent.capturedImage = selectedImage
                           } else if mediaType == "public.movie",
                                     let videoURL = info[.mediaURL] as? URL {
                               parent.capturedMedia = videoURL
                           }
                       }
                       parent.isPresented = false
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.presentationMode.wrappedValue.dismiss()
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
                picker.mediaTypes = mediaTypes
                picker.videoQuality = .typeHigh
                picker.sourceType = .camera
                picker.allowsEditing = false   
                picker.delegate = context.coordinator
                return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
}
