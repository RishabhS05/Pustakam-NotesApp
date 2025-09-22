//
//  HSVColorPicker.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 26/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//


import SwiftUI

struct HSVColorPicker: View {
    @Binding var selectedColor: Color
    
    @State private var hue: Double = 0
    @State private var saturation: Double = 1
    @State private var brightness: Double = 1
    @State private var hex: String = "#FF0000"
    
    var body: some View {
        VStack(spacing: 16) {
            // SV square
            GeometryReader { geo in
                ZStack {
                    LinearGradient(colors: [.white, Color(hue: hue, saturation: 1, brightness: 1)],
                                   startPoint: .leading, endPoint: .trailing)
                    LinearGradient(colors: [.clear, .black],
                                   startPoint: .top, endPoint: .bottom)
                    
                    Circle()
                        .stroke(Color.white, lineWidth: 2)
                        .frame(width: 20, height: 20)
                        .position(x: saturation * geo.size.width,
                                  y: (1 - brightness) * geo.size.height)
                }
                .gesture(DragGesture(minimumDistance: 0).onChanged { value in
                    let x = min(max(0, value.location.x / geo.size.width), 1)
                    let y = min(max(0, value.location.y / geo.size.height), 1)
                    saturation = x
                    brightness = 1 - y
                    updateColor()
                })
            }
            .frame(height: 200)
            .cornerRadius(8)
            
            // Hue slider
            Slider(value: $hue, in: 0...1, onEditingChanged: { _ in updateColor() })
                .accentColor(.clear)
                .background(LinearGradient(
                    gradient: Gradient(colors: stride(from: 0.0, to: 1.0, by: 0.1).map {
                        Color(hue: $0, saturation: 1, brightness: 1)
                    }),
                    startPoint: .leading, endPoint: .trailing
                ).cornerRadius(4))
                .frame(height: 30)
            
            // HEX input
            HStack {
                Text("HEX:")
                TextField("", text: $hex)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .frame(width: 100)
                    .onChange(of: hex) { newVal in
                        if let ui = UIColor(hex: newVal) {
                            selectedColor = Color(ui)
                            var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
                            ui.getHue(&h, saturation: &s, brightness: &b, alpha: &a)
                            hue = Double(h)
                            saturation = Double(s)
                            brightness = Double(b)
                        }
                    }
            }
            
            // Preview
            Rectangle()
                .fill(selectedColor)
                .frame(height: 40)
        }
        .padding()
        .onAppear { updateColor() }
    }
    
    private func updateColor() {
        selectedColor = Color(hue: hue, saturation: saturation, brightness: brightness)
        hex = selectedColor.tohexColor() ?? "#000000"
    }
}


extension UIColor {
    convenience init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if hexSanitized.hasPrefix("#") { hexSanitized.remove(at: hexSanitized.startIndex) }
        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }
        self.init(
            red: CGFloat((rgb & 0xFF0000) >> 16) / 255,
            green: CGFloat((rgb & 0x00FF00) >> 8) / 255,
            blue: CGFloat(rgb & 0x0000FF) / 255,
            alpha: 1
        )
    }
}

// Show on button click
struct HSVColorPickerDemo: View {
    @State private var showSheet = false
    @State private var color = Color.red
    
    var body: some View {
        VStack {
            Button("Pick Color") { showSheet = true }
                .padding()
            Circle().fill(color).frame(width: 60, height: 60)
        }
        .sheet(isPresented: $showSheet) {
            HSVColorPicker(selectedColor: $color)
        }
    }
}
