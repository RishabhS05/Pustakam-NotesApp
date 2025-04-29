
import Foundation
import UIKit

 func openAppSettings() {
    if let appSettingsURL = URL(string: UIApplication.openSettingsURLString) {
        UIApplication.shared.open(appSettingsURL, options: [:], completionHandler: nil)
    }
}
