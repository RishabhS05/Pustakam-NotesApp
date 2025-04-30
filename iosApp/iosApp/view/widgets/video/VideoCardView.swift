
import shared
import SwiftUI
import AVKit
struct VideoCardPlayer : View {
    var content : NoteContentModel.MediaContent
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void = {}
    var mediaManager = MediaManager.mediaManager
    init(content: NoteContentModel.MediaContent){
        self.content = content
    }
    
    var body: some View {
        ZStack{
            if mediaManager.currentPlaying?.id == content.id {
                            SystemControlledPlayerView(player: mediaManager.getPlayer())
                        } else {
                            ZStack {
                                // You can show thumbnail or just placeholder when not active
                                Rectangle().fill(Color.black.opacity(0.2))
                                Image(systemName: "play.circle.fill")
                                    .resizable()
                                    .frame(width: 40, height: 40)
                                    .foregroundColor(.white)
                            }
                        }
        }.frame(width: 200,height: 300)
            .cornerRadius(12)
            .padding(12)
            .onAppear{
                mediaManager.selectAndPlayMedia(media: content)
            }
            .onTapGesture {
                mediaManager.resumePlaying(media:content)
                actionClick()
            }
    }
}

