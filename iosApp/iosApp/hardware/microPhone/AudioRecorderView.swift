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
                
                // Waveform Placeholder
                WaveformView()
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
        startTimer()
    }
    // Stop Recording Function
    private func stopRecording() {
        tempFileName = audioRecorder.fileName
        audioRecorder.stopRecording()
        audioRecorder.isRecording  = false
        stopTimer()
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

// Placeholder for Waveform View
struct WaveformView: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 10)
            .fill(Color.white.opacity(0.5))
            .overlay(
                SoundWaveAnimationView()
            )
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


struct SoundWaveAnimationView: View {
    @State private var waveData: [CGFloat] = Array(repeating: 0.5, count: 30)
    @State private var timer: Timer? = nil
    
    var body: some View {
        ZStack {
            LinearGradient(gradient: Gradient(colors: [Color.green.opacity(0.6), Color.red.opacity(0.6)]),
                           startPoint: .leading,
                           endPoint: .trailing)
                .edgesIgnoringSafeArea(.all)
            
            HStack(spacing: 4) {
                ForEach(0..<waveData.count, id: \.self) { index in
                    BarView(height: waveData[index])
                }
            }
        }
        .onAppear {
            startWaveAnimation()
        }
        .onDisappear {
            stopWaveAnimation()
        }
    }
    
    private func startWaveAnimation() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            withAnimation(.easeInOut(duration: 0.1)) {
                waveData = waveData.map { _ in
                    CGFloat.random(in: 0.2...1.0)
                }
            }
        }
    }
    
    private func stopWaveAnimation() {
        timer?.invalidate()
        timer = nil
    }
}

struct BarView: View {
    let height: CGFloat
    
    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(Color.black)
            .frame(width: 4, height: height * 100)
    }
}
