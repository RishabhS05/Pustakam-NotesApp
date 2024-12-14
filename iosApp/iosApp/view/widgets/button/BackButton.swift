
import SwiftUI
struct BackButton: View {
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack{
                Image(systemName: "chevron.backward")
                Text("Back")
            }
        }
        .foregroundColor(.brown)
    }
}
