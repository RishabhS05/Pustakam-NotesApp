import SwiftUI

struct LoadingUI : View {
    var body: some View {
            VStack {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint:.brown))
                    .scaleEffect(x: 1.5, y: 1.5)
            }
            .frame(width: 100,
                   height: 100, alignment: .center)
            .background(.white.opacity(0.5))
            .foregroundColor(.brown)
            .cornerRadius(20)
            .shadow(color: .yellow.opacity(0.2), radius: 10)
        }
}
