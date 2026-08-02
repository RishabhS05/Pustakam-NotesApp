import SwiftUI

struct CardActionsButton: View {
    var tint: Color = .white
    var iconSize: CGFloat = 15
    var diameter: CGFloat = 30

    var chipColor: Color? = Color.gray.opacity(0.3)
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "ellipsis")
                .rotationEffect(.degrees(90))            // vertical dots = Android MoreVert
                .font(.system(size: iconSize, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: diameter, height: diameter)
                .background(
                    Group {
                        if let chipColor { Circle().fill(chipColor) }
                    }
                )
        }
        .buttonStyle(.plain)
        .contentShape(Circle())
    }
}
