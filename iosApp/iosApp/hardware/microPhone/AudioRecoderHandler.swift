import Foundation
import AVFoundation

class AudioRecorder: ObservableObject {
    @Published var isRecording = false
    @Published var isPlaying = false
    @Published var normalizedPower: Float = 0.0
    @Published var phase: Double = 0.0
    var audioFileURL: URL?
    private var audioRecorder: AVAudioRecorder?
    private var audioPlayer: AVAudioPlayer?
    var fileName = ""
    
    private var timer: Timer?
    
    /// Start Recording
    func startRecording() {
        let session = AVAudioSession.sharedInstance()
        do {
            fileName = "\(Date().toString(dateFormat: "dd-MM-YYYY-HH:mm:ss")).m4a"
            try session.setCategory(.playAndRecord, mode: .default,options: [.defaultToSpeaker])
            try session.setActive(true)
            let tempDir = FileManager.default.temporaryDirectory
            let fileURL = tempDir.appendingPathComponent(fileName)
            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 48000,
                AVNumberOfChannelsKey: 2,
                AVEncoderBitRateKey: 320000,
                AVEncoderAudioQualityKey: AVAudioQuality.max.rawValue
            ]
            audioRecorder = try AVAudioRecorder(url: fileURL, settings: settings)
            audioRecorder?.record()
            audioRecorder?.isMeteringEnabled = true
            isRecording = true
            startTimerMonitoring()
        } catch {
            print("Failed to start recording: \(error)")
        }
    }
    
    /// Stop Recording
    func stopRecording() {
        audioRecorder?.stop()
        audioFileURL = audioRecorder?.url
        audioRecorder = nil
        isRecording = false
        stopTimerMonitoring()
    }
    
    /// Save Recording with Custom Name
    func saveRecording(withName name: String) {
        
        guard let originalURL = audioFileURL else { return }
        
        let documentsDir = FileManager.default.urls(for: .documentDirectory,
                                                    in: .userDomainMask).first!
        let newFileURL = documentsDir.appendingPathComponent(name)
        
        do {
            
            try FileManager.default.moveItem(at: originalURL, to: newFileURL)
            audioFileURL = newFileURL
            print(audioFileURL ?? "")
            
        } catch {
            print("Failed to save file: \(error)")
        }
    }
    
    /// Play Recording
    func playRecording() {
        guard let fileURL = audioFileURL else { return }
        
        do {
            audioPlayer = try AVAudioPlayer(contentsOf: fileURL)
            audioPlayer?.prepareToPlay()
            audioPlayer?.play()
            isPlaying = true
        } catch {
            print("Failed to play recording: \(error)")
        }
    }
    
    /// Pause/Resume Playback
    func togglePlayback() {
        guard let player = audioPlayer else { return }
        if player.isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }
   
    func startTimerMonitoring() {
          timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { _ in
              self.updateAudioLevel()
          }
      }

      func stopTimerMonitoring() {
          timer?.invalidate()
          timer = nil
      }

      private func updateAudioLevel() {
          guard let audioRecorder = audioRecorder else { return }
          audioRecorder.updateMeters()

          // Get normalized power level (0.0 to 1.0)
          let power = audioRecorder.averagePower(forChannel: 0)
          normalizedPower = max(0, (power + 80) / 80) // Normalize the power to 0...1

          // Update phase for wave animation
          phase += 0.1
      }

    /// Stop Playback
    func stopPlayback() {
        audioPlayer?.stop()
        isPlaying = false
    }
}
