    //
    //  AudioRecorderView.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 24/11/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //
import SwiftUI

import SwiftUI
import AVFoundation

struct AudioRecorderView : View {
    @StateObject private var audioRecorder = AudioRecorder()
    @StateObject private var audioLevelsMonitor = AudioLevelsMonitor()
    
    @State private var elapsedTime: TimeInterval = 0.0
    @State private var timer: Timer? = nil
    @State private var showRenameSheet = false
    @State private var fileName = ""
    @State private var tempFileName = ""
    
    let onDismiss: (() -> Void) = { }
    var body: some View {
        ZStack {
                // Background gradient
            LinearGradient(gradient: Gradient(colors: [Color.green.opacity(0.6), Color.red.opacity(0.6)]),
                           startPoint: .top,
                           endPoint: .bottom)
            .edgesIgnoringSafeArea(.all)
            
            VStack(spacing: 40) {
                    // Timer Display
                Text(String(format: "%02d : %02d . %02d",
                            Int(elapsedTime / 60),
                            Int(elapsedTime.truncatingRemainder(dividingBy: 60)),
                            Int((elapsedTime * 100).truncatingRemainder(dividingBy: 100))))
                .font(.largeTitle.monospacedDigit())
                .foregroundColor(.white)
                    // Real-time Wave Animation
                AudioVisualizerView(audioLevelsMonitor: audioLevelsMonitor)
                .frame(height: 150)
                .padding()
                .background(LinearGradient(colors: [Color.green.opacity(0.4), Color.red.opacity(0.3)], startPoint: .top, endPoint: .bottom))
                .cornerRadius(20)
                .frame(height: 100)
                
                    
                // Record Button
                HStack {
                        // Play Button
                    Button(action: {
                        if !audioRecorder.isPlaying {
                            playRecording()
                        }
                    }) {
                        Image(systemName: audioRecorder.isPlaying ? "pause.fill" : "play.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 50, height: 50)
                            .padding()
                            .background(Color.blue)
                            .clipShape(Circle())
                            .foregroundColor(.white)
                    }
                    .disabled(audioRecorder.audioFileURL == nil) // Disable if there's no recorded file
                    
                        // Stop Playback Button
                    Button(action: {
                        if audioRecorder.isRecording {
                            stopRecording()
                        } else {
                           startRecording()
                        }
                    }) {
                        Image(systemName: audioRecorder.isRecording  ? "stop.fill" : "mic.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 70, height: 70)
                            .padding()
                            .background(audioRecorder.isRecording  ? Color.red : Color.green)
                            .clipShape(Circle())
                            .foregroundColor(.white)
                    }
                    
                    Button(action: {
                        if audioRecorder.isPlaying  {
                            stopPlayback()
                        }
                    }) {
                        Image(systemName: "stop.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 50, height: 50)
                            .padding()
                            .background(Color.orange)
                            .clipShape(Circle())
                            .foregroundColor(.white)
                    }
                    .disabled(!audioRecorder.isPlaying)
                }
            }
        }
        
        .sheet(isPresented: $showRenameSheet) {
            RenameSheetView(fileName: $fileName, tempFileName: $tempFileName,
                            showRenameSheet: $showRenameSheet, audioRecorder: audioRecorder)
        }
        .onDisappear() {
            onDismiss()
        }
    }
        // Start Recording Function
    private func startRecording() {
        audioRecorder.startRecording()
        audioRecorder.isRecording  = true
        audioLevelsMonitor.startLevelsMonitoring()
        startTimer()
    }
        // Stop Recording Function
    private func stopRecording() {
        audioRecorder.stopRecording()
        audioLevelsMonitor.stopLevelsMonitoring()
        audioLevelsMonitor.loadAudioFile(url:audioRecorder.audioFileURL ?? nil)
        audioRecorder.isRecording  = false
        stopTimer()
        tempFileName = fileName
        showRenameSheet = true
    }
    private func playRecording(){
        audioRecorder.playRecording()
    }
    private func pausePlayback(){
        audioRecorder.togglePlayback()
    }
    private func stopPlayback(){
        audioRecorder.stopPlayback()
    }
    
        // Timer Functions
    private func startTimer() {
        elapsedTime = 0.0
        timer = Timer.scheduledTimer(withTimeInterval: 0.01, repeats: true) { _ in
            elapsedTime += 0.01
        }
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}


    // Rename Sheet View
struct RenameSheetView: View {
    @Binding var fileName: String
    @Binding var tempFileName: String
    @Binding var showRenameSheet: Bool
    var audioRecorder: AudioRecorder
    
    var body: some View {
        VStack {
            TextField("Enter new file name", text: $tempFileName)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            
            HStack {
                Button("Cancel") {
                    showRenameSheet = false
                }
                .padding()
                
                Spacer()
                
                Button("Save") {
                    if !tempFileName.isEmpty {
                        fileName = tempFileName
                        audioRecorder.saveRecording(withName: fileName)
                    }
                    showRenameSheet = false
                }
                .padding()
            }
        }
        .padding()
    }
}


struct BarView: View {
    let height: CGFloat
    
    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(Color.red)
            .frame(width: 4, height: height * 100)
    }
}



    //                ZStack {
    //
    //                    ForEach(0..<5, id: \.self) { i in
    //                        PulseShape(
    //                            amplitude: CGFloat(audioRecorder.normalizedPower) * (1 - CGFloat(i) * 0.2),
    //                            phase: CGFloat(audioRecorder.phase + Double(i) * 0.5)
    //                        )
    //                        .stroke(lineWidth: 4)
    //                        .foregroundColor(Color.gray.opacity(1 - Double(i) * 0.2))
    //                    }
// wave pattern
struct WaveShape: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    
    var animatableData: AnimatablePair<CGFloat, CGFloat> {
        get { AnimatablePair(amplitude, phase) }
        set {
            amplitude = newValue.first
            phase = newValue.second
        }
    }
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY
        
        for x in stride(from: 0, to: rect.width, by: 1) {
            let normalizedX = x / rect.width
            let angle = normalizedX * .pi * 2 + phase
            let y = midY + sin(angle) * amplitude * rect.height / 2
            if x == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        return path
    }
}

struct WaveProgressView: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    var progress: CGFloat

    var animatableData: AnimatablePair<CGFloat, CGFloat> {
        get { AnimatablePair(phase, progress) }
        set {
            phase = newValue.first
            progress = newValue.second
        }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY

        for x in stride(from: 0, to: rect.width * progress, by: 1) {
            let normalizedX = x / rect.width
            let angle = normalizedX * .pi * 2 + phase
            let y = midY + sin(angle) * amplitude * rect.height / 2
            if x == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }

        return path
    }
}


    // pulse pattern
struct PulseShape: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    
    var animatableData: AnimatablePair<CGFloat, CGFloat> {
        get { AnimatablePair(amplitude, phase) }
        set {
            amplitude = newValue.first
            phase = newValue.second
        }
    }
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY
        let pulseWidth: CGFloat = 5 // Width of each pulse
        let pulseSpacing: CGFloat = 10 // Space between pulses
        for x in stride(from: 0, to: rect.width, by: pulseSpacing) {
            let normalizedX = x / rect.width
            let angle = normalizedX * .pi * 2 + phase
            let pulseHeight = sin(angle) * amplitude * rect.height / 2
            
                // Draw a vertical line (pulse)
            let pulseStart = CGPoint(x: x, y: midY - pulseHeight / 2)
            let pulseEnd = CGPoint(x: x, y: midY + pulseHeight / 2)
            path.move(to: pulseStart)
            path.addLine(to: pulseEnd)
        }
        return path
    }
}
