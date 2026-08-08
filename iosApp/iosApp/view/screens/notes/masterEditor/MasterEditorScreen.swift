import SwiftUI
import shared

struct MasterEditorScreen: View {

    let noteId: String?

    @StateObject private var viewModel = MasterEditorViewModel()
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dismiss) private var dismiss
    @State private var showAttach = false

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var commands: CanvasCommands { CanvasCommands.shared }

    var body: some View {
        ZStack(alignment: .bottom) {
            MasterCanvas(
                state: viewModel.canvas,
                onIntent: viewModel.onCanvasIntent,
                onRename: { viewModel.renameNode(nodeId: $0, name: $1) }
            ) { node, isEditing in
                MasterNodeContent(
                    node: node,
                    isEditing: isEditing,
                    scale: CGFloat(viewModel.canvas.viewport.scale),
                    textState: viewModel.text(for: node.id),
                    content: viewModel.content(for: node),
                    dismissToken: viewModel.keyboardDismissToken,
                    onTextIntent: { viewModel.onTextIntent(nodeId: node.id, intent: $0) },
                    onFocused: { viewModel.onCanvasIntent(commands.setEditing(nodeId: node.id)) },
                    onDelete: { viewModel.deleteNode(nodeId: node.id) }
                )
            }

            zoomBar

            if let editingId = viewModel.canvas.editingNodeId,
               let focused = viewModel.text(for: editingId) {
                SmartTextKeyboardToolbar(
                    toolbar: focused.toolbar,
                    expanded: focused.isToolbarExpanded,
                    canUndo: focused.canUndo,
                    canRedo: focused.canRedo,
                    onAction: { action in handle(action, nodeId: editingId, state: focused) }
                )
            }
        }
        .background(palette.page.ignoresSafeArea())
        .onAppear { viewModel.load(noteId: noteId) }
        .confirmationDialog("Add beside the focused widget", isPresented: $showAttach, titleVisibility: .visible) {
            Button("Text") { viewModel.addTextNode() }
            Button("Table") { viewModel.addWidgetNearFocused(kind: CanvasNodeKind.table) }
            Button("Drawing") { viewModel.addWidgetNearFocused(kind: CanvasNodeKind.drawing) }
            Button("Rebuild layout from note order") { viewModel.rebuildLayoutFromNote() }
            Button("Apply canvas order back to the note") { viewModel.applyCanvasOrderToNote() }
            Button("Cancel", role: .cancel) {}
        }
    }

    private var zoomBar: some View {
        HStack(spacing: 4) {
            Button { viewModel.onCanvasIntent(commands.zoomOut()) } label: {
                Image(systemName: "minus").foregroundColor(palette.onSurface)
            }
            Text("\(viewModel.canvas.zoomPercent)%")
                .font(.system(size: 13))
                .foregroundColor(palette.onSurfaceMuted)
            Button { viewModel.onCanvasIntent(commands.zoomIn()) } label: {
                Image(systemName: "plus").foregroundColor(palette.onSurface)
            }
            Button { viewModel.onCanvasIntent(commands.zoomToFit()) } label: {
                Image(systemName: "viewfinder").foregroundColor(palette.onSurface)
            }
            Button {
                viewModel.onCanvasIntent(
                    commands.isHandTool(state: viewModel.canvas)
                        ? commands.useSelectTool()
                        : commands.useHandTool()
                )
            } label: {
                Image(systemName: commands.isHandTool(state: viewModel.canvas) ? "hand.raised.fill" : "hand.point.up.left")
                    .foregroundColor(
                        commands.isHandTool(state: viewModel.canvas) ? palette.accent : palette.onSurface
                    )
            }
            Button { showAttach = true } label: {
                Image(systemName: "plus.square.on.square").foregroundColor(palette.accent)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 12).fill(palette.toolbar))
        .padding(.bottom, 16)
    }

    private func handle(_ action: ToolbarAction, nodeId: String, state: MasterTextState) {
        let master = MasterTextCommands.shared
        if SmartTextCommands.shared.isMore(action: action) {
            viewModel.onTextIntent(
                nodeId: nodeId,
                intent: master.setToolbarExpanded(expanded: !state.isToolbarExpanded)
            )
        } else if SmartTextCommands.shared.isDismiss(action: action) {
            viewModel.keyboardDismissToken &+= 1
            viewModel.onCanvasIntent(commands.setEditing(nodeId: nil))
        } else if let intent = master.forToolbar(action: action) {
            viewModel.onTextIntent(nodeId: nodeId, intent: intent)
        }
    }
}

struct MasterNodeContent: View {

    let node: CanvasNode
    let isEditing: Bool
    let scale: CGFloat
    let textState: MasterTextState?
    let content: NoteContentModel?
    let dismissToken: Int
    let onTextIntent: (MasterTextIntent) -> Void
    let onFocused: () -> Void
    var onOpenMedia: () -> Void = {}
    var onDelete: () -> Void = {}

    @Environment(\.colorScheme) private var scheme
    @Environment(\.openURL) private var openURL

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    private var media: NoteContentModel.MediaContent? {
        content as? NoteContentModel.MediaContent
    }

    var body: some View {
        switch node.kind {
        case CanvasNodeKind.masterText:
            textBody

        case CanvasNodeKind.media:
            mediaBody

        case CanvasNodeKind.document:
            if let media {
                InlineBookFileView(
                    media: media,
                    onOpenFull: onOpenMedia,
                    onDelete: onDelete
                )
            } else {
                placeholder("Document")
            }

        case CanvasNodeKind.link:
            linkBody

        case CanvasNodeKind.location:
            locationBody

        default:
            placeholder(node.kind.name)
        }
    }

    @ViewBuilder
    private var textBody: some View {
        if let textState {
            MasterTextWidget(
                state: textState,
                readOnly: !isEditing,
                scale: scale,
                accessory: nil,
                dismissToken: dismissToken,
                shouldFocus: isEditing,
                onIntent: onTextIntent
            )
            .padding(16)
            .onTapGesture { onFocused() }
        } else {
            placeholder("Empty text")
        }
    }

    @ViewBuilder
    private var mediaBody: some View {
        if let media {
            switch media.type {
            case ContentType.image, ContentType.gif:
                CardImageEditor(
                    content: media,
                    actionClick: onOpenMedia,
                    actionDelete: onDelete
                )

            case ContentType.video:
                VideoCardPlayer(
                    content: media,
                    actionClick: onOpenMedia,
                    actionDelete: onDelete
                )

            case ContentType.audio:
                AudioPlayView(mediaContent: media, onDelete: onDelete)

            default:
                placeholder("Media")
            }
        } else {
            placeholder("Media")
        }
    }

    private var linkBody: some View {
        let link = content as? NoteContentModel.Link
        return VStack(alignment: .leading, spacing: 8) {
            Image(systemName: "link").foregroundColor(palette.accent)
            Text(link?.url.isEmpty == false ? link!.url : "Link")
                .font(.system(size: 15))
                .foregroundColor(palette.accent)
                .lineLimit(3)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(16)
        .contentShape(Rectangle())
        .onTapGesture {
            if let raw = link?.url, let url = URL(string: raw) { openURL(url) }
        }
    }

    private var locationBody: some View {
        let location = content as? NoteContentModel.Location
        let label: String = {
            if let address = location?.address, !address.isEmpty { return address }
            if let location { return "\(location.latitude), \(location.longitude)" }
            return "Location"
        }()
        return VStack(alignment: .leading, spacing: 8) {
            Image(systemName: "mappin.and.ellipse").foregroundColor(palette.accent)
            Text(label)
                .font(.system(size: 15))
                .foregroundColor(palette.onSurface)
                .lineLimit(3)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(16)
        .contentShape(Rectangle())
        .onTapGesture {
            guard let location,
                  let url = URL(string: "maps://?ll=\(location.latitude),\(location.longitude)")
            else { return }
            openURL(url)
        }
    }

    private func placeholder(_ label: String) -> some View {
        ZStack {
            palette.codeBackground
            Text(label)
                .font(.system(size: 13))
                .foregroundColor(palette.onSurfaceMuted)
        }
    }
}
