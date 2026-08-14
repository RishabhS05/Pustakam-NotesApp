import SwiftUI
import UIKit
import shared

enum MasterTextRenderer {

    static let indentStep: CGFloat = 20
    static let markerGutter: CGFloat = 26
    static let bulletScale: CGFloat = 2
    static let numberScale: CGFloat = 0.8
    static let checkboxSide: CGFloat = 18
    static let xHeightRatio: CGFloat = 0.32
    static let numberTrail: CGFloat = 4

    static func attributed(
        state: MasterTextState,
        palette: SmartTextPalette,
        baseSize: CGFloat
    ) -> NSAttributedString {
        let text = state.text
        let result = NSMutableAttributedString(string: text)
        let length = (text as NSString).length
        let paragraphs = MasterTextLayout.shared.paragraphs(state: state)

        for paragraph in paragraphs {
            let start = min(max(Int(paragraph.start), 0), length)
            let end = min(max(Int(paragraph.end), start), length)
            guard end >= start else { continue }
            let range = NSRange(location: start, length: end - start)
            guard NSMaxRange(range) <= length else { continue }

            let size = baseSize * CGFloat(paragraph.style.relativeSize)
            let indent = CGFloat(paragraph.indentLevel) * indentStep
                + (paragraph.hasMarker ? markerGutter : 0)

            let style = NSMutableParagraphStyle()
            style.alignment = SmartTextStyleMapper.alignment(paragraph.align)
            style.lineHeightMultiple = CGFloat(paragraph.lineHeight)
            style.paragraphSpacing = CGFloat(paragraph.paragraphSpacing)
            style.headIndent = indent
            style.firstLineHeadIndent = paragraph.firstLineIndent ? indent + 24 : indent

            let isCaption = SmartTextCatalog.shared.isCaption(style: paragraph.style)
            result.addAttributes(
                [
                    .font: UIFont.systemFont(
                        ofSize: size,
                        weight: SmartTextStyleMapper.uiWeight(Int(paragraph.style.weight))
                    ),
                    .foregroundColor: UIColor(isCaption ? palette.onSurfaceMuted : palette.onSurface),
                    .paragraphStyle: style
                ],
                range: range
            )
        }

        for span in MasterTextLayout.shared.flatSpans(state: state) {
            let start = min(max(Int(span.start), 0), length)
            let end = min(max(Int(span.end), start), length)
            guard end > start else { continue }
            applySpan(
                span,
                to: result,
                range: NSRange(location: start, length: end - start),
                palette: palette,
                baseSize: baseSize
            )
        }
        return result
    }

    private static func applySpan(
        _ span: RichSpan,
        to string: NSMutableAttributedString,
        range: NSRange,
        palette: SmartTextPalette,
        baseSize: CGFloat
    ) {
        let formats = SmartTextFormats.shared
        let set = span.formats
        let existing = string.attribute(.font, at: range.location, effectiveRange: nil) as? UIFont
        let base = existing ?? UIFont.systemFont(ofSize: baseSize)

        var traits = base.fontDescriptor.symbolicTraits
        if formats.isBold(formats: set) { traits.insert(.traitBold) }
        if formats.isItalic(formats: set) { traits.insert(.traitItalic) }

        let isSuper = formats.isSuperscript(formats: set)
        let isSub = formats.isSubscript(formats: set)
        let scale: CGFloat = (isSuper || isSub) ? 0.72 : 1
        var size = base.pointSize * scale
        if let explicit = span.fontSize { size = CGFloat(explicit.floatValue) * scale }

        var descriptor = base.fontDescriptor.withSymbolicTraits(traits) ?? base.fontDescriptor
        if let weight = span.fontWeight {
            descriptor = descriptor.addingAttributes([
                .traits: [UIFontDescriptor.TraitKey.weight: SmartTextStyleMapper.uiWeight(Int(weight.int32Value))]
            ])
        }
        var font = UIFont(descriptor: descriptor, size: size)
        if formats.isCode(formats: set) {
            font = UIFont.monospacedSystemFont(ofSize: size, weight: .regular)
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
        if isSuper { string.addAttribute(.baselineOffset, value: base.pointSize * 0.32, range: range) }
        if isSub { string.addAttribute(.baselineOffset, value: -base.pointSize * 0.2, range: range) }
        if let link = span.link, let url = URL(string: link) {
            string.addAttribute(.link, value: url, range: range)
        }
    }

    static func markerFrames(
        state: MasterTextState,
        textView: UITextView,
        baseSize: CGFloat
    ) -> [(paragraph: MasterParagraphLayout, origin: CGPoint, size: CGFloat)] {
        let length = (textView.text as NSString).length
        return MasterTextLayout.shared.paragraphs(state: state).compactMap { paragraph in
            guard paragraph.hasMarker else { return nil }
            let offset = min(max(Int(paragraph.start), 0), max(length - 1, 0))
            guard length > 0 else { return nil }
            let glyphRange = textView.layoutManager.glyphRange(
                forCharacterRange: NSRange(location: offset, length: 0),
                actualCharacterRange: nil
            )
            var rect = textView.layoutManager.lineFragmentRect(
                forGlyphAt: glyphRange.location,
                effectiveRange: nil
            )
            rect.origin.x += textView.textContainerInset.left
            rect.origin.y += textView.textContainerInset.top
            let indent = CGFloat(paragraph.indentLevel) * indentStep
            let fontSize = baseSize * CGFloat(paragraph.style.relativeSize)
            let markerSize = fontSize
                * (paragraph.listStyle == ListStyle.bullet ? bulletScale : numberScale)
            let font = UIFont.systemFont(ofSize: markerSize, weight: .medium)
            // markers sit on the text baseline, not centred in the line box, or they read low
            let baseline = rect.minY + textView.layoutManager.location(
                forGlyphAt: glyphRange.location
            ).y
            let isBullet = paragraph.listStyle == ListStyle.bullet

            if paragraph.isChecklist {
                let box = checkboxSide
                return (
                    paragraph,
                    CGPoint(
                        x: rect.minX + indent + (markerGutter - box) / 2,
                        y: baseline - fontSize * xHeightRatio - box / 2
                    ),
                    markerSize
                )
            }

            let width = (paragraph.marker as NSString)
                .size(withAttributes: [.font: font]).width
            let x = isBullet
                ? rect.minX + indent + (markerGutter - width) / 2
                : rect.minX + indent + markerGutter - width - numberTrail
            let y: CGFloat = isBullet
                ? baseline - (font.ascender + (font.descender * 0.5))
                : baseline - font.ascender
            return (
                paragraph,
                CGPoint(x: x, y: y),
                markerSize
            )
        }
    }

    static func checklistOffset(
        state: MasterTextState,
        textView: UITextView,
        point: CGPoint,
        baseSize: CGFloat
    ) -> Int? {
        for entry in markerFrames(state: state, textView: textView, baseSize: baseSize) {
            guard entry.paragraph.isChecklist else { continue }
            let box = CGRect(
                x: entry.origin.x,
                y: entry.origin.y,
                width: markerGutter,
                height: entry.size * 1.4
            )
            if box.contains(point) { return Int(entry.paragraph.start) }
        }
        return nil
    }
}
