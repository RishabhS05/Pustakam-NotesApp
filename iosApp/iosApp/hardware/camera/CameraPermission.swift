    //
    //  CameraPermission.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 23/11/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //

import AVFoundation
import SwiftUI
class CameraPermission  {
 func checkCameraPermission() -> Bool {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        var  isAuthorized: Bool = false
        switch status {
            case .authorized:
                isAuthorized = true
                return isAuthorized
            case .notDetermined :
                AVCaptureDevice.requestAccess(for: .video){ granted in
                    isAuthorized = granted
                }
                return isAuthorized
                
            case .restricted, .denied:
                isAuthorized = false
                return isAuthorized
            @unknown default:
                print("Unknown")
                isAuthorized = false
                return  isAuthorized
                
        }
    }

    func showAlert(onDismiss: @escaping () -> Void) -> Alert {
        return Alert(
            title: Text("Camera Access Required"),
            message: Text("Please enable camera access in Settings to use this feature."),
            primaryButton: .default(Text("Allow"), action: {
               openAppSettings()
               onDismiss()
            }),secondaryButton: .default(Text("Dont Allow"), action: {
                onDismiss()
            })
        )
    }
}
