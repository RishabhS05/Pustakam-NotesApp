import Combine
import AVKit
import shared
import SwiftUI
import MediaPlayer

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
        playNextAutomatically()
    }
    private func playNextAutomatically(){
        guard let playing = currentPlaying else { return }
        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: getPlayingItem(media: playing),
            queue: .main
        ) { [weak self] _ in
            self?.playNext()
        }
    }
    
    private func convertAVPlayerItem(list : [NoteContentModel.MediaContent]){
        list.forEach{ item in
            self.playList[item.id] = PlayerUiState(
                mediaPlayerItem :AVPlayerItem(url: URL(fileURLWithPath: item.getMediaUrl())),
                mediaContent: item
            )
        }
    }
    private func activateSession() {
        do {
            try session.setCategory(
                .playback,
                mode: .moviePlayback,
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
    
    func updateTimeElapsed(){
        Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if let player = self.player {
                let elapsed = CMTimeGetSeconds(player.currentTime())
                MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] = elapsed
            }
        }
    }
    func selectAndPlayMedia(media : NoteContentModel.MediaContent){
            // activate our session before playing audio
        activateSession()
        let player = getPlayer()
        let playNow = getPlayingItem(media: media)
        player.replaceCurrentItem(with: playNow?.mediaPlayerItem)
        currentPlaying = media
        updateNowPlayingInfo(title: media.title, duration: TimeInterval(media.duration))
        updateTimeElapsed()
        setupRemoteTransportControls()
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
            updateNowPlayingInfo(title: media.title, duration: TimeInterval(media.duration))
            setupRemoteTransportControls()
            player.play()
            updateTimeElapsed()
            
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
    // background Playing
    
    func updateNowPlayingInfo(title: String, duration: TimeInterval, artwork: UIImage? = nil) {
        var nowPlayingInfo: [String: Any] = [
            MPMediaItemPropertyTitle: title,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyPlaybackRate: 1.0
        ]

        if let artwork = artwork {
            nowPlayingInfo[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: artwork.size) { _ in artwork }
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }
    
    func setupRemoteTransportControls() {
        guard let player = self.player else { return }
        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.playCommand.addTarget { event in
            player.play()
            return .success
        }

        commandCenter.pauseCommand.addTarget { event in
            player.pause()
            return .success
        }

        commandCenter.nextTrackCommand.isEnabled = true
        commandCenter.previousTrackCommand.isEnabled = true

        commandCenter.nextTrackCommand.addTarget { _ in
                self.playNext()
                return .success
            }
        commandCenter.previousTrackCommand.addTarget { _ in
                self.playPrevious()
                return .success
            }
    }

    func playNext() {
        guard let current = currentPlaying  else { return }
        if let index = playingMediaList.firstIndex(where : {$0.id == current.id}), index + 1 < playingMediaList.count {
            let media = playingMediaList[index + 1]
              selectAndPlayMedia(media: media)
        }
    }

    func playPrevious() {
        guard let playing = currentPlaying  else { return }
        if let index = playingMediaList.firstIndex(where : {$0.id == playing.id}), index - 1 > 0  {
            let media = playingMediaList[index - 1]
              selectAndPlayMedia(media: media)
        }
    }
    deinit{
        NotificationCenter.default.removeObserver(self, name: .AVPlayerItemDidPlayToEndTime, object: currentPlaying)
    }
}
