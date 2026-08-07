import SwiftUI
import shared

// floating bar over a selection — primary row always, secondary revealed by MORE
struct SmartTextSelectionToolbar: View {

    let toolbar: ToolbarState
    let expanded: Bool
    let onAction: (ToolbarAction) -> Void

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    var body: some View {
        VStack(spacing: 2) {
            HStack(spacing: 0) {
                ForEach(ToolbarSpec.shared.selectionPrimary, id: \.self) { action in
                    SmartTextToolbarButton(
                        action: action,
                        active: ToolbarSpec.shared.isActive(action: action, toolbar: toolbar),
                        enabled: true,
                        palette: palette
                    ) { onAction(action) }
                }
                SmartTextToolbarSeparator(palette: palette)
                SmartTextToolbarButton(
                    action: moreAction,
                    active: expanded,
                    enabled: true,
                    palette: palette
                ) { onAction(moreAction) }
            }
            if expanded {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 0) {
                        ForEach(secondaryActions, id: \.self) { action in
                            SmartTextToolbarButton(
                                action: action,
                                active: ToolbarSpec.shared.isActive(action: action, toolbar: toolbar),
                                enabled: true,
                                palette: palette
                            ) { onAction(action) }
                        }
                    }
                }
                .transition(.opacity)
            }
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: SmartTextMetrics.toolbarCorner)
                .fill(palette.toolbar)
                .shadow(color: .black.opacity(0.22), radius: 10, y: 4)
        )
        .animation(.easeInOut(duration: 0.18), value: expanded)
    }

    private var moreAction: ToolbarAction {
        ToolbarSpec.shared.selectionSecondary.first {
            SmartTextCommands.shared.isMore(action: $0)
        } ?? ToolbarSpec.shared.selectionSecondary[0]
    }

    private var secondaryActions: [ToolbarAction] {
        ToolbarSpec.shared.selectionSecondary.filter {
            !SmartTextCommands.shared.isMore(action: $0)
        }
    }
}

// pinned above the keyboard — horizontally scrollable, with an expandable tray
struct SmartTextKeyboardToolbar: View {

    let toolbar: ToolbarState
    let expanded: Bool
    let canUndo: Bool
    let canRedo: Bool
    let onAction: (ToolbarAction) -> Void

    @Environment(\.colorScheme) private var scheme

    private var palette: SmartTextPalette { SmartTextPalette.of(scheme) }

    var body: some View {
        VStack(spacing: 0) {
            if expanded {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 0) {
                        ForEach(ToolbarSpec.shared.keyboardExpanded, id: \.self) { action in
                            button(action)
                        }
                    }
                    .padding(.horizontal, 6)
                    .padding(.vertical, 4)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(ToolbarSpec.shared.keyboardPrimary, id: \.self) { action in
                        button(action)
                    }
                    SmartTextToolbarSeparator(palette: palette)
                    SmartTextToolbarButton(
                        action: moreAction,
                        active: expanded,
                        enabled: true,
                        palette: palette
                    ) { onAction(moreAction) }
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 4)
            }
        }
        .background(palette.toolbar)
        .clipShape(
            UnevenRoundedRectangle(
                topLeadingRadius: SmartTextMetrics.toolbarCorner,
                topTrailingRadius: SmartTextMetrics.toolbarCorner
            )
        )
        .shadow(color: .black.opacity(0.18), radius: 8, y: -2)
        .animation(.easeInOut(duration: 0.18), value: expanded)
    }

    private func button(_ action: ToolbarAction) -> some View {
        SmartTextToolbarButton(
            action: action,
            active: ToolbarSpec.shared.isActive(action: action, toolbar: toolbar),
            enabled: SmartTextCommands.shared.isEnabled(
                action: action,
                toolbar: toolbar,
                canUndo: canUndo,
                canRedo: canRedo
            ),
            palette: palette
        ) { onAction(action) }
    }

    private var moreAction: ToolbarAction {
        ToolbarSpec.shared.selectionSecondary.first {
            SmartTextCommands.shared.isMore(action: $0)
        } ?? ToolbarSpec.shared.selectionSecondary[0]
    }
}

struct SmartTextToolbarButton: View {

    let action: ToolbarAction
    let active: Bool
    let enabled: Bool
    let palette: SmartTextPalette
    let onTap: () -> Void

    var body: some View {
        let isPrimary = SmartTextCommands.shared.isPrimaryFormat(action: action)
        let background: Color = active ? (isPrimary ? palette.accent : palette.accentSoft) : .clear
        let tint: Color = {
            if active { return isPrimary ? palette.onAccent : palette.accent }
            return enabled ? palette.onSurface : palette.onSurfaceMuted.opacity(0.4)
        }()

        Button(action: onTap) {
            Image(
                systemName: SmartTextStyleMapper.sfSymbol(
                    forIconKey: SmartTextCommands.shared.iconKey(action: action)
                )
            )
            .font(.system(size: 17, weight: .medium))
            .foregroundColor(tint)
            .frame(width: SmartTextMetrics.buttonSize, height: SmartTextMetrics.buttonSize)
            .background(
                RoundedRectangle(cornerRadius: SmartTextMetrics.buttonCorner).fill(background)
            )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .padding(2)
        .accessibilityLabel(ToolbarSpec.shared.label(action: action))
        .accessibilityAddTraits(active ? [.isSelected] : [])
    }
}

struct SmartTextToolbarSeparator: View {

    let palette: SmartTextPalette

    var body: some View {
        Rectangle()
            .fill(palette.divider)
            .frame(width: 1, height: 22)
            .padding(.horizontal, 5)
    }
}
