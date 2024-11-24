//
//  HardwareManager.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 21/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import UIKit

class HardwareManager  : ObservableObject{
    
}
 func openAppSettings() {
    if let appSettingsURL = URL(string: UIApplication.openSettingsURLString) {
        UIApplication.shared.open(appSettingsURL, options: [:], completionHandler: nil)
    }
}
