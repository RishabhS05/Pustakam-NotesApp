//
//  CameraHandler.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import CoreImage

@Observable
class CameraHandler{
    var currentFrame : CGImage?
 private let cameraManager  = CameraManager()
    
init() {
    Task {
                await handleCameraPreviews()
            }
}
    func handleCameraPreviews() async{
        for await image in cameraManager.previewStream {
            Task {  @MainActor in
                currentFrame = image
            }
        }
        
    }
}
