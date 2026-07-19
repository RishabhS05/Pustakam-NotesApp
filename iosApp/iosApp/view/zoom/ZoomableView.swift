// 🔧 19-Jul-2026: NEW — reusable pinch-zoom container (zoom feature request).
//   Pinch 1x..5x, drag-pan while zoomed (clamped), double-tap toggles 1x ↔ 2.5x.
//   Single-finger drags at 1x are NOT captured, so the page-curl flip keeps working.
//   Used by the book reader's image/PDF pages (DRY).

import SwiftUI

struct ZoomableView<Content: View>: View {
    @ViewBuilder let content: () -> Content

    private static var minZoom: CGFloat { 1 }
    private static var maxZoom: CGFloat { 5 }
    private static var doubleTapZoom: CGFloat { 2.5 }

    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        GeometryReader { proxy in
            let size = proxy.size

            // 🔧 19-Jul-2026: pan never reveals empty space — clamp to (scale-1) * half size
            func clamped(_ proposed: CGSize) -> CGSize {
                let maxX = (scale - 1) * size.width / 2
                let maxY = (scale - 1) * size.height / 2
                return CGSize(width: min(max(proposed.width, -maxX), maxX),
                              height: min(max(proposed.height, -maxY), maxY))
            }

            let pinch = MagnificationGesture()
                .onChanged { value in
                    scale = min(max(lastScale * value, Self.minZoom), Self.maxZoom)
                    offset = clamped(offset)
                }
                .onEnded { _ in
                    lastScale = scale
                    if scale <= Self.minZoom { offset = .zero; lastOffset = .zero }
                }

            // pan only participates while zoomed — at 1x the page-curl gets the drag
            let pan = DragGesture(minimumDistance: 1)
                .onChanged { value in
                    guard scale > Self.minZoom else { return }
                    offset = clamped(CGSize(width: lastOffset.width + value.translation.width,
                                            height: lastOffset.height + value.translation.height))
                }
                .onEnded { _ in lastOffset = offset }

            content()
                .frame(width: size.width, height: size.height)
                .scaleEffect(scale)
                .offset(offset)
                .gesture(pinch)
                .simultaneousGesture(scale > Self.minZoom ? pan : nil)
                .onTapGesture(count: 2) { location in
                    withAnimation(.easeInOut(duration: 0.2)) {
                        if scale > Self.minZoom {
                            scale = Self.minZoom; lastScale = Self.minZoom
                            offset = .zero; lastOffset = .zero
                        } else {
                            scale = Self.doubleTapZoom; lastScale = Self.doubleTapZoom
                            let proposed = CGSize(width: (size.width / 2 - location.x) * (Self.doubleTapZoom - 1),
                                                  height: (size.height / 2 - location.y) * (Self.doubleTapZoom - 1))
                            offset = clamped(proposed); lastOffset = offset
                        }
                    }
                }
                .clipped()
        }
    }
}
