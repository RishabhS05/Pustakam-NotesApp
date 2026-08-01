
import SwiftUI

struct AvatarImageView: View {
    var imageUrl : String
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void
    var body: some View {
        
        ZStack (alignment: .bottom){
            if imageUrl.isEmpty {
                placeholderAvatar
            } else {
                AsyncImage(url: URL(fileURLWithPath: imageUrl)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFit()
                            .frame(width: 200,height: 200)
                            .clipShape(Circle())
                            .overlay {
                                Circle().stroke(.brown, lineWidth: 4)
                            }
                            .shadow(radius:7)
                    } else if phase.error != nil {
                        placeholderAvatar
                    } else {
                        ProgressView()
                            .frame(width: 200, height: 200)
                    }
                }
            }
            Image(systemName: "square.and.arrow.up.circle.fill")
                .font(.system(size: 30)
                    .weight(.bold))
                .scaledToFill().imageScale(.large)
                .foregroundColor(.brown)
                .frame(width: 150,height: 50,alignment: .bottomTrailing)
                .padding(8).onTapGesture {
                    actionEdit()
                }
        }

    }
    
    private var placeholderAvatar: some View {
        Image(systemName: "person.crop.circle.fill")
            .resizable()
            .scaledToFit()
            .frame(width: 200,height: 200)
            .foregroundStyle(.brown)
            .clipShape(Circle())
            .overlay {
                Circle().stroke(.brown, lineWidth: 4)
            }
            .shadow(radius:7)
    }
    
    }
#Preview {
    AvatarImageView(imageUrl: "" ,actionEdit:  {print("ok")}, actionClick: {})
}
