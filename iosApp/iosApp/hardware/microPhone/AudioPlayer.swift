
import Foundation
import AVFoundation
class AudioPlayer {
    private var audioPlayer: AVAudioPlayer?
     
    var audioFileURL: URL?
        /// Stop Playback
        func stopPlayback() {
            audioPlayer?.stop()
        }
    
        /// Play Recording
        func playRecording() {
            guard let fileURL = audioFileURL else { return }
            
            do {
                audioPlayer = try AVAudioPlayer(contentsOf: fileURL)
                audioPlayer?.prepareToPlay()
                audioPlayer?.play()
            } catch {
                print("Failed to play recording: \(error)")
            }
        }
}
