import SwiftUI
import shared

private struct MasterNodePlacement: Identifiable {

    let node: CanvasNode
    let title: String
    let frame: CGRect
    let isEditing: Bool
    let isSelected: Bool
    let dragEnabled: Bool

    var id: String { node.id }
}

struct MasterCanvas<NodeContent: View>: View {

    let state: CanvasEditorState
    let onIntent: (CanvasEditorIntent) -> Void
    var onRename: (String, String) -> Void = { _, _ in }
    @ViewBuilder let nodeContent: (CanvasNode, Bool) -> NodeContent

    @Environment(\.colorScheme) private var scheme
    @State private var liveZoom: CGFloat = 1
    @State private var lastPan: CGSize = .zero
    @State private var lastResize: CGSize = .zero

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var commands: CanvasCommands { CanvasCommands.shared }

    private var isHandTool: Bool { commands.isHandTool(state: state) }

    var body: some View {
        GeometryReader { geometry in
            canvas(size: geometry.size)
        }
    }

    /// The scaling layer. Pan, zoom and the viewport size are owned here and nowhere else;
    /// every node below is positioned and sized from `state.viewport`.
    private func canvas(size: CGSize) -> some View {
        ZStack(alignment: .topLeading) {
            // background takes the canvas taps, so a tap that lands on a page reaches the page
            // instead of being swallowed here and dropping the keyboard
            palette.page
                .contentShape(Rectangle())
                .onTapGesture(count: 2) { location in doubleTap(at: location) }
                .onTapGesture { location in singleTap(at: location) }

            ForEach(placements()) { placement in
                nodeLayer(placement)
            }
        }
        .gesture(canvasGestures)
        .onAppear { reportSize(size) }
        .onChange(of: size) { _, updated in reportSize(updated) }
    }

    private func placements() -> [MasterNodePlacement] {
        let viewport: Viewport = state.viewport
        let editingId: String? = state.editingNodeId
        let selectedId: String? = state.selectedNodeId
        let handTool: Bool = isHandTool
        let allNodes: [CanvasNode] = state.document.nodes
        let visible: [CanvasNode] = state.visibleNodes

        var order: [String: Int32] = [:]
        for (position, node) in allNodes.enumerated() {
            order[node.id] = Int32(position)
        }

        var result: [MasterNodePlacement] = []
        result.reserveCapacity(visible.count)

        for node in visible {
            let rect: CanvasRect = commands.screenRectOf(node: node, viewport: viewport)
            let frame: CGRect = rect.cgRect
            let editing: Bool = node.id == editingId
            let selected: Bool = node.id == selectedId
            let fallback: Int32 = order[node.id] ?? 0
            let title: String = node.displayName(fallbackIndex: fallback)
            let placement = MasterNodePlacement(
                node: node,
                title: title,
                frame: frame,
                isEditing: editing,
                isSelected: selected,
                // drag only once selected, so a pinch starting over a page still reaches
                // the scaling layer instead of being claimed as a node drag
                dragEnabled: selected && !handTool && !editing && !node.locked
            )
            result.append(placement)
        }
        return result
    }

    @ViewBuilder
    private func nodeLayer(_ placement: MasterNodePlacement) -> some View {
        let node: CanvasNode = placement.node
        let frame: CGRect = placement.frame

        VStack(spacing: 0) {
            MasterNodeNameBar(
                title: placement.title,
                initialName: node.name,
                isSelected: placement.isSelected,
                palette: palette,
                onRename: { newName in onRename(node.id, newName) }
            )
            nodeContent(node, placement.isEditing)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(width: frame.width, height: frame.height)
        .background(nodeSurface)
        .overlay(nodeBorder(placement.isSelected))
        .overlay(alignment: .bottomTrailing) {
            if placement.isSelected && !isHandTool && !node.locked {
                resizeHandle(for: node)
            }
        }
        .offset(x: frame.minX, y: frame.minY)
        // simultaneous, so selecting a node never steals the tap from the text view inside it
        .simultaneousGesture(
            TapGesture().onEnded { onIntent(commands.selectNode(nodeId: node.id)) }
        )
        .gesture(dragGesture(for: node, enabled: placement.dragEnabled))
    }

    private func resizeHandle(for node: CanvasNode) -> some View {
        RoundedRectangle(cornerRadius: 4)
            .fill(palette.accent)
            .frame(width: 22, height: 22)
            .padding(2)
            .gesture(
                DragGesture(minimumDistance: 1)
                    .onChanged { value in
                        let deltaX = value.translation.width - lastResize.width
                        let deltaY = value.translation.height - lastResize.height
                        lastResize = value.translation
                        guard let intent = commands.resizedTo(
                            state: state,
                            nodeId: node.id,
                            deltaXPx: Float(deltaX),
                            deltaYPx: Float(deltaY)
                        ) else { return }
                        onIntent(intent)
                    }
                    .onEnded { _ in lastResize = .zero }
            )
    }

    private var nodeSurface: some View {
        RoundedRectangle(cornerRadius: 8).fill(palette.surface)
    }

    private func nodeBorder(_ isSelected: Bool) -> some View {
        let color: Color = isSelected ? palette.accent : palette.divider
        let width: CGFloat = isSelected ? 1.5 : 1.0
        return RoundedRectangle(cornerRadius: 8).stroke(color, lineWidth: width)
    }

    /// GeometryReader reports during layout, so publishing the new viewport straight away
    /// mutates state inside the update pass. Skip no-op sizes, defer the rest by one tick.
    private func reportSize(_ size: CGSize) {
        let width = Float(size.width)
        let height = Float(size.height)
        guard width > 0, height > 0 else { return }
        guard state.viewport.widthPx != width || state.viewport.heightPx != height else { return }
        DispatchQueue.main.async {
            onIntent(commands.viewportResized(width: width, height: height))
        }
    }

    private func singleTap(at location: CGPoint) {
        let screenX = Float(location.x)
        let screenY = Float(location.y)
        onIntent(commands.selectAt(screenX: screenX, screenY: screenY))
    }

    private func doubleTap(at location: CGPoint) {
        let point: CGPoint = state.viewport.canvasPoint(of: location)
        let canvasX = Float(point.x)
        let canvasY = Float(point.y)
        if let node = state.document.hitTest(canvasX: canvasX, canvasY: canvasY) {
            onIntent(commands.focusNode(nodeId: node.id))
        } else {
            onIntent(commands.zoomToFit())
        }
    }

    private var canvasGestures: some Gesture {
        SimultaneousGesture(magnifyGesture, panGesture)
    }

    private var magnifyGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                let factor: CGFloat = value / liveZoom
                liveZoom = value
                let focusX = Float(state.viewport.widthPx / 2)
                let focusY = Float(state.viewport.heightPx / 2)
                onIntent(commands.zoom(factor: Float(factor), focusX: focusX, focusY: focusY))
            }
            .onEnded { _ in liveZoom = 1 }
    }

    private var panGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                guard isHandTool || state.draggingNodeId == nil else { return }
                let deltaX: CGFloat = value.translation.width - lastPan.width
                let deltaY: CGFloat = value.translation.height - lastPan.height
                lastPan = value.translation
                onIntent(commands.pan(deltaX: Float(deltaX), deltaY: Float(deltaY)))
            }
            .onEnded { _ in lastPan = .zero }
    }

    private func dragGesture(for node: CanvasNode, enabled: Bool) -> some Gesture {
        let threshold: CGFloat = enabled ? 4 : .infinity
        return DragGesture(minimumDistance: threshold)
            .onChanged { value in
                guard enabled else { return }
                if state.draggingNodeId != node.id {
                    onIntent(commands.beginDrag(nodeId: node.id))
                    lastPan = .zero
                }
                let deltaX: CGFloat = value.translation.width - lastPan.width
                let deltaY: CGFloat = value.translation.height - lastPan.height
                lastPan = value.translation
                onIntent(commands.dragBy(deltaX: Float(deltaX), deltaY: Float(deltaY)))
            }
            .onEnded { _ in
                guard enabled else { return }
                lastPan = .zero
                onIntent(commands.endDrag())
            }
    }
}

private struct MasterNodeNameBar: View {

    let title: String
    let initialName: String
    let isSelected: Bool
    let palette: SmartTextPalette
    let onRename: (String) -> Void

    @State private var editing = false
    @State private var draft = ""

    var body: some View {
        content
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isSelected ? palette.accentSoft : palette.surface)
    }

    @ViewBuilder
    private var content: some View {
        if editing {
            nameField
        } else {
            nameLabel
        }
    }

    private var nameField: some View {
        TextField("Name", text: $draft)
            .font(.system(size: 12))
            .foregroundColor(palette.onSurface)
            .submitLabel(.done)
            .onSubmit { commit() }
    }

    private var nameLabel: some View {
        Text(title)
            .font(.system(size: 12))
            .foregroundColor(isSelected ? palette.accent : palette.onSurfaceMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture { beginEditing() }
    }

    private func beginEditing() {
        draft = initialName
        editing = true
    }

    private func commit() {
        editing = false
        onRename(draft.trimmingCharacters(in: .whitespaces))
    }
}
