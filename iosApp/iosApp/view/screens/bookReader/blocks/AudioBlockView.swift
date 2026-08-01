import SwiftUI
import shared

// 📖 01-Aug-2026: AudioPlayView is used AS IS — no compact variant. Several audio blocks share a
//   page purely because their estimated heights fit, never because the widget was shrunk.
struct AudioBlockView: View {
    let block: ReaderBlock.Audio
    private let mediaManager = MediaManager.mediaManager

    var body: some View {
        AudioPlayView(mediaContent: block.item)
            .frame(maxWidth: .infinity)
            .onAppear {
                if mediaManager.currentPlaying?.id != block.item.id {
                    mediaManager.prepareMedia(media: block.item)
                }
            }
    }
}
