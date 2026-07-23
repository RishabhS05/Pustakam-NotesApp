//
//  ThemeManager.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 05/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Observation

// 🎨 22-Jul-2026 — Granth spec §6 Appearance: four tiles (System / Light / Dark / AMOLED).
//   `system` is the modern-iOS default: it returns a nil ColorScheme so the app inherits the OS
//   appearance (Control Center toggle + scheduled dark) instead of pinning one.
//   Additive — the existing isDarkMode / toggleTheme / getTheme API is preserved so no call site breaks.
enum ThemeMode: String, CaseIterable, Identifiable {
    case system, light, dark, amoled

    var id: String { rawValue }

    /// nil = follow the OS appearance. Non-nil pins the app to one scheme.
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark, .amoled: return .dark   // AMOLED is a dark variant; true-black surfaces via `isAmoled`
        }
    }

    /// true when true-black OLED surfaces should replace the warm dark ones.
    var isAmoled: Bool { self == .amoled }

    // 🎨 Settings tile labels + SF Symbols (spec §6)
    var title: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        case .amoled: return "AMOLED"
        }
    }

    var icon: String {
        switch self {
        case .system: return "iphone.gen3"
        case .light: return "sun.max.fill"
        case .dark: return "moon.fill"
        case .amoled: return "circle.fill"
        }
    }
}

@Observable class ThemeManager {

    // 🎨 22-Jul-2026 — persisted across launches. @Observable can't use @AppStorage, so the
    //   defaults read/write is done by hand in the didSet + init.
    private static let storageKey = "granth.themeMode"

    /// 🎨 new source of truth
    var mode: ThemeMode {
        didSet {
            isDarkMode = (mode != .light)
            UserDefaults.standard.set(mode.rawValue, forKey: Self.storageKey)
        }
    }

    /// Preserved for existing call sites. For `.system` this is only a best-effort hint —
    /// views that need the real resolved scheme should read `\.colorScheme` from the environment.
    var isDarkMode: Bool = false

    init() {
        let saved = UserDefaults.standard.string(forKey: Self.storageKey)
        let restored = saved.flatMap(ThemeMode.init(rawValue:)) ?? .system
        self.mode = restored
        self.isDarkMode = (restored != .light)
    }

    func toggleTheme() {
        // preserved behavior: flip between light and (warm) dark
        mode = isDarkMode ? .light : .dark
    }

    // 🎨 pick a specific tile (settings Appearance section)
    func setMode(_ newMode: ThemeMode) {
        mode = newMode
    }

    /// nil means "follow the system", which is what `.preferredColorScheme(nil)` expects.
    func getTheme() -> ColorScheme? {
        mode.colorScheme
    }
}
