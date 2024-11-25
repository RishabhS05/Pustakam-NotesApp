//
//  AudioVisualer2.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 26/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import AVFoundation
import Accelerate

class AudioRecorder2: ObservableObject {
    private var audioEngine: AVAudioEngine!
    private var audioInputNode: AVAudioInputNode!
    private var fftAnalyzer: FFTAnalyzer!

    @Published var audioLevels: [CGFloat] = Array(repeating: 0.0, count: 50)

    init() {
        setupAudio()
    }

    // Setup AVAudioEngine to capture audio input
    private func setupAudio() {
        audioEngine = AVAudioEngine()
        audioInputNode = audioEngine.inputNode
        
        // Set up FFT analyzer (to process the frequencies)
        fftAnalyzer = FFTAnalyzer()

        let bus = 0
        let inputFormat = audioInputNode.inputFormat(forBus: bus)

        audioInputNode.installTap(onBus: bus, bufferSize: 1024, format: inputFormat) { [weak self] buffer, time in
            self?.fftAnalyzer.processAudio(buffer: buffer)
            self?.updateAudioLevels()
        }

        do {
            try audioEngine.start()
        } catch {
            print("Error starting audio engine: \(error.localizedDescription)")
        }
    }

    // Update audio levels based on FFT analysis
    private func updateAudioLevels() {
        let levels = fftAnalyzer.getFrequencyLevels()
        DispatchQueue.main.async {
            self.audioLevels = levels
        }
    }

    // Start recording
    func startRecording() {
        do {
            try audioEngine.start()
        } catch {
            print("Error starting audio engine: \(error.localizedDescription)")
        }
    }

    // Stop recording
    func stopRecording() {
        audioEngine.stop()
    }
}


// FFTAnalyzer to process the audio data and extract frequency levels
class FFTAnalyzer {
    private var fftSetup: FFTSetup?
        private var log2n: UInt
        private var bufferSize: UInt
        private var halfBufferSize: Int

        private var realp: [Float]
        private var imagp: [Float]
        private var splitComplex: DSPSplitComplex

        init(bufferSize: UInt = 1024) {
            self.bufferSize = bufferSize
            self.halfBufferSize = Int(bufferSize / 2)
            self.log2n = UInt(log2(Double(bufferSize)))

            // Initialize real and imaginary arrays for FFT processing
            self.realp = [Float](repeating: 0.0, count: halfBufferSize)
            self.imagp = [Float](repeating: 0.0, count: halfBufferSize)

            // Create DSPSplitComplex structure
            self.splitComplex = DSPSplitComplex(realp: &realp, imagp: &imagp)

            // Setup FFT
            self.fftSetup = vDSP_create_fftsetup(log2n, FFTRadix(kFFTRadix2))
        }
    
    @Published var audioLevels: [CGFloat] = Array(repeating: 0.0, count: 50)
    
        
        // Set up the FFT analysis for the audio buffer
    private func setupFFT() {
        log2n = UInt(log2(Double(bufferSize)))
        fftSetup = vDSP_create_fftsetup(log2n, Int32(kFFTRadix2))
    }

    // Process the audio buffer and get frequency levels
    func processAudio(buffer: AVAudioPCMBuffer) -> [CGFloat] {
            guard let channelData = buffer.floatChannelData?[0] else { return [] }
            let frameCount = Int(buffer.frameLength)

            // Ensure the frameCount doesn't exceed our buffer size
            let input = Array(UnsafeBufferPointer(start: channelData, count: frameCount))

            // Copy input to real part of DSPSplitComplex, zero out imaginary part
            for i in 0..<min(frameCount, halfBufferSize) {
                 realp[i] = input[i]
             }
        
            imagp = [Float](repeating: 0.0, count: halfBufferSize)

            // Perform FFT
            vDSP_fft_zip(fftSetup!, &splitComplex, 1, log2n, FFTDirection(FFT_FORWARD))

            // Compute magnitude
            var magnitudes = [Float](repeating: 0.0, count: halfBufferSize)
            vDSP_zvmags(&splitComplex, 1, &magnitudes, 1, vDSP_Length(halfBufferSize))

            // Normalize magnitudes
            var normalizedMagnitudes = [Float](repeating: 0.0, count: halfBufferSize)
            var scalingFactor: Float = 1.0 / Float(bufferSize)
            vDSP_vsmul(&magnitudes, 1, &scalingFactor, &normalizedMagnitudes, 1, vDSP_Length(halfBufferSize))

            // Convert to CGFloat and return
            return normalizedMagnitudes.map { CGFloat($0) }
        }

    // Update audio levels
    func updateAudioLevels(levels: [CGFloat]) {
        // Process levels to match UI visualization range, e.g., max value normalization
        let scaledLevels = levels.map { min($0, 150.0) }
        DispatchQueue.main.async {
            self.audioLevels = scaledLevels
        }
    }

    // Get frequency levels
    func getFrequencyLevels() -> [CGFloat] {
        return audioLevels
    }
    
    deinit {
        // Clean up FFT setup when done
        vDSP_destroy_fftsetup(fftSetup)
    }

}


struct AudioVisualizrView: View {
    @ObservedObject var recorder = AudioRecorder2()

    var body: some View {
        ZStack {
            // Background
            Color.black.edgesIgnoringSafeArea(.all)

            // Horizontal timeline
            Rectangle()
                .frame(height: 1)
                .foregroundColor(.gray)
                .offset(y: -50)

            // Vertical origin line
            Rectangle()
                .frame(width: 2, height: 300)
                .foregroundColor(.white)
                .offset(x: 0)

            // Pulse waveform based on audio levels
            HStack(spacing: 4) {
                ForEach(recorder.audioLevels.indices, id: \.self) { index in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color.blue)
                        .frame(width: 6, height: recorder.audioLevels[index])
                }
            }
            .padding(.leading, 150) // Push pulses to the left
        }
        .onAppear {
            recorder.startRecording()
        }
        .onDisappear {
            recorder.stopRecording()
        }
    }
}

