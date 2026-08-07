import SwiftUI
import shared

struct SmartTextSheetHost: View {

    @ObservedObject var store: SmartTextStore

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var commands: SmartTextCommands { SmartTextCommands.shared }

    private var catalog: SmartTextCatalog { SmartTextCatalog.shared }

    var body: some View {
        NavigationStack {
            content
                .padding(.horizontal, 20)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .background(palette.surface)
    }

    @ViewBuilder
    private var content: some View {
        switch store.sheet {
        case .textStyle: textStyleSheet
        case .textColor: colorSheet(title: "Text colour", palette: catalog.textColors, isBackground: false)
        case .backgroundColor:
            colorSheet(title: "Background colour", palette: catalog.highlightColors, isBackground: true)
        case .fontSize: fontSizeSheet
        case .align: alignSheet
        case .table: tableSheet
        case .link: linkSheet
        case .findReplace: findReplaceSheet
        case .none: EmptyView()
        }
    }

    private func title(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 17, weight: .semibold))
            .foregroundColor(palette.onSurface)
            .padding(.top, 16)
            .padding(.bottom, 10)
    }

    private var textStyleSheet: some View {
        VStack(alignment: .leading, spacing: 0) {
            title("Text style")
            ScrollView {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(catalog.paragraphStyles, id: \.self) { style in
                        let selected = style == store.toolbar.paragraphStyle
                        Button {
                            store.dispatch(commands.setParagraphStyle(style: style))
                            store.sheet = .none
                        } label: {
                            Text(catalog.paragraphLabel(style: style))
                                .font(
                                    .system(
                                        size: 15 * CGFloat(min(style.relativeSize, 1.6)),
                                        weight: SmartTextStyleMapper.swiftUIWeight(Int(style.weight))
                                    )
                                )
                                .foregroundColor(selected ? palette.accent : palette.onSurface)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 10)
                                .background(
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(selected ? palette.accentSoft : .clear)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func colorSheet(title text: String, palette colors: [String], isBackground: Bool) -> some View {
        ColorSheetBody(
            title: text,
            swatches: colors,
            palette: palette,
            onPick: { hex in
                store.dispatch(
                    isBackground
                        ? commands.setBackgroundColor(color: hex)
                        : commands.setTextColor(color: hex)
                )
                store.sheet = .none
            }
        )
    }

    private var fontSizeSheet: some View {
        FontSizeSheetBody(
            initial: store.toolbar.fontSize.map { CGFloat($0.floatValue) } ?? 16,
            palette: palette,
            onApply: { size in
                store.dispatch(commands.setFontSize(size: size.map { KotlinFloat(float: Float($0)) }))
                store.sheet = .none
            }
        )
    }

    private var alignSheet: some View {
        VStack(alignment: .leading, spacing: 0) {
            title("Alignment")
            ForEach(catalog.alignments, id: \.self) { align in
                let selected = align == store.toolbar.align
                Button {
                    store.dispatch(commands.setAlignment(align: align))
                    store.sheet = .none
                } label: {
                    Text(catalog.alignLabel(align: align))
                        .font(.system(size: 15))
                        .foregroundColor(selected ? palette.accent : palette.onSurface)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(selected ? palette.accentSoft : .clear)
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var tableSheet: some View {
        let rows = Int(store.focusedTable?.data.rowCount ?? 0)
        let columns = Int(store.focusedTable?.data.columnCount ?? 0)

        return VStack(alignment: .leading, spacing: 0) {
            title("Table")
            HStack(spacing: 10) {
                ForEach(0..<catalog.tablePresetRows.count, id: \.self) { index in
                    let presetRows = Int(truncating: catalog.tablePresetRows[index])
                    let presetColumns = Int(truncating: catalog.tablePresetColumns[index])
                    Button("\(presetRows) × \(presetColumns)") {
                        store.dispatch(
                            commands.insertTable(
                                rows: Int32(presetRows),
                                columns: Int32(presetColumns)
                            )
                        )
                        store.sheet = .none
                    }
                    .foregroundColor(palette.accent)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 8).fill(palette.accentSoft))
                }
            }
            .padding(.bottom, 16)

            if store.focusedTable != nil {
                tableRow("Add row") { commands.tableAddRow(at: Int32(rows)) }
                tableRow("Add column") { commands.tableAddColumn(at: Int32(columns)) }
                tableRow("Delete last row") { commands.tableDeleteRow(at: Int32(rows - 1)) }
                tableRow("Delete last column") { commands.tableDeleteColumn(at: Int32(columns - 1)) }
                tableRow("Toggle header row") { commands.tableToggleHeaderRow() }
                tableRow("Toggle header column") { commands.tableToggleHeaderColumn() }
            }
        }
    }

    private func tableRow(_ label: String, command: @escaping () -> TableCommand) -> some View {
        Button {
            guard let table = store.focusedTable else { return }
            store.dispatch(commands.tableAction(blockId: table.id, command: command()))
        } label: {
            Text(label)
                .font(.system(size: 15))
                .foregroundColor(palette.onSurface)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
    }

    private var linkSheet: some View {
        LinkSheetBody(
            initial: store.toolbar.link ?? "",
            hasExisting: store.toolbar.link != nil,
            palette: palette,
            onApply: { url in
                store.dispatch(commands.setLink(url: url))
                store.sheet = .none
            }
        )
    }

    private var findReplaceSheet: some View {
        VStack(alignment: .leading, spacing: 10) {
            title("Find and replace")
            TextField(
                "Find",
                text: Binding(
                    get: { store.state.search.query },
                    set: { store.dispatch(commands.setSearchQuery(query: $0, matchCase: false)) }
                )
            )
            .textFieldStyle(.roundedBorder)

            Text(
                store.state.search.matches.isEmpty
                    ? "No matches"
                    : "\(Int(store.state.search.currentIndex) + 1) of \(store.state.search.matches.count)"
            )
            .font(.system(size: 13))
            .foregroundColor(palette.onSurfaceMuted)

            TextField(
                "Replace with",
                text: Binding(
                    get: { store.state.search.replacement },
                    set: { store.dispatch(commands.setReplacement(replacement: $0)) }
                )
            )
            .textFieldStyle(.roundedBorder)

            HStack(spacing: 8) {
                Button("Previous") { store.dispatch(commands.findPrevious()) }
                Button("Next") { store.dispatch(commands.findNext()) }
                Button("Replace") { store.dispatch(commands.replaceCurrent()) }
                Button("All") { store.dispatch(commands.replaceAll()) }
            }
            .foregroundColor(palette.accent)
            .padding(.top, 6)
            Spacer()
        }
    }
}

private struct FontSizeSheetBody: View {

    @State var size: CGFloat
    let palette: SmartTextPalette
    let onApply: (CGFloat?) -> Void

    init(initial: CGFloat, palette: SmartTextPalette, onApply: @escaping (CGFloat?) -> Void) {
        _size = State(initialValue: initial)
        self.palette = palette
        self.onApply = onApply
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Font size")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(palette.onSurface)
                .padding(.top, 16)
            Text("\(Int(size)) pt")
                .font(.system(size: 14))
                .foregroundColor(palette.onSurfaceMuted)
            Slider(value: $size, in: 10...48, step: 1) { editing in
                if !editing { onApply(size) }
            }
            .tint(palette.accent)
            Button("Reset to default") { onApply(nil) }
                .foregroundColor(palette.accent)
            Spacer()
        }
    }
}

private struct LinkSheetBody: View {

    @State var url: String
    let hasExisting: Bool
    let palette: SmartTextPalette
    let onApply: (String?) -> Void

    init(
        initial: String,
        hasExisting: Bool,
        palette: SmartTextPalette,
        onApply: @escaping (String?) -> Void
    ) {
        _url = State(initialValue: initial)
        self.hasExisting = hasExisting
        self.palette = palette
        self.onApply = onApply
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(hasExisting ? "Edit link" : "Insert link")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(palette.onSurface)
                .padding(.top, 16)
            TextField("https://", text: $url)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
            HStack(spacing: 12) {
                Button("Apply") { onApply(url.isEmpty ? nil : url) }
                    .foregroundColor(palette.onAccent)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 8).fill(palette.accent))
                if hasExisting {
                    Button("Remove link") { onApply(nil) }
                        .foregroundColor(palette.accent)
                }
            }
            Spacer()
        }
    }
}


/// Quick swatches plus the ColorSelector already used by Create Tag.
private struct ColorSheetBody: View {

    let title: String
    let swatches: [String]
    let palette: SmartTextPalette
    let onPick: (String?) -> Void

    @State private var picked: Color = .orange

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(palette.onSurface)
                .padding(.top, 16)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 12) {
                ForEach(swatches, id: \.self) { hex in
                    Button { onPick(hex) } label: {
                        Circle()
                            .fill(Color(smartTextHex: hex) ?? .gray)
                            .frame(width: 42, height: 42)
                            .overlay(Circle().stroke(palette.divider, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(hex)
                }
            }

            ScrollView {
                ColorSelector(selectedColor: $picked)
            }

            HStack(spacing: 12) {
                Button("Apply") { onPick(picked.tohexColor()) }
                    .foregroundColor(palette.onAccent)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 8).fill(palette.accent))
                Button("Remove colour") { onPick(nil) }
                    .foregroundColor(palette.accent)
            }
            .padding(.bottom, 16)
        }
    }
}
