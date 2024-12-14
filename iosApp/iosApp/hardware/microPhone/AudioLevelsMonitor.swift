//
//  AudioLevelsMonitor.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 26/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import AVFoundation

class AudioLevelsMonitor : ObservableObject {
    private var audioEngine: AVAudioEngine = AVAudioEngine()
    private var inputNode: AVAudioInputNode?
    
        // Array to hold audio levels over time
         @Published var audioLevels: [CGFloat] = []
         private let bufferSize: AVAudioFrameCount = 1024
        // Function to read and process recorded audio
    
        func loadAudioFile(url: URL?) {
            guard let url else { return }
            audioLevels.removeAll()
            do {
                // Open the audio file
                let audioFile = try AVAudioFile(forReading: url)
                let format = audioFile.processingFormat
                let frameCount = AVAudioFrameCount(audioFile.length)
                
                // Create a buffer to hold the audio data
                guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frameCount) else { return }
                try audioFile.read(into: buffer)
                
                // Process the buffer in chunks
                let totalFrames = Int(buffer.frameLength)
                for startFrame in stride(from: 0, to: totalFrames, by: Int(bufferSize)) {
                    let chunkSize = min(Int(bufferSize), totalFrames - startFrame)
                    
                    // Create a sub-buffer for processing
                    let chunkBuffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(chunkSize))!
                    chunkBuffer.frameLength = AVAudioFrameCount(chunkSize)
                    
                    let sourcePointer = buffer.floatChannelData!.pointee.advanced(by: startFrame)
                    let destinationPointer = chunkBuffer.floatChannelData!.pointee
                    destinationPointer.update(from: sourcePointer, count: chunkSize)
                    
                    // Calculate the level for this chunk
                    let level = processAudioBuffer(chunkBuffer)
                    audioLevels.append(level)
                }
            } catch {
                print("Error loading audio file: \(error.localizedDescription)")
            }
        }
    
    func startLevelsMonitoring() {
        inputNode = audioEngine.inputNode
        let recordingFormat = inputNode?.outputFormat(forBus: 0)
        inputNode?.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            let level = self.processAudioBuffer(buffer)
            DispatchQueue.main.async {
                self.updateLevels(level)
            }
        }
        try? audioEngine.start()
    }
    
    func stopLevelsMonitoring() {
        inputNode?.removeTap(onBus: 0)
        audioEngine.stop()
    }
    
    private func processAudioBuffer(_ buffer: AVAudioPCMBuffer) -> CGFloat {
            // Ensure the buffer has valid channel data
                guard let channelData = buffer.floatChannelData else {
                    return 0.0
                }

                // Create an array from the channel data
                let frameCount = Int(buffer.frameLength)
                let channelDataArray = Array(UnsafeBufferPointer(start: channelData.pointee, count: frameCount))

                // Map over the array and calculate the absolute values
                let channelDataValue = channelDataArray.map { abs($0) }

                // Calculate the average level
                let averageLevel = channelDataValue.reduce(0, +) / Float(frameCount)
                
                // Scale the level for visualization (adjust the scaling factor as needed)
                return CGFloat(averageLevel) * 500
    }
    
    private func updateLevels(_ level: CGFloat) {
        audioLevels.append(level)
            // Add the new level to the array
        }
}
