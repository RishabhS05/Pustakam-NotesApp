import Foundation
import AVFoundation

class AudioRecorder: ObservableObject {
    @Published var isRecording = false
    @Published var normalizedPower: Float = 0.0
    @Published var phase: Double = 0.0
    var audioFileURL: URL?
    private var audioRecorder: AVAudioRecorder?
    private var timer: Timer?
    /// Start Recording
    func startRecording() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord,
                                    mode: .default,
                                    options: [.defaultToSpeaker])
            try session.setActive(true)
            let fileURL = FileManager.default.temporaryDirectory
                .appendingPathComponent("audio.m4a")
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
            audioFileURL = audioRecorder?.url

            isRecording = true
            startTimerMonitoring()
        } catch {
            print("Failed to start recording: \(error)")
        }
    }
    func pauseRecording(){
        isRecording = false
        audioRecorder?.pause()
        stopTimerMonitoring()
       
    }
    func resumeRecording(){
        isRecording = true
        audioRecorder?.record()
        startTimerMonitoring()
    }
    
    /// Stop Recording
    func stopRecording() {
        audioFileURL = audioRecorder?.url
        audioRecorder?.stop()
        isRecording = false
        stopTimerMonitoring()
    }
    
    func reset(){ audioRecorder = nil }
   
    
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
}
