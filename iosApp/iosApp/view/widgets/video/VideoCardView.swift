
import shared
import SwiftUI
import AVKit
struct VideoCardPlayer : View {
    var content : NoteContentModel.MediaContent
    var actionEdit : () -> Void = {}
    var actionClick : () -> Void = {}
    // 🔧 14-Jul-2026: NEW — delete callback (video had no delete UI before)
    var actionDelete : () -> Void = {}
    // 🔧 14-Jul-2026: NEW — save-to-device callback (video → Photos gallery)
    var actionSave : () -> Void = {}
    // 🔧 14-Jul-2026: NEW — long-press actions bar visibility (same pattern as CardImageEditor)
    @State private var showActions : Bool = false
    var mediaManager = MediaManager.mediaManager
    // 🔧 14-Jul-2026: init now accepts actionDelete + actionSave (defaults keep old call sites compiling)
    //   Usage: VideoCardPlayer(content: media, actionDelete: { ... }, actionSave: { ... })
    init(content: NoteContentModel.MediaContent,
         actionDelete: @escaping () -> Void = {},
         actionSave: @escaping () -> Void = {}){
        self.content = content
        self.actionDelete = actionDelete
        self.actionSave = actionSave
    }

    var body: some View {
        // 🔧 14-Jul-2026: outer ZStack aligned .bottom so the actions bar overlays the card bottom
        ZStack(alignment: .bottom){
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
                                        .foregroundColor(Theme.Colors.primary)
                                }
                            }
            }.frame(width: 200,height: 300)
                .cornerRadius(12)
                .onAppear{
                    // 🔧 14-Jul-2026: prepare only (no autoplay). Scrolling a video into view no
                    //   longer hijacks the shared player / forces a load. Playback starts on tap.
                    mediaManager.prepareMedia(media: content)
                }
                .onTapGesture {
                    mediaManager.resumePlaying(media:content)
                    actionClick()
                }
                // 🔧 14-Jul-2026: NEW — long-press shows the actions bar for 2.5s (CardImageEditor parity)
                .onLongPressGesture {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        showActions = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            showActions = false
                        }
                    }
                }

            // 🔧 14-Jul-2026: NEW — bottom-fade actions bar with delete/edit (CardImageEditor parity)
            if showActions {
                ZStack(alignment: .bottom) {
                    LinearGradient(
                        colors: [
                            .clear,
                            .black.opacity(0.05),
                            .black.opacity(0.35),
                            .black.opacity(0.55)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 90)

                    HStack(spacing: 40) {
                        Button {
                            actionDelete()
                        } label: {
                            Image(systemName: "trash.fill")
                                .font(.title2)
                                .foregroundColor(.red)
                        }

                        // 🔧 14-Jul-2026: NEW — save-to-gallery button
                        Button {
                            actionSave()
                        } label: {
                            Image(systemName: "square.and.arrow.down.fill")
                                .font(.title2)
                                .foregroundColor(.white)
                        }

                        Button {
                            actionEdit()
                        } label: {
                            Image(systemName: "square.and.arrow.up.fill")
                                .font(.title2)
                                .foregroundColor(.white)
                        }
                    }
                    .padding(.bottom, 20)
                }
                .frame(width: 200, height: 70)
                .transition(.opacity)
            }
        }
        .frame(width: 200, height: 300)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(12)
    }
}
