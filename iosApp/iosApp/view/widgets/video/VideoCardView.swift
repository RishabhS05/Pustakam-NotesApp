
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
        mediaManager.selectedMedia(media: content)
    }
    
    var body: some View {
        ZStack{
            SystemControlledPlayerView(player: mediaManager.currentPlaying?.id == content.id ? mediaManager.getPlayer() : nil)
        }.frame(width: 200,height: 300)
            .cornerRadius(12)
            .padding(12)
            .onTapGesture {
                actionClick()
            }
    }
}

