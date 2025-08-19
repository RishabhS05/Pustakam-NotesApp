

import SwiftUI
import shared
import AVKit
class AudioPlayerViewModel : BaseViewModel,ObservableObject {
    let mediaContent  : NoteContentModel.MediaContent
    @Published var state : PlayerUiState
    private let manager = MediaManager.mediaManager
    private var playerObserver: Any?
    
    init( mediaContent: NoteContentModel.MediaContent){
        self.mediaContent = mediaContent
        self.state =  manager.getPlayingItem(media: mediaContent) ?? PlayerUiState()
        super.init()
        setupTimeObserver()
    }
    
    func onPlayPause(){
        if state.isPlaying {
            manager.pause()
        }else {
            manager.resumePlaying(media: mediaContent)
        }
        state.isPlaying.toggle()
    }
    func selectMedia(){
        manager.selectAndPlayMedia(media : mediaContent)
    }
    func onSeek(){
        manager.seekTo(media : mediaContent, currentTime: state.currentTime)
    }
        
    func getPlayer() -> AVPlayer {return manager.getPlayer()}
    
    func setupTimeObserver() {
            let player = manager.getPlayer()
            let interval = CMTime(seconds: 1, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
            playerObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] progressTime in
                guard let self = self, self.manager.currentPlaying?.id == self.mediaContent.id  else { return }
                if let duration = player.currentItem?.duration, CMTIME_IS_NUMERIC(duration) {
                    let totalSeconds = CMTimeGetSeconds(duration)
                    let currentSeconds = CMTimeGetSeconds(progressTime)
                    self.state.totalTime = totalSeconds
                    self.state.currentTime = currentSeconds
                    // Optional: Reset if finished playing
                    if currentSeconds >=  totalSeconds {
                        self.state.isPlaying = false
                    }
                }
            }
        }
    
    deinit {
           if let observer = playerObserver {
               manager.getPlayer().removeTimeObserver(observer)
           }
       }

}
struct AudioPlayView: View {
    
    let mediaContent  : NoteContentModel.MediaContent
    @State private var isEditing = false
    @State private var tempCurrentTime: Double = 0.0
    
    var onDelete: () -> Void = {}
    // edit the audio
    var onEdit: ()-> Void = {}
    @StateObject  private var viewModel : AudioPlayerViewModel
    init(
        mediaContent: NoteContentModel.MediaContent,
    ) {
        self.mediaContent = mediaContent
        _viewModel = StateObject(wrappedValue:AudioPlayerViewModel(mediaContent: mediaContent))
    }
    var body: some View{
        content
    }
    @ViewBuilder
    var content: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                let utils = Utils()
                Text(utils.formatTime(viewModel.state.totalTime))
                    .font(.caption2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(utils.formatTime(isEditing ? tempCurrentTime : viewModel.state.currentTime))
                    .font(.caption)
                    .frame(maxWidth: .infinity, alignment: .center)

                Text("-" + utils.formatTime(max(viewModel.state.totalTime - (isEditing ? tempCurrentTime : viewModel.state.currentTime), 0)))
                    .font(.caption2)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .onAppear{
                MediaManager.mediaManager.selectAndPlayMedia(media: mediaContent)
            }
            .padding(.horizontal, 6)
            .padding(.top, 4)

            HStack(spacing: 8) {
                Slider(
                    value: $viewModel.state.currentTime,
                    in: 0...max(viewModel.state.totalTime, 1),
                    onEditingChanged: { editing in
                        isEditing = editing
                        if !isEditing{
                            viewModel.onSeek()
                        }else {
                            tempCurrentTime = viewModel.state.currentTime
                        }
                    }
        
                )
                .padding(.horizontal, 4)
                .padding(.bottom, 4)

                Button(action:{
                    viewModel.onPlayPause()
                }) {
                    Image(systemName: viewModel.state.isPlaying ? "pause.fill" : "play.fill")
                        .resizable()
                        .frame(width: 20,height: 20)
                        .foregroundColor(Theme.Colors.secondary)
                }.padding(4)

                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .resizable()
                        .frame(width: 20, height: 20)
                        .foregroundColor(Color.red)
                }.padding(.trailing,4)
            }
            .padding(.horizontal, 6)
        }.onHover{ _ in
            viewModel.selectMedia()
        }
        .padding(8)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 1)
    }
}

