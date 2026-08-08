import SwiftUI
import shared

struct MasterCanvas<NodeContent: View>: View {

    let state: CanvasEditorState
    let onIntent: (CanvasEditorIntent) -> Void
    var onRename: (String, String) -> Void = { _, _ in }
    @ViewBuilder let nodeContent: (CanvasNode, Bool) -> NodeContent

    @Environment(\.colorScheme) private var scheme
    @State private var liveZoom: CGFloat = 1
    @State private var lastPan: CGSize = .zero

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var commands: CanvasCommands { CanvasCommands.shared }

    private var isHandTool: Bool { commands.isHandTool(state: state) }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .topLeading) {
                palette.page.ignoresSafeArea()

                ForEach(state.visibleNodes, id: \.id) { node in
                    let screen = commands.screenRectOf(node: node, viewport: state.viewport)
                    let isEditing = node.id == state.editingNodeId
                    let isSelected = node.id == state.selectedNodeId

                    VStack(spacing: 0) {
                        MasterNodeNameBar(
                            node: node,
                            index: state.document.nodes.firstIndex { $0.id == node.id } ?? 0,
                            isSelected: isSelected,
                            palette: palette,
                            onRename: { onRename(node.id, $0) }
                        )
                        nodeContent(node, isEditing)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                        .frame(width: CGFloat(screen.width), height: CGFloat(screen.height))
                        .background(
                            RoundedRectangle(cornerRadius: 8).fill(palette.surface)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(
                                    isSelected ? palette.accent : palette.divider,
                                    lineWidth: isSelected ? 1.5 : 1
                                )
                        )
                        .offset(x: CGFloat(screen.x), y: CGFloat(screen.y))
                        .gesture(dragGesture(for: node, enabled: !isHandTool && !isEditing && !node.locked))
                }
            }
            .contentShape(Rectangle())
            .gesture(canvasGestures)
            .onTapGesture(count: 2) { location in
                let canvasX = state.viewport.toCanvasX(screenX: Float(location.x))
                let canvasY = state.viewport.toCanvasY(screenY: Float(location.y))
                if let node = state.document.hitTest(canvasX: canvasX, canvasY: canvasY) {
                    onIntent(commands.focusNode(nodeId: node.id))
                } else {
                    onIntent(commands.zoomToFit())
                }
            }
            .onTapGesture { location in
                onIntent(commands.selectAt(screenX: Float(location.x), screenY: Float(location.y)))
            }
            .onAppear {
                onIntent(
                    commands.viewportResized(
                        width: Float(geometry.size.width),
                        height: Float(geometry.size.height)
                    )
                )
            }
            .onChange(of: geometry.size) { _, size in
                onIntent(
                    commands.viewportResized(width: Float(size.width), height: Float(size.height))
                )
            }
        }
    }

    private var canvasGestures: some Gesture {
        SimultaneousGesture(
            MagnificationGesture()
                .onChanged { value in
                    let factor = value / liveZoom
                    liveZoom = value
                    onIntent(
                        commands.zoom(
                            factor: Float(factor),
                            focusX: Float(state.viewport.widthPx / 2),
                            focusY: Float(state.viewport.heightPx / 2)
                        )
                    )
                }
                .onEnded { _ in liveZoom = 1 },
            DragGesture()
                .onChanged { value in
                    guard isHandTool || state.draggingNodeId == nil else { return }
                    let deltaX = value.translation.width - lastPan.width
                    let deltaY = value.translation.height - lastPan.height
                    lastPan = value.translation
                    onIntent(commands.pan(deltaX: Float(deltaX), deltaY: Float(deltaY)))
                }
                .onEnded { _ in lastPan = .zero }
        )
    }

    private func dragGesture(for node: CanvasNode, enabled: Bool) -> some Gesture {
        DragGesture(minimumDistance: enabled ? 4 : .infinity)
            .onChanged { value in
                guard enabled else { return }
                if state.draggingNodeId != node.id {
                    onIntent(commands.beginDrag(nodeId: node.id))
                    lastPan = .zero
                }
                let deltaX = value.translation.width - lastPan.width
                let deltaY = value.translation.height - lastPan.height
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

    let node: CanvasNode
    let index: Int
    let isSelected: Bool
    let palette: SmartTextPalette
    let onRename: (String) -> Void

    @State private var editing = false
    @State private var draft = ""

    var body: some View {
        Group {
            if editing {
                TextField("Name", text: $draft, onCommit: {
                    editing = false
                    onRename(draft.trimmingCharacters(in: .whitespaces))
                })
                .font(.system(size: 12))
                .foregroundColor(palette.onSurface)
            } else {
                Text(node.displayName(fallbackIndex: Int32(max(index, 0))))
                    .font(.system(size: 12))
                    .foregroundColor(isSelected ? palette.accent : palette.onSurfaceMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        draft = node.name
                        editing = true
                    }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isSelected ? palette.accentSoft : palette.surface)
    }
}
