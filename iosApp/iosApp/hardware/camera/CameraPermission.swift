import AVFoundation
import SwiftUI

class CameraPermission: AppPermission {

    var status: AppPermissionStatus {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:          return .granted
        case .notDetermined:       return .notDetermined
        case .restricted, .denied: return .denied
        @unknown default:          return .denied
        }
    }

    func request(_ completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in completion(granted) }
    }

    func checkCameraPermission() -> Bool { status == .granted }

    func showAlert(onDismiss: @escaping () -> Void) -> Alert {
        return Alert(
            title: Text("Camera Access Required"),
            message: Text("Please enable camera access in Settings to use this feature."),
            primaryButton: .default(Text("Open Settings"), action: {
                openAppSettings()
                onDismiss()
            }), secondaryButton: .cancel(Text("Not Now"), action: {
                onDismiss()
            })
        )
    }
}
