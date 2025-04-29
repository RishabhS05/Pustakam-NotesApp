import Combine
import AVKit
import shared




@Observable final class MediaManager {
    private var player : MediaControlledPlayer?
    
   static let mediaManager = MediaManager()
    
    var session = AVAudioSession.sharedInstance()
    private var playingMediaList = [NoteContentModel.MediaContent]()
    
    var playList :[String:PlayerUiState] = [:]
    var currentPlaying : NoteContentModel.MediaContent?
    
    init(){
        NoteRepositoryHelper().noteContentMediaList{ mediaList in
            self.playingMediaList = mediaList
            self.convertAVPlayerItem(list: self.playingMediaList)
        }
    }
    
    private func convertAVPlayerItem(list : [NoteContentModel.MediaContent]){
        list.forEach{ item in
            self.playList[item.id] = PlayerUiState(mediaPlayerItem :AVPlayerItem(url: URL(string: item.getMediaUrl())!), mediaContent: item)
        }
    }
    private func activateSession() {
        do {
            try session.setCategory(
                .playback,
                mode: .default,
                options: []
            )
        } catch _ {}
        
        do {
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch _ {}
        
        do {
            try session.overrideOutputAudioPort(.speaker)
        } catch _ {}
    }
    
    func deactivateSession() {
        do {
            try session.setActive(false, options: .notifyOthersOnDeactivation)
        } catch let error as NSError {
            print("Failed to deactivate audio session: \(error.localizedDescription)")
        }
    }
    
    
    func selectedMedia(media : NoteContentModel.MediaContent){
            // activate our session before playing audio
        activateSession()
        let player = getPlayer()
        let playNow = getPlayingItem(media: media)
        player.replaceCurrentItem(with: playNow?.mediaPlayerItem)
        currentPlaying = media
        player.play()
    }
    func pause() {
        if let player = player {
            player.pause()
        }
    }
    
    func resumePlaying(media : NoteContentModel.MediaContent){
        if let player = player {
            let playNow = getPlayingItem(media: media)
            currentPlaying = media
            player.replaceCurrentItem(with: playNow?.mediaPlayerItem)
            player.play()
        }
    }
    func seekTo(media: NoteContentModel.MediaContent, currentTime : Double){
        if let player = player {
            if currentPlaying?.id != media.id {
                currentPlaying = media
                let playNow = getPlayingItem(media: media)
                player.replaceCurrentItem(with: playNow?.mediaPlayerItem)
            }
            let seekTime = CMTime(seconds: currentTime, preferredTimescale: 600)
            player.seek(to: seekTime)
        }
    }
    
    func getPlaybackDuration() -> Double {
        guard let player = player else {
            return 0
        }
        
        return player.currentItem?.duration.seconds ?? 0
    }
    
 
    func getPlayingItem(media : NoteContentModel.MediaContent) -> PlayerUiState? {
      return playList[media.id]
    }
    
    func getPlayer()-> MediaControlledPlayer {
        if let player = player { return self.player! }
        else {
            self.player = MediaControlledPlayer()
            return self.player!
        }
    }
}
