import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0) // Default to clear or black if invalid hex
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
    // 🔧 07-Aug-2026 — green/blue were both read from rgba.r, so every colour came out grey;
    //   the alpha branch also emitted blue before green. Create Tag used this too.
    func tohexColor() ->  String {
        if let rgba = UIColor(self).rgba {
            let red =  Int64(rgba.r*255)
            let green =  Int64(rgba.g*255)
            let blue =  Int64(rgba.b*255)

            if rgba.a == 1 {
                return String(format: "#%02X%02X%02X", red, green, blue)
            } else {
                return String(format: "#%02X%02X%02X%02X", Int(rgba.a*255), red, green, blue)
            }
        }
        return "#000000"
    }
}
