import SwiftUI
import UIKit
import shared

// one UITextView per block — matches the Compose side so both platforms behave identically
struct SmartTextBlockView: View {

    let block: RichBlock
    let isFocused: Bool
    let listNumber: Int?
    let searchRanges: [NSRange]
    let activeSearchRange: NSRange?
    let readOnly: Bool
    let accessory: AnyView?
    let onIntent: (SmartTextIntent) -> Void

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    var body: some View {
        switch block {
        case let text as RichBlock.Text:
            textBlock(text)
        case let code as RichBlock.Code:
            codeBlock(code)
        case let table as RichBlock.Table:
            tableBlock(table)
        default:
            dividerBlock
        }
    }

    @ViewBuilder
    private func textBlock(_ block: RichBlock.Text) -> some View {
        let catalog = SmartTextCatalog.shared
        let indent = SmartTextMetrics.indentStep * CGFloat(block.listLevel + Int32(block.indent))

        HStack(alignment: .top, spacing: 0) {
            if catalog.isQuote(style: block.style) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(palette.accent)
                    .frame(width: SmartTextMetrics.quoteBarWidth)
                    .padding(.trailing, 10)
            }

            if let marker = block.list {
                listMarker(marker: marker, checked: block.checked)
                    .accessibilityLabel(marker.accessibilityLabel(checked: block.checked))
            }

            SmartTextEditorView(
                block: block,
                isFocused: isFocused,
                searchRanges: searchRanges,
                activeSearchRange: activeSearchRange,
                readOnly: readOnly,
                palette: palette,
                accessory: accessory,
                onIntent: onIntent
            )
            .frame(minHeight: 24)
        }
        .padding(.leading, indent)
        .padding(.bottom, CGFloat(block.paragraphSpacing))
        .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private func listMarker(marker: ListMarker, checked: Bool) -> some View {
        let key = SmartTextCatalog.shared.listKey(style: marker.style)
        Group {
            switch key {
            case "NUMBERED":
                Text("\(listNumber ?? 1).")
                    .font(.system(size: SmartTextMetrics.baseFontSize, weight: .medium))
                    .foregroundColor(palette.accent)

            case "CHECKLIST":
                Button {
                    onIntent(SmartTextCommands.shared.toggleChecked(blockId: block.id))
                } label: {
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(checked ? palette.accent : palette.onSurfaceMuted, lineWidth: 1.5)
                        .background(
                            RoundedRectangle(cornerRadius: 4)
                                .fill(checked ? palette.accent : Color.clear)
                        )
                        .frame(width: 18, height: 18)
                        .overlay(
                            Image(systemName: "checkmark")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(palette.onAccent)
                                .opacity(checked ? 1 : 0)
                        )
                }
                .buttonStyle(.plain)
                .padding(.top, 3)

            default:
                Text(ListEngine.shared.bulletGlyph(level: marker.level))
                    .font(.system(size: SmartTextMetrics.baseFontSize))
                    .foregroundColor(palette.accent)
            }
        }
        .frame(minWidth: 22, alignment: .leading)
        .padding(.trailing, 8)
    }

    private var dividerBlock: some View {
        HStack(spacing: 8) {
            Rectangle().fill(palette.divider).frame(height: 1)
            Circle().fill(palette.accent).frame(width: 4, height: 4)
            Rectangle().fill(palette.divider).frame(height: 1)
        }
        .padding(.vertical, 14)
        .accessibilityLabel("Divider")
    }

    private func codeBlock(_ code: RichBlock.Code) -> some View {
        ZStack(alignment: .topTrailing) {
            ScrollView(.horizontal, showsIndicators: false) {
                SmartTextPlainEditorView(
                    text: code.code,
                    palette: palette,
                    readOnly: readOnly,
                    monospaced: true
                ) { newValue in
                    onIntent(
                        SmartTextCommands.shared.updateCodeBlock(blockId: code.id, code: newValue)
                    )
                }
                .frame(minWidth: 240, minHeight: 44)
                .padding(EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 44))
            }
            Button {
                UIPasteboard.general.string = code.code
            } label: {
                Image(systemName: "doc.on.doc")
                    .font(.system(size: 14))
                    .foregroundColor(palette.onSurfaceMuted)
                    .padding(10)
            }
            .accessibilityLabel("Copy code")
        }
        .background(RoundedRectangle(cornerRadius: 10).fill(palette.codeBackground))
        .padding(.vertical, 6)
        .accessibilityLabel("Code block")
    }

    private func tableBlock(_ table: RichBlock.Table) -> some View {
        ScrollView(.horizontal, showsIndicators: true) {
            VStack(spacing: 0) {
                ForEach(0..<Int(table.data.rowCount), id: \.self) { rowIndex in
                    HStack(spacing: 0) {
                        ForEach(0..<Int(table.data.columnCount), id: \.self) { columnIndex in
                            tableCell(table, rowIndex, columnIndex)
                        }
                    }
                }
            }
        }
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private func tableCell(_ table: RichBlock.Table, _ rowIndex: Int, _ columnIndex: Int) -> some View {
        if let cell = table.data.cellAt(row: Int32(rowIndex), column: Int32(columnIndex)),
           !cell.merged {
            let isHeader = TableEngine.shared.isHeader(
                table: table.data,
                row: Int32(rowIndex),
                column: Int32(columnIndex)
            )
            let width = CGFloat(
                (table.data.columnWidths.indices.contains(columnIndex)
                    ? Float(truncating: table.data.columnWidths[columnIndex])
                    : 120) * Float(cell.columnSpan)
            )
            SmartTextPlainEditorView(
                text: cell.text,
                palette: palette,
                readOnly: readOnly,
                monospaced: false,
                bold: isHeader,
                alignment: SmartTextStyleMapper.alignment(cell.align)
            ) { newValue in
                onIntent(
                    SmartTextCommands.shared.updateTableCell(
                        blockId: table.id,
                        row: Int32(rowIndex),
                        column: Int32(columnIndex),
                        text: newValue
                    )
                )
            }
            .frame(width: width)
            .frame(minHeight: 40)
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
            .background(
                cell.backgroundColor.flatMap { Color(smartTextHex: $0) }
                    ?? (isHeader ? palette.accentSoft : Color.clear)
            )
            .overlay(Rectangle().stroke(palette.divider, lineWidth: 0.5))
            .accessibilityLabel("Row \(rowIndex + 1) column \(columnIndex + 1)")
        }
    }
}

private extension ListMarker {
    func accessibilityLabel(checked: Bool) -> String {
        switch SmartTextCatalog.shared.listKey(style: style) {
        case "NUMBERED": return "Numbered item"
        case "CHECKLIST": return checked ? "Completed task" : "Task"
        default: return "Bullet item"
        }
    }
}
