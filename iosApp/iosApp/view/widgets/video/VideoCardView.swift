
import shared
import SwiftUI
import AVKit
struct VideoCardPlayer : View {
    var content : NoteContentModel.MediaContent
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void
    var body: some View {
        ZStack{
            VideoPlayer(player: AVPlayer(url: URL(string : content.getMediaUrl())!))
        }
            .frame(width: 200,height: 300)
            .cornerRadius(12)
            .padding(12)
            .onTapGesture {
                actionClick()
            }
    }
}
