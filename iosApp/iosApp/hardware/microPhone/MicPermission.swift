import AVFoundation
import SwiftUI

class MicPermission: AppPermission {

    var status: AppPermissionStatus {
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .authorized:          return .granted
        case .notDetermined:       return .notDetermined
        case .restricted, .denied: return .denied
        @unknown default:          return .denied
        }
    }

    func request(_ completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .audio) { granted in completion(granted) }
    }

    func checkMicPermission() -> Bool { status == .granted }

    func showAlert(onDismiss: @escaping () -> Void) -> Alert {
        return Alert(
            title: Text("Microphone Access Required"),
            message: Text("Please enable microphone access in Settings to use this feature."),
            primaryButton: .default(Text("Open Settings"), action: {
                openAppSettings()
                onDismiss()
            }), secondaryButton: .cancel(Text("Not Now"), action: {
                onDismiss()
            })
        )
    }
}
