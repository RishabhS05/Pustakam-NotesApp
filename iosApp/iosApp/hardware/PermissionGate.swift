import Foundation

enum AppPermissionStatus {
    case granted
    case denied
    case notDetermined
}

protocol AppPermission {
    var status: AppPermissionStatus { get }
    func request(_ completion: @escaping (Bool) -> Void)
}

func requirePermission(
    _ permission: AppPermission,
    onGranted: @escaping () -> Void,
    onDenied: @escaping () -> Void
) {
    switch permission.status {
    case .granted:
        onGranted()
    case .denied:
        onDenied()
    case .notDetermined:
        permission.request { granted in
            DispatchQueue.main.async {
                if granted { onGranted() } else { onDenied() }
            }
        }
    }
}
