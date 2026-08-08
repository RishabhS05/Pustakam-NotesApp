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
    let onIntent: (MasterTextIntent) -> Void

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var baseSize: CGFloat { SmartTextMetrics.baseFontSize * scale }

    func makeUIView(context: Context) -> MasterTextUITextView {
        let textView = MasterTextUITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.isScrollEnabled = false
        textView.isEditable = !readOnly
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.keyboardDismissMode = .interactive
        textView.tintColor = UIColor(palette.accent)
        textView.dataDetectorTypes = []
        textView.alwaysBounceVertical = false
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
            uiView.attributedText = rendered
            let limit = (state.text as NSString).length
            uiView.selectedRange = NSRange(
                location: min(previous.location, limit),
                length: min(previous.length, max(limit - min(previous.location, limit), 0))
            )
        }
        uiView.setNeedsDisplay()
        context.coordinator.syncAccessory(on: uiView, accessory: accessory)
        context.coordinator.syncFirstResponder(uiView)
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: MasterTextUITextView, context: Context) -> CGSize? {
        let width = proposal.width ?? UIScreen.main.bounds.width
        let size = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: max(size.height, baseSize * 2))
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextViewDelegate {

        var parent: MasterTextWidget
        private var accessoryHost: UIHostingController<AnyView>?
        private var accessoryContainer: SmartTextAccessoryView?
        private var lastDismissToken: Int = 0
        private var claimedFocus = false

        init(_ parent: MasterTextWidget) {
            self.parent = parent
        }

        func textViewDidChange(_ textView: UITextView) {
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

    func configure(palette: SmartTextPalette, baseSize: CGFloat) {
        self.palette = palette
        self.baseSize = baseSize
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
