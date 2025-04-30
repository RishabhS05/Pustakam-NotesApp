import AVKit
import shared
import SwiftUI
struct PlayerUiState {
 var totalDuration: String = "00:00"
 var timeElapsed: String = "00:00"
 var timeRemaining: String = "-00:00"
 var currentTime: Double = 0.0 // 0 to 100
 var totalTime : Double = 1.0// avoid 0 division
 var isPlaying: Bool = false
 var mediaPlayerItem : AVPlayerItem?
 var mediaContent : NoteContentModel.MediaContent?
}
