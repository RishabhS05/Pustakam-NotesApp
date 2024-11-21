//
//  CameraPermission.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import AVFoundation

class CameraPermission  : ObservableObject {
    
    var isCameraPermissionGranted : Bool{
        get async {
            let status = AVCaptureDevice.authorizationStatus(for: .video)
            var isAuthorized : Bool = false
            switch status {
                case .authorized:
                    isAuthorized = true
                    break
                case .notDetermined:
                    isAuthorized = await AVCaptureDevice.requestAccess(for: .video)
                    break
                
                case .restricted:
                    isAuthorized = false
                    break
                case .denied:
                    isAuthorized = false
                    break
                @unknown default:
                    isAuthorized = false
                    break
            }
            return isAuthorized
        }
        
    }
}
