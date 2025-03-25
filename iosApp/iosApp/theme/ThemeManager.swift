//
//  ThemeManager.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 05/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Observation

@Observable class ThemeManager {
     var isDarkMode: Bool = false
    
    func toggleTheme() {
        isDarkMode.toggle()
    }
    func getTheme() -> ColorScheme {
        isDarkMode ? .dark: .light
    }
}
