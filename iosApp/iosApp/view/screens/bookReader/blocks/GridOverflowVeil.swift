import SwiftUI

// 📖 01-Aug-2026: the "+N" veil. It sits ON TOP of the last cell and owns the tap, which is why
//   tapping it opens the full set instead of the single item underneath.
struct GridOverflowVeil: View {
    let count: Int
    var onTap: () -> Void = {}

    var body: some View {
        if count > 0 {
            ZStack {
                Color.black.opacity(0.5)
                Text("+\(count)").font(.title2).foregroundColor(.white)
            }
            .contentShape(Rectangle())
            .onTapGesture { onTap() }
        }
    }
}
