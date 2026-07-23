//
//  ThemePalette.swift
//  iosApp
//
//  Created by Rishabh on 22/07/26.
//  Copyright © 2026 orgName. All rights reserved.
//

import SwiftUI

// 🎨 22-Jul-2026 — Granth spec §2: AMOLED resolution layer.
//   Asset catalogs can only express two appearances (light/dark), so a third "AMOLED" ramp cannot
//   live in Assets.xcassets. This palette resolves each surface token against the *active* ThemeMode
//   and the *resolved* ColorScheme: normally it passes the catalog color straight through, and only
//   for .amoled does it substitute the true-black ramp. That keeps light/dark 100% catalog-driven
//   (so they still animate on appearance change) while making the AMOLED tile actually do something.
struct ThemePalette {

    let mode: ThemeMode
    /// The scheme iOS actually resolved — matters for .system, where mode alone can't tell us.
    let scheme: ColorScheme

    /// true only when the AMOLED tile is picked AND we are really rendering dark.
    var isAmoled: Bool { mode.isAmoled && scheme == .dark }

    // MARK: - AMOLED ramp (spec §2.3 — true black surfaces, warmer ink text)
    private static let amoledBackground   = Color(hex: "#000000")
    private static let amoledSurface      = Color(hex: "#0C0A07")
    private static let amoledSurface2     = Color(hex: "#13100B")
    private static let amoledSurfaceRaise = Color(hex: "#171309")
    private static let amoledText         = Color(hex: "#F0E6D0")
    private static let amoledText2        = Color(hex: "#AE9D7C")
    private static let amoledText3        = Color(hex: "#6E6248")
    private static let amoledAccent       = Color(hex: "#E8941A")
    private static let amoledLink         = Color(hex: "#94A1E0")

    // MARK: - Resolved tokens
    var background: Color   { isAmoled ? Self.amoledBackground   : Theme.Colors.background }
    var surface: Color      { isAmoled ? Self.amoledSurface      : Theme.Colors.surface }
    var surface2: Color     { isAmoled ? Self.amoledSurface2     : Theme.Colors.surface2 }
    var surfaceRaise: Color { isAmoled ? Self.amoledSurfaceRaise : Theme.Colors.surfaceRaise }
    var text: Color         { isAmoled ? Self.amoledText         : Theme.Colors.text }
    var text2: Color        { isAmoled ? Self.amoledText2        : Theme.Colors.text2 }
    var text3: Color        { isAmoled ? Self.amoledText3        : Theme.Colors.text3 }
    var accent: Color       { isAmoled ? Self.amoledAccent       : Theme.Colors.accent }
    var link: Color         { isAmoled ? Self.amoledLink         : Theme.Colors.link }

    // 🎨 Hairline borders are an alpha wash over the ink/ivory base in the spec, but the catalog
    //   colorset stores them opaque. Applying the opacity here keeps every border consistent.
    var border: Color {
        (isAmoled ? Color(hex: "#F0E6D0") : Theme.Colors.border)
            .opacity(scheme == .dark ? 0.10 : 0.10)
    }

    var borderStrong: Color {
        (isAmoled ? Color(hex: "#F0E6D0") : Theme.Colors.border)
            .opacity(scheme == .dark ? 0.18 : 0.18)
    }

    /// Chip / inactive-segment fill.
    var chip: Color {
        isAmoled ? Color(hex: "#15110B") : (scheme == .dark ? Color(hex: "#2C2419") : Color(hex: "#EFE3C9"))
    }

    /// Signature 135° saffron → copper gradient (spec §2.1). Same in every mode — it is the brand mark.
    var accentGradient: LinearGradient { Theme.Colors.accentGradient }

    /// Paper-grain overlay strength (spec §1). Dialled back as the surface darkens.
    var textureOpacity: Double {
        if isAmoled { return 0.18 }
        return scheme == .dark ? 0.30 : 0.55
    }
}

// MARK: - Environment plumbing

private struct ThemePaletteKey: EnvironmentKey {
    static let defaultValue = ThemePalette(mode: .system, scheme: .light)
}

extension EnvironmentValues {
    // 🎨 22-Jul-2026 — read anywhere with @Environment(\.palette) var palette
    var palette: ThemePalette {
        get { self[ThemePaletteKey.self] }
        set { self[ThemePaletteKey.self] = newValue }
    }
}

// 🎨 22-Jul-2026 — Applied once at the app root. Injects the palette built from the chosen mode and
//   the scheme iOS resolved, so .system + AMOLED both land on the right colors.
private struct ThemedRoot: ViewModifier {
    let mode: ThemeMode
    @Environment(\.colorScheme) private var resolvedScheme

    func body(content: Content) -> some View {
        content
            .environment(\.palette, ThemePalette(mode: mode, scheme: resolvedScheme))
    }
}

extension View {
    /// Injects `\.palette`. Apply *inside* `.preferredColorScheme(...)` so the resolved scheme is correct.
    func themedRoot(mode: ThemeMode) -> some View {
        modifier(ThemedRoot(mode: mode))
    }
}
