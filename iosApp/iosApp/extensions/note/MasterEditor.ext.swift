import SwiftUI
import shared

extension CanvasNode: @retroactive Identifiable, @retroactive ObservableObject {}

extension RichBlock: @retroactive Identifiable, @retroactive ObservableObject {}

extension EditableSegment: @retroactive Identifiable, @retroactive ObservableObject {}

extension MasterParagraphLayout: @retroactive Identifiable {}

extension SmartTextRow: @retroactive Identifiable {}

extension CanvasRect {
    var cgRect: CGRect {
        CGRect(x: CGFloat(x), y: CGFloat(y), width: CGFloat(width), height: CGFloat(height))
    }
}

extension CanvasNode {
    var isText: Bool { kind == ContentType.text }

    var canvasFrame: CGRect { rect.cgRect }
}

extension Viewport {
    var cgScale: CGFloat { CGFloat(scale) }

    func canvasPoint(of screenPoint: CGPoint) -> CGPoint {
        CGPoint(
            x: CGFloat(toCanvasX(screenX: Float(screenPoint.x))),
            y: CGFloat(toCanvasY(screenY: Float(screenPoint.y)))
        )
    }
}
