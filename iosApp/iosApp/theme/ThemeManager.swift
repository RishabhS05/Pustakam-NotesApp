//
//  ThemeManager.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 05/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Observation

// 🎨 20-Jul-2026 — Granth spec §6 Appearance: three theme tiles (Light / Dark / AMOLED).
//   Additive — the existing isDarkMode / toggleTheme / getTheme API is preserved so no call site breaks;
//   `mode` is the new source of truth and isDarkMode stays in sync with it.
enum ThemeMode: String, CaseIterable {
    case light, dark, amoled

    var colorScheme: ColorScheme {
        switch self {
        case .light: return .light
        case .dark, .amoled: return .dark   // AMOLED is a dark variant; true-black surface via `amoled` colors
        }
    }
    /// true when a true-black OLED background should be used instead of the warm dark surfaces.
    var isAmoled: Bool { self == .amoled }
}

@Observable class ThemeManager {
    // 🎨 new source of truth
    var mode: ThemeMode = .light {
        didSet { isDarkMode = (mode != .light) }
    }

    var isDarkMode: Bool = false

    func toggleTheme() {
        // preserved behavior: flip between light and (warm) dark
        mode = isDarkMode ? .light : .dark
    }

    // 🎨 pick a specific tile (settings Appearance section)
    func setMode(_ newMode: ThemeMode) {
        mode = newMode
    }

    func getTheme() -> ColorScheme {
        mode.colorScheme
    }
}
