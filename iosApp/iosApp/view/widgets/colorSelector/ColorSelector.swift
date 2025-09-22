import SwiftUI

struct ColorSelector: View {
    @Binding var selectedColor: Color
    var showAlpha: Bool = false
    
    // Internal state in HSV
    @State private var hue: Double = 0.7
    @State private var saturation: Double = 0.5
    @State private var brightness: Double = 0.8
    @State private var alpha: Double = 1.0
    
    let swatches: [Color] = [
        .black, .white, .red, .orange, .yellow, .green,
        .blue, .purple, .pink, .teal, .indigo, .brown
    ]
    
    var body: some View {
        VStack(spacing: 20) {
            
            // Preview + hex
            HStack(spacing: 8) {
                Circle()
                    .fill(selectedColor)
                    .frame(width: 44, height: 44)
                    .overlay(Circle().stroke(Color.gray, lineWidth: 1))
                
                Text(selectedColor.tohexColor())
                    .font(.headline)
                
                Spacer()
                
                Text("Aa")
                    .padding(8)
                    .background(selectedColor)
                    .foregroundColor(selectedColor.luminance > 0.5 ? .black : .white)
                    .clipShape(Capsule())
            }
            
            // Swatches
            Text("Swatches")
                .font(.caption)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 6), spacing: 8) {
                ForEach(swatches, id: \.self) { c in
                    Circle()
                        .fill(c)
                        .frame(height: 36)
                        .overlay(
                            Circle()
                                .stroke(c.isClose(to: selectedColor) ? Color.accentColor : .gray, lineWidth: c.isClose(to: selectedColor) ? 2 : 1)
                        )
                        .onTapGesture {
                            selectedColor = c
                            updateHSV(from: c)
                        }
                }
            }
            
            // Sliders
            VStack {
                LabeledSlider(label: "Hue", value: $hue, range: 0...1) { updateColor() }
                LabeledSlider(label: "Saturation", value: $saturation, range: 0...1) { updateColor() }
                LabeledSlider(label: "Brightness", value: $brightness, range: 0...1) { updateColor() }
                if showAlpha {
                    LabeledSlider(label: "Alpha", value: $alpha, range: 0...1) { updateColor() }
                }
            }
        }
        .padding()
        .onAppear { updateHSV(from: selectedColor) }
    }
    
    // Update selected color when sliders change
    private func updateColor() {
        selectedColor = Color(hue: hue, saturation: saturation, brightness: brightness, opacity: alpha)
    }
    
    // Sync sliders when swatch is tapped
    private func updateHSV(from color: Color) {
        if let ui = UIColor(color).hsba {
            hue = Double(ui.h)
            saturation = Double(ui.s)
            brightness = Double(ui.b)
            alpha = Double(ui.a)
        }
    }
    
    // Convert to HEX string
    private func colorToHex(_ color: Color) -> String {
        if let rgba = UIColor(color).rgba {
            if rgba.a == 1 {
                return String(format: "#%02X%02X%02X", Int(rgba.r*255), Int(rgba.g*255), Int(rgba.b*255))
            } else {
                return String(format: "#%02X%02X%02X%02X", Int(rgba.a*255), Int(rgba.r*255), Int(rgba.g*255), Int(rgba.b*255))
            }
        }
        return "#000000"
    }
}

// MARK: - Labeled Slider
struct LabeledSlider: View {
    let label: String
    @Binding var value: Double
    let range: ClosedRange<Double>
    var onChange: () -> Void
    
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Text(label)
                Spacer()
                Text(label == "Hue" ? "\(Int(value*360))°" :
                        label == "Alpha" ? "\(Int(value*100))%" :
                        String(format: "%.2f", value))
                    .foregroundColor(.secondary)
            }
            Slider(value: Binding(get: { value }, set: {
                value = $0
                onChange()
            }), in: range)
        }
    }
}

// MARK: - Helpers
extension UIColor {
    var rgba: (r: CGFloat, g: CGFloat, b: CGFloat, a: CGFloat)? {
        var r: CGFloat=0, g: CGFloat=0, b: CGFloat=0, a: CGFloat=0
        return getRed(&r, green: &g, blue: &b, alpha: &a) ? (r,g,b,a) : nil
    }
    
    var hsba: (h: CGFloat, s: CGFloat, b: CGFloat, a: CGFloat)? {
        var h: CGFloat=0, s: CGFloat=0, b: CGFloat=0, a: CGFloat=0
        return getHue(&h, saturation: &s, brightness: &b, alpha: &a) ? (h,s,b,a) : nil
    }
}

extension Color {
    var luminance: CGFloat {
        UIColor(self).rgba.map { 0.299*$0.r + 0.587*$0.g + 0.114*$0.b } ?? 0
    }
    
    func isClose(to other: Color, threshold: CGFloat = 0.1) -> Bool {
        guard let a = UIColor(self).rgba, let b = UIColor(other).rgba else { return false }
        return abs(a.r-b.r) + abs(a.g-b.g) + abs(a.b-b.b) < threshold
    }
}

// MARK: - Preview

struct ColorSelector_Previews: PreviewProvider {
    static var previews: some View {
        StatefulPreviewWrapper(value: Color.blue) { binding in
            ColorSelector(selectedColor: binding, showAlpha: true)
        }
    }
}

// Simple wrapper to preview @Binding
struct StatefulPreviewWrapper<Value, Content: View>: View {
    @State var value: Value
    var content: (Binding<Value>) -> Content
    var body: some View { content($value) }
}
