    //
    //  CameraContent.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 22/11/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //
import SwiftUI

    // SwiftUI View to Display the Camera
struct CameraContent: View {
       @State private var isCameraPresented = false
       @State private var capturedImage: UIImage?
       @State private var capturedVideo: URL?
     var cameraPermission = CameraPermission()
    

       var body: some View {
           CameraView(
               isPresented: $isCameraPresented,
               capturedMedia: $capturedVideo,
               capturedImage: $capturedImage,
               mediaTypes: ["public.image", "public.movie"]
           )
       }
}
