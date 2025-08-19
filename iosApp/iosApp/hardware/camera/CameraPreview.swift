    //
    //  CameraContent.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 22/11/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //
import SwiftUI
import AVKit

    // SwiftUI View to Display the Camera
struct CameraPreview: View {
    @State private var capturedMedia: CapturedMedia?
    @Environment(\.dismiss) private var dismiss
    @State private var isCameraPresented = false
    var onCapture: (CapturedMedia?) -> Void
    var cameraPermission = CameraPermission()
    var body: some View {
        
        ZStack {
            if let media = capturedMedia {
                switch media {
                    case .image(let image):
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill().frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
                    case .video(let video):
                        VideoPlayer(player: AVPlayer(url: video))
                            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity).ignoresSafeArea()
                    case .audio(let audio): AudioRecorderView(){ media in }
                
                }
            }
            else {
                Text("No media captured")
            }
            VStack{
                Spacer()

                    Button("Done") {
                        onCapture(capturedMedia)
                        dismiss()
                    }.padding()
                    .background(Theme.Colors.secondary)
                        .cornerRadius(50)
                        .shadow(radius: 8)
                        .padding(8)
                
            }
               
        }.frame(maxWidth: .infinity, maxHeight: .infinity,)
        .onAppear() {
            isCameraPresented =  cameraPermission.checkCameraPermission()
        }.fullScreenCover(isPresented: $isCameraPresented ){
            CameraCaptureView(
                isPresented: $isCameraPresented,
                capturedMedia: $capturedMedia
            ).ignoresSafeArea()
        }
        
    }
}
