    //
    //  AudioRecorderView.swift
    //  iosApp
    //
    //  Created by Rishabh Shrivastava on 24/11/24.
    //  Copyright © 2024 orgName. All rights reserved.
    //
import SwiftUI
import AVFoundation

struct AudioRecorderView : View {
    private var path = ""
    @StateObject private var audioRecorder = AudioRecorder()
    @StateObject private var audioLevelsMonitor = AudioLevelsMonitor()
    @State private var elapsedTime: TimeInterval = 0.0
    @State private var timer: Timer? = nil
    @State private var showRenameSheet = false
    @State private var fileName = ""
    @State private var tempFileName = ""
    let onDismiss: (() -> Void) = { }
    var body: some View {
            HStack(spacing: 8) {
                Image(systemName: "microphone.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .padding(8)
                    .background(Color.red)
                    .clipShape(Circle())
                    .foregroundColor(.white)
                
                    // Timer Display
                Text(String(format: "%02d : %02d . %02d",
                            Int(elapsedTime / 60),
                            Int(elapsedTime.truncatingRemainder(dividingBy: 60)),
                            Int((elapsedTime * 100).truncatingRemainder(dividingBy: 100))))
                .font(.headline.monospacedDigit())
                .foregroundColor(.brown)
                    // Real-time Wave Animation
                AudioVisualizerView(audioLevelsMonitor: audioLevelsMonitor)
                .frame(height: 30)
                .padding()
                        // Play Button
                    Button(action: {
                        if audioRecorder.isRecording {
                            pauseRecording()
                        } else {
                            resumeRecording()
                        }
                    }) {
                        Image(systemName: audioRecorder.isRecording ? "pause.fill" : "play.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundColor(.brown)
                    }
                    .disabled(audioRecorder.audioFileURL == nil)
                    .padding(8)
                    // Disable if there's no recorded file
                        // Stop Playback Button
                    Button(action: {
                        stopRecording()
                    }) {
                        Image(systemName: "stop.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundColor(.brown)
                    }
                }
            .frame(height: 40)
            .padding(8)
            .background(Theme.Colors.onSurface)
            .cornerRadius(12)
            .shadow(radius: 12)
            .padding(8)
        .sheet(isPresented: $showRenameSheet) {
            RenameSheetView(fileName: $fileName, tempFileName: $tempFileName,
                            showRenameSheet: $showRenameSheet, audioRecorder: audioRecorder)
        }.onAppear(){
            startRecording()
        }
        .onDisappear() {
            onDismiss()
        }
    }
        // Start Recording Function
    private func startRecording() {
        audioRecorder.setPath(value: path)
        audioRecorder.startRecording()
        audioLevelsMonitor.startLevelsMonitoring()
        startTimer()
    }
    
    private func pauseRecording(){
        audioRecorder.pauseRecording()
        stopTimer()
    }
    private func resumeRecording(){
        audioRecorder.resumeRecording()
        startTimer()
    }
    
        // Stop Recording Function
    private func stopRecording() {
        audioRecorder.stopRecording()
        audioLevelsMonitor.stopLevelsMonitoring()
        audioLevelsMonitor.loadAudioFile(url:audioRecorder.audioFileURL ?? nil)
        stopTimer()
        tempFileName = fileName
        showRenameSheet = true
    }
 
//    private func playRecording(){
//        audioRecorder.playRecording()
//    }
//    private func pausePlayback(){
//        audioRecorder.togglePlayback()
//    }
//    private func stopPlayback(){
//        audioRecorder.stopPlayback()
//    }
    
        // Timer start or pause Functions
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.01, repeats: true) { _ in
            elapsedTime += 0.01
        }
    }
    // this will call only when recording is completed
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

#Preview{
    AudioRecorderView()
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
