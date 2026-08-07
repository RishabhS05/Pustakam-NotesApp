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
    let dismissToken: Int
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

        syncFirstResponder(uiView, coordinator: context.coordinator)
    }

    /**
     Focus is claimed ONCE per block, not on every update.

     The old `if isFocused && !isFirstResponder { becomeFirstResponder() }` ran on every render,
     so anything that resigned the keyboard — the dismiss button, opening the colour picker — was
     undone by the very next render and the keyboard bounced straight back.
     */
    private func syncFirstResponder(_ textView: UITextView, coordinator: Coordinator) {
        guard !readOnly else { return }

        if coordinator.lastDismissToken != dismissToken {
            coordinator.lastDismissToken = dismissToken
            coordinator.claimedFocusForBlockId = block.id   // do not re-claim after dismissing
            if textView.isFirstResponder { textView.resignFirstResponder() }
            return
        }

        if !isFocused {
            coordinator.claimedFocusForBlockId = nil
            return
        }

        if !textView.isFirstResponder, coordinator.claimedFocusForBlockId != block.id {
            coordinator.claimedFocusForBlockId = block.id
            DispatchQueue.main.async { textView.becomeFirstResponder() }
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
        private var accessoryContainer: SmartTextAccessoryView?
        var lastDismissToken: Int = 0
        var claimedFocusForBlockId: String?

        init(_ parent: SmartTextEditorView) {
            self.parent = parent
        }

        /// Hosts the SwiftUI toolbar in the keyboard accessory, re-measuring only when its
        /// height changes — expanding the tray is the one case that needs reloadInputViews().
        func syncAccessory(on textView: UITextView, accessory: AnyView?) {
            guard let accessory else {
                textView.inputAccessoryView = nil
                accessoryHost = nil
                accessoryContainer = nil
                return
            }
            let width = UIScreen.main.bounds.width

            let host: UIHostingController<AnyView>
            let container: SmartTextAccessoryView
            if let existingHost = accessoryHost, let existingContainer = accessoryContainer {
                host = existingHost
                container = existingContainer
                host.rootView = accessory
            } else {
                host = UIHostingController(rootView: accessory)
                host.view.backgroundColor = .clear
                host.view.translatesAutoresizingMaskIntoConstraints = false
                // a hosting controller adds the bottom safe-area inset by default, which measured
                // taller than the bar actually draws and left a gap above the keyboard
                if #available(iOS 16.4, *) { host.safeAreaRegions = [] }
                host.view.insetsLayoutMarginsFromSafeArea = false
                container = SmartTextAccessoryView()
                container.addSubview(host.view)
                // all four edges: the container height is the single source of truth, so the
                // SwiftUI content can never be shorter than the bar and leave a gap
                NSLayoutConstraint.activate([
                    host.view.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                    host.view.trailingAnchor.constraint(equalTo: container.trailingAnchor),
                    host.view.topAnchor.constraint(equalTo: container.topAnchor),
                    host.view.bottomAnchor.constraint(equalTo: container.bottomAnchor)
                ])
                accessoryHost = host
                accessoryContainer = container
            }

            let fitted = host.sizeThatFits(
                in: CGSize(width: width, height: .greatestFiniteMagnitude)
            )
            let grew = container.apply(height: fitted.height)

            if textView.inputAccessoryView !== container {
                textView.inputAccessoryView = container
                if textView.isFirstResponder { textView.reloadInputViews() }
            } else if grew, textView.isFirstResponder {
                // expanding More makes the bar taller and can cover the caret
                textView.reloadInputViews()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak textView] in
                    guard let textView else { return }
                    self.scrollCaretIntoView(textView, clearance: fitted.height)
                }
            }
        }

        /// Our text view does not scroll itself, so this walks up to whichever scroll view
        /// actually holds the note and lifts the caret clear of the accessory.
        func scrollCaretIntoView(_ textView: UITextView, clearance: CGFloat) {
            guard let scrollView = textView.enclosingScrollView(),
                  let range = textView.selectedTextRange else { return }
            let caret = textView.convert(textView.caretRect(for: range.end), to: scrollView)
            let target = caret.insetBy(dx: 0, dy: -(clearance + 24))
            scrollView.scrollRectToVisible(target, animated: true)
        }

        // the caret must stay above the keyboard as soon as editing starts, and as text grows
        func textViewDidBeginEditing(_ textView: UITextView) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak textView] in
                guard let textView else { return }
                self.scrollCaretIntoView(textView, clearance: self.accessoryHeight)
            }
        }

        private var accessoryHeight: CGFloat {
            accessoryContainer?.intrinsicContentSize.height ?? 0
        }

        func textViewDidChange(_ textView: UITextView) {
            scrollCaretIntoView(textView, clearance: accessoryHeight)
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

/**
 A keyboard accessory only grows if it reports an intrinsic height AND carries
 `.flexibleHeight`. Without both, UIKit keeps the bar at its original height and the expanded
 tray renders outside those bounds — which is what put it behind the keyboard.
 */
final class SmartTextAccessoryView: UIView {

    private var measuredHeight: CGFloat = 0

    override var intrinsicContentSize: CGSize {
        CGSize(width: UIView.noIntrinsicMetric, height: measuredHeight)
    }

    init() {
        super.init(frame: .zero)
        autoresizingMask = .flexibleHeight
        backgroundColor = .clear
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    /// Returns true when the height actually changed and the input views need reloading.
    @discardableResult
    func apply(height: CGFloat) -> Bool {
        guard abs(measuredHeight - height) > 0.5 else { return false }
        measuredHeight = height
        frame.size.height = height
        invalidateIntrinsicContentSize()
        return true
    }
}

extension UIView {
    /// Nearest ancestor scroll view — the SwiftUI ScrollView that wraps the note editor.
    func enclosingScrollView() -> UIScrollView? {
        var candidate: UIView? = superview
        while let view = candidate {
            if let scrollView = view as? UIScrollView { return scrollView }
            candidate = view.superview
        }
        return nil
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
