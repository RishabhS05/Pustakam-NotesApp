
import SwiftUI
struct BackButton: View {
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
                Image(systemName: "chevron.backward")
        }
        .foregroundColor(Theme.Colors.secondary)
    }
}
