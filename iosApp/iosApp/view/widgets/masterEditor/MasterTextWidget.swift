import SwiftUI
import UIKit
import shared

struct MasterTextWidget: UIViewRepresentable {

    let state: MasterTextState
    var readOnly: Bool = false
    var scale: CGFloat = 1
    var accessory: AnyView?
    var dismissToken: Int = 0
    var shouldFocus: Bool = false
    var placeholder: String = "Keep your thoughts alive."
    var onFocused: () -> Void = {}
    let onIntent: (MasterTextIntent) -> Void

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var baseSize: CGFloat {
        SmartTextMetrics.baseFontSize * scale * CGFloat(CanvasNode.companion.BASE_FONT_SCALE)
    }

    func makeUIView(context: Context) -> MasterTextUITextView {
        let textView = MasterTextUITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        // scrolls inside the fixed node frame — mirrors Android's verticalScroll(). With this off,
        // sizeThatFits laid out the whole document on every SwiftUI pass and froze the canvas.
        textView.isScrollEnabled = true
        textView.isEditable = !readOnly
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.keyboardDismissMode = .interactive
        textView.tintColor = UIColor(palette.accent)
        textView.dataDetectorTypes = []
        textView.alwaysBounceVertical = false
        textView.showsVerticalScrollIndicator = false
        textView.configure(palette: palette, baseSize: baseSize)

        let doubleTap = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleDoubleTap(_:))
        )
        doubleTap.numberOfTapsRequired = 2
        textView.addGestureRecognizer(doubleTap)

        let singleTap = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleSingleTap(_:))
        )
        singleTap.require(toFail: doubleTap)
        singleTap.cancelsTouchesInView = false
        textView.addGestureRecognizer(singleTap)

        return textView
    }

    func updateUIView(_ uiView: MasterTextUITextView, context: Context) {
        context.coordinator.parent = self
        uiView.isEditable = !readOnly
        uiView.configure(palette: palette, baseSize: baseSize)
        uiView.masterState = state

        let rendered = MasterTextRenderer.attributed(
            state: state,
            palette: palette,
            baseSize: baseSize
        )
        if uiView.attributedText != rendered {
            let previous = uiView.selectedRange
            // assigning attributedText fires textViewDidChangeSelection synchronously; without
            // this flag that delegate publishes new state from inside the SwiftUI update pass
            context.coordinator.isSyncing = true
            uiView.attributedText = rendered
            let limit = (state.text as NSString).length
            uiView.selectedRange = NSRange(
                location: min(previous.location, limit),
                length: min(previous.length, max(limit - min(previous.location, limit), 0))
            )
            context.coordinator.isSyncing = false
            uiView.setNeedsDisplay()
        }
        uiView.showPlaceholder(state.text.isEmpty ? placeholder : nil)
        context.coordinator.syncAccessory(on: uiView, accessory: accessory)
        context.coordinator.syncFirstResponder(uiView)
    }

    /// Takes the frame the canvas node gives it and scrolls internally. Measuring the document
    /// here re-laid out every visible node on every layout pass.
    func sizeThatFits(_ proposal: ProposedViewSize, uiView: MasterTextUITextView, context: Context) -> CGSize? {
        CGSize(
            width: proposal.width ?? UIScreen.main.bounds.width,
            height: proposal.height ?? baseSize * 2
        )
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextViewDelegate {

        var parent: MasterTextWidget
        private var accessoryHost: UIHostingController<AnyView>?
        private var accessoryContainer: SmartTextAccessoryView?
        private var lastDismissToken: Int = 0
        private var claimedFocus = false
        var isSyncing = false

        init(_ parent: MasterTextWidget) {
            self.parent = parent
        }

        /// UIKit's counterpart to Compose's onFocusChanged — the node that gains the caret
        /// becomes the editing node.
        func textViewDidBeginEditing(_ textView: UITextView) {
            guard !isSyncing else { return }
            parent.onFocused()
        }

        func textViewDidChange(_ textView: UITextView) {
            guard !isSyncing else { return }
            let range = textView.selectedRange
            parent.onIntent(
                MasterTextCommands.shared.edit(
                    text: textView.text ?? "",
                    selectionStart: Int32(range.location),
                    selectionEnd: Int32(range.location + range.length)
                )
            )
        }

        func textViewDidChangeSelection(_ textView: UITextView) {
            guard !isSyncing else { return }
            guard textView.text == parent.state.text else { return }
            let range = textView.selectedRange
            parent.onIntent(
                MasterTextCommands.shared.selectionChanged(
                    start: Int32(range.location),
                    end: Int32(range.location + range.length)
                )
            )
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

        @objc func handleDoubleTap(_ recognizer: UITapGestureRecognizer) {
            guard let textView = recognizer.view as? UITextView else { return }
            let point = recognizer.location(in: textView)
            let offset = textView.closestPosition(to: point).map {
                textView.offset(from: textView.beginningOfDocument, to: $0)
            } ?? 0
            parent.onIntent(MasterTextCommands.shared.selectWord(offset: Int32(offset)))
        }

        @objc func handleSingleTap(_ recognizer: UITapGestureRecognizer) {
            guard let textView = recognizer.view as? MasterTextUITextView else { return }
            // read-only means the node is not the editing node yet — a tap claims it,
            // the same way Android's onFocusChanged does
            guard !parent.readOnly else {
                parent.onFocused()
                return
            }
            let point = recognizer.location(in: textView)
            guard let offset = MasterTextRenderer.checklistOffset(
                state: parent.state,
                textView: textView,
                point: point,
                baseSize: parent.baseSize
            ) else { return }
            parent.onIntent(MasterTextCommands.shared.toggleChecked(offset: Int32(offset)))
        }

        func syncFirstResponder(_ textView: UITextView) {
            guard !parent.readOnly else { return }
            if lastDismissToken != parent.dismissToken {
                lastDismissToken = parent.dismissToken
                claimedFocus = true
                if textView.isFirstResponder { textView.resignFirstResponder() }
                return
            }
            guard parent.shouldFocus else {
                claimedFocus = false
                return
            }
            if !textView.isFirstResponder, !claimedFocus {
                claimedFocus = true
                DispatchQueue.main.async { textView.becomeFirstResponder() }
            }
        }

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
                if #available(iOS 16.4, *) { host.safeAreaRegions = [] }
                host.view.insetsLayoutMarginsFromSafeArea = false
                container = SmartTextAccessoryView()
                container.addSubview(host.view)
                NSLayoutConstraint.activate([
                    host.view.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                    host.view.trailingAnchor.constraint(equalTo: container.trailingAnchor),
                    host.view.topAnchor.constraint(equalTo: container.topAnchor),
                    host.view.bottomAnchor.constraint(equalTo: container.bottomAnchor)
                ])
                accessoryHost = host
                accessoryContainer = container
            }
            let fitted = host.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude))
            let grew = container.apply(height: fitted.height)
            if textView.inputAccessoryView !== container {
                textView.inputAccessoryView = container
                if textView.isFirstResponder { textView.reloadInputViews() }
            } else if grew, textView.isFirstResponder {
                textView.reloadInputViews()
            }
        }
    }
}

final class MasterTextUITextView: UITextView {

    var masterState: MasterTextState?
    private var palette: SmartTextPalette = .light
    private var baseSize: CGFloat = 16
    private weak var placeholderLabel: UILabel?

    func configure(palette: SmartTextPalette, baseSize: CGFloat) {
        self.palette = palette
        self.baseSize = baseSize
    }

    func showPlaceholder(_ text: String?) {
        guard let text else {
            placeholderLabel?.removeFromSuperview()
            placeholderLabel = nil
            return
        }
        if let label = placeholderLabel {
            label.text = text
            label.textColor = UIColor(palette.onSurfaceMuted)
            label.font = .systemFont(ofSize: baseSize)
            return
        }
        let label = UILabel()
        label.text = text
        label.textColor = UIColor(palette.onSurfaceMuted)
        label.font = .systemFont(ofSize: baseSize)
        label.numberOfLines = 1
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: leadingAnchor),
            label.topAnchor.constraint(equalTo: topAnchor, constant: 2)
        ])
        placeholderLabel = label
    }

    override func draw(_ rect: CGRect) {
        super.draw(rect)
        guard let state = masterState else { return }
        for entry in MasterTextRenderer.markerFrames(
            state: state,
            textView: self,
            baseSize: baseSize
        ) {
            if entry.paragraph.isChecklist {
                drawCheckbox(at: entry.origin, checked: entry.paragraph.checked)
            } else {
                drawGlyph(entry.paragraph.marker, at: entry.origin, size: entry.size)
            }
        }
    }

    private func drawGlyph(_ glyph: String, at point: CGPoint, size: CGFloat) {
        (glyph as NSString).draw(
            at: point,
            withAttributes: [
                .font: UIFont.systemFont(ofSize: size, weight: .medium),
                .foregroundColor: UIColor(palette.accent)
            ]
        )
    }

    private func drawCheckbox(at point: CGPoint, checked: Bool) {
        let side: CGFloat = 16
        let box = CGRect(x: point.x, y: point.y + 2, width: side, height: side)
        let path = UIBezierPath(roundedRect: box, cornerRadius: 4)
        if checked {
            UIColor(palette.accent).setFill()
            path.fill()
        } else {
            UIColor(palette.onSurfaceMuted).setStroke()
            path.lineWidth = 1.5
            path.stroke()
        }
    }
}
