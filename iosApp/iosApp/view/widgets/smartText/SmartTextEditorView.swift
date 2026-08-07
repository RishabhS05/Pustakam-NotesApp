import SwiftUI
import UIKit
import shared

// rich block editor: attributed rendering in, intents out
struct SmartTextEditorView: UIViewRepresentable {

    let block: RichBlock.Text
    let isFocused: Bool
    let searchRanges: [NSRange]
    let activeSearchRange: NSRange?
    let readOnly: Bool
    let palette: SmartTextPalette
    // rendered into the keyboard's inputAccessoryView, which is what pins it above the keyboard
    let accessory: AnyView?
    let onIntent: (SmartTextIntent) -> Void

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.isScrollEnabled = false
        textView.isEditable = !readOnly
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.keyboardDismissMode = .interactive
        textView.tintColor = UIColor(palette.accent)
        textView.adjustsFontForContentSizeCategory = true
        textView.dataDetectorTypes = []
        textView.setContentCompressionResistancePriority(.required, for: .vertical)
        textView.setContentHuggingPriority(.required, for: .vertical)
        return textView
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        context.coordinator.parent = self
        uiView.isEditable = !readOnly
        context.coordinator.syncAccessory(on: uiView, accessory: accessory)

        let rendered = SmartTextStyleMapper.attributed(
            block: block,
            palette: palette,
            searchRanges: searchRanges,
            activeSearchRange: activeSearchRange
        )
        // only reassign when something actually changed — assigning resets the caret
        if uiView.attributedText != rendered {
            let previous = uiView.selectedRange
            uiView.attributedText = rendered
            let limit = (block.text as NSString).length
            uiView.selectedRange = NSRange(
                location: min(previous.location, limit),
                length: min(previous.length, max(limit - min(previous.location, limit), 0))
            )
        }

        if block.text.isEmpty {
            context.coordinator.showPlaceholder(on: uiView, palette: palette, block: block)
        } else {
            context.coordinator.hidePlaceholder()
        }

        if isFocused, !uiView.isFirstResponder, !readOnly {
            DispatchQueue.main.async { uiView.becomeFirstResponder() }
        }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
        let width = proposal.width ?? UIScreen.main.bounds.width
        let size = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: max(size.height, 24))
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextViewDelegate {

        var parent: SmartTextEditorView
        private weak var placeholderLabel: UILabel?
        private var accessoryHost: UIHostingController<AnyView>?

        init(_ parent: SmartTextEditorView) {
            self.parent = parent
        }

        /// Hosts the SwiftUI toolbar in the keyboard accessory, re-measuring only when its
        /// height changes — expanding the tray is the one case that needs reloadInputViews().
        func syncAccessory(on textView: UITextView, accessory: AnyView?) {
            guard let accessory else {
                textView.inputAccessoryView = nil
                accessoryHost = nil
                return
            }
            let width = UIScreen.main.bounds.width
            let host: UIHostingController<AnyView>
            if let existing = accessoryHost {
                host = existing
                host.rootView = accessory
            } else {
                host = UIHostingController(rootView: accessory)
                host.view.backgroundColor = .clear
                accessoryHost = host
            }
            let fitted = host.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude))
            let previousHeight = host.view.frame.height
            host.view.frame = CGRect(x: 0, y: 0, width: width, height: fitted.height)
            if textView.inputAccessoryView !== host.view {
                textView.inputAccessoryView = host.view
                if textView.isFirstResponder { textView.reloadInputViews() }
            } else if abs(previousHeight - fitted.height) > 0.5, textView.isFirstResponder {
                textView.reloadInputViews()
            }
        }

        func textViewDidChange(_ textView: UITextView) {
            let caret = textView.selectedRange.location
            parent.onIntent(
                SmartTextCommands.shared.typeText(
                    blockId: parent.block.id,
                    text: textView.text ?? "",
                    caret: Int32(caret)
                )
            )
        }

        func textViewDidChangeSelection(_ textView: UITextView) {
            // typeText already positions the caret; a selection event raised by that same edit
            // would undo the caret move a markdown shortcut just made
            guard textView.text == parent.block.text else { return }
            let range = textView.selectedRange
            parent.onIntent(
                SmartTextCommands.shared.selectionChanged(
                    blockId: parent.block.id,
                    start: Int32(range.location),
                    end: Int32(range.location + range.length)
                )
            )
        }

        func textView(
            _ textView: UITextView,
            shouldChangeTextIn range: NSRange,
            replacementText text: String
        ) -> Bool {
            // return splits the block; backspace at offset 0 outdents then merges upward
            if text == "\n" {
                parent.onIntent(
                    SmartTextCommands.shared.splitBlock(
                        blockId: parent.block.id,
                        caret: Int32(range.location)
                    )
                )
                return false
            }
            if text.isEmpty, range.length == 0, range.location == 0 {
                parent.onIntent(
                    SmartTextCommands.shared.mergeWithPrevious(blockId: parent.block.id)
                )
                return false
            }
            return true
        }

        func textView(
            _ textView: UITextView,
            shouldInteractWith URL: URL,
            in characterRange: NSRange,
            interaction: UITextItemInteraction
        ) -> Bool {
            UIApplication.shared.open(URL)
            return false
        }

        func showPlaceholder(on textView: UITextView, palette: SmartTextPalette, block: RichBlock.Text) {
            let label = placeholderLabel ?? {
                let created = UILabel()
                created.numberOfLines = 1
                textView.addSubview(created)
                created.translatesAutoresizingMaskIntoConstraints = false
                NSLayoutConstraint.activate([
                    created.leadingAnchor.constraint(equalTo: textView.leadingAnchor),
                    created.topAnchor.constraint(equalTo: textView.topAnchor)
                ])
                placeholderLabel = created
                return created
            }()
            label.text = SmartTextEditorView.placeholder(for: block)
            label.font = SmartTextStyleMapper.baseFont(for: block)
            label.textColor = UIColor(palette.onSurfaceMuted)
            label.isHidden = false
        }

        func hidePlaceholder() {
            placeholderLabel?.isHidden = true
        }
    }

    static func placeholder(for block: RichBlock.Text) -> String {
        let catalog = SmartTextCatalog.shared
        if block.list != nil { return "List item" }
        if catalog.isQuote(style: block.style) { return "Quote" }
        if catalog.isTitleLike(style: block.style) { return "Heading" }
        return "Keep your thoughts alive."
    }
}

// plain editor used by code blocks and table cells
struct SmartTextPlainEditorView: UIViewRepresentable {

    let text: String
    let palette: SmartTextPalette
    let readOnly: Bool
    var monospaced: Bool = false
    var bold: Bool = false
    var alignment: NSTextAlignment = .natural
    let onChange: (String) -> Void

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.isScrollEnabled = false
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.tintColor = UIColor(palette.accent)
        textView.adjustsFontForContentSizeCategory = true
        return textView
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        context.coordinator.parent = self
        uiView.isEditable = !readOnly
        uiView.textAlignment = alignment
        uiView.textColor = UIColor(palette.onSurface)
        uiView.font = monospaced
            ? UIFont.monospacedSystemFont(ofSize: SmartTextMetrics.codeFontSize, weight: .regular)
            : UIFont.systemFont(
                ofSize: SmartTextMetrics.baseFontSize,
                weight: bold ? .semibold : .regular
            )
        if uiView.text != text { uiView.text = text }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
        let width = proposal.width ?? 240
        let size = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: max(size.height, 20))
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextViewDelegate {

        var parent: SmartTextPlainEditorView

        init(_ parent: SmartTextPlainEditorView) {
            self.parent = parent
        }

        func textViewDidChange(_ textView: UITextView) {
            parent.onChange(textView.text ?? "")
        }
    }
}
