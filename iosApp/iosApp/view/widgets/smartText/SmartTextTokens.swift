import SwiftUI

// standalone palette for the editor only — intentionally not wired to the app Theme
struct SmartTextPalette {
    let page: Color
    let surface: Color
    let toolbar: Color
    let onSurface: Color
    let onSurfaceMuted: Color
    let accent: Color
    let onAccent: Color
    let accentSoft: Color
    let highlight: Color
    let divider: Color
    let codeBackground: Color
    let searchHit: Color
    let searchHitActive: Color

    static let dark = SmartTextPalette(
        page: Color(hex: 0x161311),
        surface: Color(hex: 0x1C1815),
        toolbar: Color(hex: 0x2A2420),
        onSurface: Color(hex: 0xEFE7DC),
        onSurfaceMuted: Color(hex: 0xA2968A),
        accent: Color(hex: 0xE9A33C),
        onAccent: Color(hex: 0x1C1815),
        accentSoft: Color(hex: 0x3B2F1C),
        highlight: Color(hex: 0x5C4A1E),
        divider: Color(hex: 0x3A322B),
        codeBackground: Color(hex: 0x232019),
        searchHit: Color(hex: 0x4A4128),
        searchHitActive: Color(hex: 0x8A6A1E)
    )

    static let light = SmartTextPalette(
        page: Color(hex: 0xFBF6EE),
        surface: Color(hex: 0xFFFFFF),
        toolbar: Color(hex: 0xFFFDF9),
        onSurface: Color(hex: 0x2B2723),
        onSurfaceMuted: Color(hex: 0x6B6259),
        accent: Color(hex: 0xE2971F),
        onAccent: Color(hex: 0xFFFFFF),
        accentSoft: Color(hex: 0xF7E7C8),
        highlight: Color(hex: 0xFBE6A0),
        divider: Color(hex: 0xE7DED0),
        codeBackground: Color(hex: 0xF5EFE4),
        searchHit: Color(hex: 0xF3E3B4),
        searchHitActive: Color(hex: 0xEFC968)
    )

    static func of(_ scheme: ColorScheme) -> SmartTextPalette {
        scheme == .dark ? .dark : .light
    }
}

enum SmartTextMetrics {
    static let baseFontSize: CGFloat = 16
    static let codeFontSize: CGFloat = 14
    static let blockSpacing: CGFloat = 2
    static let indentStep: CGFloat = 20
    static let toolbarCorner: CGFloat = 12
    static let buttonCorner: CGFloat = 8
    static let buttonSize: CGFloat = 40
    static let minTouchTarget: CGFloat = 44
    static let quoteBarWidth: CGFloat = 3
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }

    // parses the "#RRGGBB" strings the shared span model stores
    init?(smartTextHex: String) {
        var value = smartTextHex.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.hasPrefix("#") { value.removeFirst() }
        guard value.count == 6 || value.count == 8, let parsed = UInt32(value, radix: 16) else {
            return nil
        }
        if value.count == 8 {
            let alpha = Double((parsed >> 24) & 0xFF) / 255
            self.init(hex: parsed & 0x00FFFFFF, alpha: alpha)
        } else {
            self.init(hex: parsed)
        }
    }
}

extension UIColor {
    convenience init?(smartTextHex: String) {
        var value = smartTextHex.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.hasPrefix("#") { value.removeFirst() }
        guard value.count == 6, let parsed = UInt32(value, radix: 16) else { return nil }
        self.init(
            red: CGFloat((parsed >> 16) & 0xFF) / 255,
            green: CGFloat((parsed >> 8) & 0xFF) / 255,
            blue: CGFloat(parsed & 0xFF) / 255,
            alpha: 1
        )
    }
}
