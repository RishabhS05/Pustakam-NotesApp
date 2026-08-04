import SwiftUI

struct ThemeTilePicker: View {

    @Binding var selection: ThemeMode
    @Environment(\.palette) private var palette

    private let columns = [GridItem(.adaptive(minimum: 78), spacing: Theme.Spacing.md)]

    var body: some View {
        LazyVGrid(columns: columns, spacing: Theme.Spacing.md) {
            ForEach(ThemeMode.allCases) { mode in
                ThemeTile(
                    mode: mode,
                    isSelected: selection == mode
                ) {
                    // 🎨 spring keeps the selection ring feeling native rather than snapping
                    withAnimation(.snappy(duration: 0.25)) {
                        selection = mode
                    }
                }
            }
        }
    }
}

// 🎨 22-Jul-2026 — one Appearance tile: swatch preview + label + selected ring.
private struct ThemeTile: View {

    let mode: ThemeMode
    let isSelected: Bool
    var onTap: () -> Void

    @Environment(\.palette) private var palette

    /// Miniature of the ramp this tile represents, so AMOLED reads as distinct from Dark.
    private var swatch: LinearGradient {
        switch mode {
        case .system:
            return LinearGradient(
                colors: [Color(hex: "#F2E8D2"), Color(hex: "#16120D")],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        case .light:
            return LinearGradient(
                colors: [Color(hex: "#F2E8D2"), Color(hex: "#E6D7B8")],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        case .dark:
            return LinearGradient(
                colors: [Color(hex: "#221B14"), Color(hex: "#16120D")],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        case .amoled:
            return LinearGradient(
                colors: [Color(hex: "#0C0A07"), Color(hex: "#000000")],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        }
    }

    private var iconTint: Color {
        switch mode {
        case .light: return Theme.Colors.ink
        case .system: return .white
        case .dark, .amoled: return Theme.Colors.saffron
        }
    }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: Theme.Spacing.sm) {

                RoundedRectangle(cornerRadius: Theme.Radius.sm)
                    .fill(swatch)
                    .frame(height: 46)
                    .overlay(
                        Image(systemName: mode.icon)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(iconTint)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Theme.Radius.sm)
                            .stroke(palette.border, lineWidth: 1)
                    )

                Text(mode.title)
                    .font(Theme.Fonts.metaCaption)
                    .foregroundStyle(isSelected ? palette.accent : palette.text2)
            }
            .padding(Theme.Spacing.sm)
            .background(palette.surface)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.md))
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.md)
                    // 🎨 2pt accent ring marks the active tile (spec §6)
                    .stroke(isSelected ? palette.accent : palette.border,
                            lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("\(mode.title) appearance"))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

// MARK: - Previews (sample data)

#Preview("Light") {
    ThemeTilePickerPreviewHost(mode: .light)
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    ThemeTilePickerPreviewHost(mode: .dark)
        .preferredColorScheme(.dark)
}

#Preview("AMOLED") {
    ThemeTilePickerPreviewHost(mode: .amoled)
        .preferredColorScheme(.dark)
}

// 🎨 22-Jul-2026 — preview host wires the palette the same way the app root does.
private struct ThemeTilePickerPreviewHost: View {
    @State var mode: ThemeMode
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = ThemePalette(mode: mode, scheme: scheme)
        return VStack(alignment: .leading, spacing: Theme.Spacing.lg) {
            Text("APPEARANCE")
                .font(Theme.Fonts.metaCaption)
                .foregroundStyle(palette.text3)
            ThemeTilePicker(selection: $mode)
            Text("Selected: \(mode.title)")
                .font(Theme.Fonts.bodyText)
                .foregroundStyle(palette.text2)
        }
        .padding(Theme.Spacing.gutter)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(palette.background)
        .environment(\.palette, palette)
    }
}
