import SwiftUI
import UIKit
import shared

// turns the shared span model into NSAttributedString — the only place that mapping lives on iOS
enum SmartTextStyleMapper {

    private static var catalog: SmartTextCatalog { SmartTextCatalog.shared }

    private static var formats: SmartTextFormats { SmartTextFormats.shared }

    static func baseFont(for block: RichBlock.Text) -> UIFont {
        let size = SmartTextMetrics.baseFontSize * CGFloat(block.style.relativeSize)
        var font = UIFont.systemFont(ofSize: size, weight: uiWeight(Int(block.style.weight)))
        if catalog.isQuote(style: block.style) {
            let descriptor = font.fontDescriptor.withSymbolicTraits(.traitItalic) ?? font.fontDescriptor
            font = UIFont(descriptor: descriptor, size: size)
        }
        // Dynamic Type: the whole editor scales with the reader's text size setting
        return UIFontMetrics(forTextStyle: .body).scaledFont(for: font)
    }

    static func paragraphStyle(for block: RichBlock.Text) -> NSMutableParagraphStyle {
        let style = NSMutableParagraphStyle()
        style.alignment = alignment(block.align)
        style.lineHeightMultiple = CGFloat(block.lineHeight)
        style.paragraphSpacing = CGFloat(block.paragraphSpacing)
        style.firstLineHeadIndent = block.firstLineIndent
            ? SmartTextMetrics.baseFontSize * CGFloat(block.style.relativeSize)
            : 0
        return style
    }

    static func alignment(_ align: TextAlign) -> NSTextAlignment {
        switch catalog.alignKey(align: align) {
        case "CENTER": return .center
        case "END": return .right
        case "JUSTIFY": return .justified
        default: return .natural
        }
    }

    static func swiftUIAlignment(_ align: TextAlign) -> TextAlignment {
        switch catalog.alignKey(align: align) {
        case "CENTER": return .center
        case "END": return .trailing
        default: return .leading
        }
    }

    static func attributed(
        block: RichBlock.Text,
        palette: SmartTextPalette,
        searchRanges: [NSRange],
        activeSearchRange: NSRange?
    ) -> NSAttributedString {
        let font = baseFont(for: block)
        let baseColor = catalog.isCaption(style: block.style) ? palette.onSurfaceMuted : palette.onSurface
        let result = NSMutableAttributedString(
            string: block.text,
            attributes: [
                .font: font,
                .foregroundColor: UIColor(baseColor),
                .paragraphStyle: paragraphStyle(for: block)
            ]
        )

        let length = (block.text as NSString).length
        for span in block.spans {
            let start = min(max(Int(span.start), 0), length)
            let end = min(max(Int(span.end), start), length)
            guard end > start else { continue }
            apply(
                span: span,
                to: result,
                range: NSRange(location: start, length: end - start),
                baseFont: font,
                palette: palette
            )
        }

        for range in searchRanges where NSMaxRange(range) <= length {
            let isActive = activeSearchRange.map { NSEqualRanges($0, range) } ?? false
            result.addAttribute(
                .backgroundColor,
                value: UIColor(isActive ? palette.searchHitActive : palette.searchHit),
                range: range
            )
        }
        return result
    }

    private static func apply(
        span: RichSpan,
        to string: NSMutableAttributedString,
        range: NSRange,
        baseFont: UIFont,
        palette: SmartTextPalette
    ) {
        let set = span.formats
        var traits = baseFont.fontDescriptor.symbolicTraits
        if formats.isBold(formats: set) { traits.insert(.traitBold) }
        if formats.isItalic(formats: set) { traits.insert(.traitItalic) }

        let isSuper = formats.isSuperscript(formats: set)
        let isSub = formats.isSubscript(formats: set)
        let scale: CGFloat = (isSuper || isSub) ? 0.72 : 1
        var size = baseFont.pointSize * scale
        if let explicit = span.fontSize { size = CGFloat(explicit.floatValue) * scale }

        var descriptor = baseFont.fontDescriptor.withSymbolicTraits(traits) ?? baseFont.fontDescriptor
        if let weight = span.fontWeight {
            descriptor = descriptor.addingAttributes([
                .traits: [UIFontDescriptor.TraitKey.weight: uiWeight(Int(weight.int32Value))]
            ])
        }
        var font = UIFont(descriptor: descriptor, size: size)
        if formats.isCode(formats: set) {
            font = UIFont.monospacedSystemFont(ofSize: size, weight: .regular)
        }
        if let family = span.fontFamily, let custom = UIFont(name: family, size: size) {
            font = custom
        }
        string.addAttribute(.font, value: font, range: range)

        if let hex = span.textColor, let color = UIColor(smartTextHex: hex) {
            string.addAttribute(.foregroundColor, value: color, range: range)
        } else if span.link != nil {
            string.addAttribute(.foregroundColor, value: UIColor(palette.accent), range: range)
        }

        if let hex = span.backgroundColor, let color = UIColor(smartTextHex: hex) {
            string.addAttribute(.backgroundColor, value: color, range: range)
        } else if formats.isHighlight(formats: set) {
            string.addAttribute(.backgroundColor, value: UIColor(palette.highlight), range: range)
        } else if formats.isCode(formats: set) {
            string.addAttribute(.backgroundColor, value: UIColor(palette.codeBackground), range: range)
        }

        if formats.isUnderline(formats: set) || span.link != nil {
            string.addAttribute(.underlineStyle, value: NSUnderlineStyle.single.rawValue, range: range)
        }
        if formats.isStrikethrough(formats: set) {
            string.addAttribute(.strikethroughStyle, value: NSUnderlineStyle.single.rawValue, range: range)
        }
        if isSuper {
            string.addAttribute(.baselineOffset, value: baseFont.pointSize * 0.32, range: range)
        }
        if isSub {
            string.addAttribute(.baselineOffset, value: -baseFont.pointSize * 0.2, range: range)
        }
        if let link = span.link, let url = URL(string: link) {
            string.addAttribute(.link, value: url, range: range)
        }
    }

    static func uiWeight(_ weight: Int) -> UIFont.Weight {
        switch weight {
        case ..<400: return .light
        case 400..<500: return .regular
        case 500..<600: return .medium
        case 600..<700: return .semibold
        case 700..<800: return .bold
        default: return .heavy
        }
    }

    static func swiftUIWeight(_ weight: Int) -> Font.Weight {
        switch weight {
        case ..<400: return .light
        case 400..<500: return .regular
        case 500..<600: return .medium
        case 600..<700: return .semibold
        case 700..<800: return .bold
        default: return .heavy
        }
    }

    static func sfSymbol(forIconKey key: String) -> String {
        switch key {
        case "bold": return "bold"
        case "italic": return "italic"
        case "underline": return "underline"
        case "strikethrough": return "strikethrough"
        case "highlight": return "highlighter"
        case "inlineCode": return "chevron.left.forwardslash.chevron.right"
        case "superscript": return "textformat.superscript"
        case "subscript": return "textformat.subscript"
        case "textStyle": return "textformat"
        case "textColor": return "character"
        case "backgroundColor": return "paintpalette"
        case "fontSize": return "textformat.size"
        case "align": return "text.alignleft"
        case "bulletList": return "list.bullet"
        case "numberedList": return "list.number"
        case "checklist": return "checklist"
        case "indent": return "increase.indent"
        case "outdent": return "decrease.indent"
        case "quote": return "text.quote"
        case "codeBlock": return "curlybraces"
        case "divider": return "minus"
        case "table": return "tablecells"
        case "link": return "link"
        case "clearFormatting": return "clear"
        case "undo": return "arrow.uturn.backward"
        case "redo": return "arrow.uturn.forward"
        case "findReplace": return "magnifyingglass"
        default: return "ellipsis"
        }
    }
}
