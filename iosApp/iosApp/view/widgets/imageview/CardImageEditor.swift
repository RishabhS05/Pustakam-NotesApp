
import shared
import SwiftUI
struct CardImageEditor : View{
    var content : NoteContentModel.MediaContent
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void
    
    var body: some View {
        ZStack (alignment: .bottom){
            AsyncImage(url: URL(string : content.getMediaUrl())) { phase in
                if let image = phase.image {
                        // Display the loaded image
                    image.resizable().scaledToFill()
                    
                } else if phase.error != nil || content.getMediaUrl().isEmpty {
                        // Display a placeholder when loading failed
                    Image("avatar").resizable()
                        .scaledToFill()
                } else {
                        // Display a placeholder while loading
                    ProgressView()
                }
            }
            .frame(width: 200,height: 300)
            .cornerRadius(12)
            .padding(12)
            .onTapGesture {
                actionClick()
            }
            Image(systemName: "square.and.arrow.up.circle.fill")
                .font(.system(size: 30)
                    .weight(.bold))
                .scaledToFill().imageScale(.large)
                .foregroundColor(.brown)
                .frame(width: 150,height: 50,alignment: .bottomTrailing)
                .padding(8)
                .onTapGesture {
                    actionEdit()
                }
        }

    }
}
