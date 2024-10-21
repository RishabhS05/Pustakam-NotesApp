
import SwiftUI

struct AvatarImageView: View {
    var imageUrl : String
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void
    var body: some View {
        
        ZStack (alignment: .bottom){
           AsyncImage(url: URL(string :  imageUrl)) { phase in
                if let image = phase.image {
                        // Display the loaded image
                    image.resizable().scaledToFit()
                        .frame(width: 200,height: 200)
                        .clipShape(Circle())
                        .overlay {
                            Circle().stroke(.brown, lineWidth: 4)
                        }
                        .shadow(radius:7)
                    
                } else if phase.error != nil || imageUrl.isEmpty {
                        // Display a placeholder when loading failed
                    
                    Image("avatar").resizable()
                        .scaledToFit()
                        .frame(width: 200,height: 200)
                        .clipShape(Circle())
                        .overlay {
                            Circle().stroke(.brown, lineWidth: 4)
                        }
                        .shadow(radius:7)
                    
                } else {
                        // Display a placeholder while loading
                    ProgressView()
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
    
    }
#Preview {
    AvatarImageView(imageUrl: "" ,actionEdit:  {print("ok")}, actionClick: {})
}
