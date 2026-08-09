import SwiftUI
import shared

enum MasterTextSheet: Int, Identifiable {
    case none
    case textStyle
    case textColor
    case backgroundColor
    case fontSize
    case align
    case link

    var id: Int { rawValue }

    var isPresented: Bool { self != .none }

    static func of(action: ToolbarAction) -> MasterTextSheet {
        let commands = SmartTextCommands.shared
        switch Int(MasterTextCommands.shared.sheetIndex(action: action)) {
        case Int(commands.SHEET_TEXT_STYLE): return .textStyle
        case Int(commands.SHEET_TEXT_COLOR): return .textColor
        case Int(commands.SHEET_BACKGROUND_COLOR): return .backgroundColor
        case Int(commands.SHEET_FONT_SIZE): return .fontSize
        case Int(commands.SHEET_ALIGN): return .align
        case Int(commands.SHEET_LINK): return .link
        default: return .none
        }
    }
}

/// Value-driven so it works off MasterTextState. The SmartText host is bound to SmartTextStore
/// and cannot be reused as-is.
struct MasterTextSheetHost: View {

    let sheet: MasterTextSheet
    let toolbar: ToolbarState
    let onIntent: (MasterTextIntent) -> Void
    let onDismiss: () -> Void

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var catalog: SmartTextCatalog { SmartTextCatalog.shared }

    private var commands: MasterTextCommands { MasterTextCommands.shared }

    var body: some View {
        NavigationStack {
            content
                .padding(.horizontal, 20)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .background(palette.surface)
    }

    @ViewBuilder
    private var content: some View {
        switch sheet {
        case .textStyle: styleSheet
        case .fontSize: sizeSheet
        case .align: alignSheet
        case .textColor: colorSheet(title: "Text colour", swatches: catalog.textColors, background: false)
        case .backgroundColor: colorSheet(title: "Highlight", swatches: catalog.highlightColors, background: true)
        case .link: linkSheet
        case .none: EmptyView()
        }
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 17, weight: .semibold))
            .foregroundColor(palette.onSurface)
            .padding(.top, 16)
            .padding(.bottom, 10)
    }

    private func apply(_ intent: MasterTextIntent) {
        onIntent(intent)
        onDismiss()
    }

    private var styleSheet: some View {
        VStack(alignment: .leading, spacing: 0) {
            heading("Text style")
            ScrollView {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(catalog.paragraphStyles, id: \.self) { style in
                        row(
                            label: catalog.paragraphLabel(style: style),
                            selected: style == toolbar.paragraphStyle
                        ) {
                            apply(commands.setParagraphStyle(style: style))
                        }
                    }
                }
            }
        }
    }

    private var sizeSheet: some View {
        VStack(alignment: .leading, spacing: 0) {
            heading("Text size")
            ScrollView {
                VStack(alignment: .leading, spacing: 2) {
                    row(label: "Default", selected: toolbar.fontSize == nil) {
                        apply(commands.setFontSize(size: nil))
                    }
                    ForEach(fontSizes, id: \.self) { size in
                        row(
                            label: "\(Int(size)) pt",
                            selected: toolbar.fontSize?.floatValue == size
                        ) {
                            apply(commands.setFontSize(size: KotlinFloat(float: size)))
                        }
                    }
                }
            }
        }
    }

    private var fontSizes: [Float] { [12, 14, 16, 18, 20, 24, 28, 32, 40, 48] }

    private var alignSheet: some View {
        VStack(alignment: .leading, spacing: 0) {
            heading("Alignment")
            VStack(alignment: .leading, spacing: 2) {
                ForEach(catalog.alignments, id: \.self) { align in
                    row(
                        label: catalog.alignLabel(align: align),
                        selected: align == toolbar.align
                    ) {
                        apply(commands.setAlignment(align: align))
                    }
                }
            }
        }
    }

    private func colorSheet(title: String, swatches: [String], background: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            heading(title)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 12) {
                ForEach(swatches, id: \.self) { hex in
                    Button { pick(hex, background: background) } label: {
                        Circle()
                            .fill(Color(smartTextHex: hex) ?? .gray)
                            .frame(width: 42, height: 42)
                            .overlay(Circle().stroke(palette.divider, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(hex)
                }
            }
            Button("Remove colour") { pick(nil, background: background) }
                .foregroundColor(palette.accent)
            Spacer()
        }
    }

    private func pick(_ hex: String?, background: Bool) {
        apply(
            background
                ? commands.setBackgroundColor(color: hex)
                : commands.setTextColor(color: hex)
        )
    }

    private var linkSheet: some View {
        MasterLinkSheetBody(initial: toolbar.link ?? "", palette: palette) { url in
            apply(commands.setLink(url: url))
        }
    }

    private func row(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .font(.system(size: 16))
                    .foregroundColor(selected ? palette.accent : palette.onSurface)
                Spacer()
                if selected {
                    Image(systemName: "checkmark").foregroundColor(palette.accent)
                }
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
    }
}

private struct MasterLinkSheetBody: View {

    @State private var url: String
    let palette: SmartTextPalette
    let onApply: (String?) -> Void

    init(initial: String, palette: SmartTextPalette, onApply: @escaping (String?) -> Void) {
        _url = State(initialValue: initial)
        self.palette = palette
        self.onApply = onApply
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(url.isEmpty ? "Insert link" : "Edit link")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(palette.onSurface)
                .padding(.top, 16)
            TextField("https://", text: $url)
                .textInputAutocapitalization(.never)
                .keyboardType(.URL)
                .textFieldStyle(.roundedBorder)
            HStack(spacing: 12) {
                Button("Apply") { onApply(url.isEmpty ? nil : url) }
                    .foregroundColor(palette.onAccent)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 8).fill(palette.accent))
                Button("Remove link") { onApply(nil) }
                    .foregroundColor(palette.accent)
            }
            Spacer()
        }
    }
}
