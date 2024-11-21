//
//  MicPermission.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import AVFoundation
@MainActor
class MicPermission : ObservableObject {
    @Published var isMicPermissionGranted: Bool = false
      
    func checkMicPermission() -> Bool {
         let status = AVCaptureDevice.authorizationStatus(for: .audio)
        switch status {
            case .authorized:
                return true
            case .denied, .notDetermined:
                requestMicPermission()
                return false
            default:
                return false
        }
    }
    private func requestMicPermission() {
         AVCaptureDevice.requestAccess(for: .audio) { granted in
             self.isMicPermissionGranted = granted
         }
    }
}
