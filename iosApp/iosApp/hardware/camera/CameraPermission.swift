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
