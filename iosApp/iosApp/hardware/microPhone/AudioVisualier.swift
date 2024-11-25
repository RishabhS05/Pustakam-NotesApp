//
//  AudioVisualier.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 26/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Combine

struct AudioVisualizerView: View {
    @State private var audioLevels: [CGFloat] = Array(repeating: 0.0, count: 50) // Represent the pulse waveform
    @State private var timer: AnyCancellable?

    var body: some View {
        ZStack {
            // Background
            Color.black.edgesIgnoringSafeArea(.all)

            // Horizontal timeline
            VStack {
                Spacer()
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(.gray)
                    .offset(y: -50)
            }

            // Vertical origin line
            Rectangle()
                .frame(width: 2, height: 200)
                .foregroundColor(.white)
                .offset(x: 0)

            // Pulse visualization
            HStack(spacing: 2) {
                ForEach(audioLevels.indices, id: \.self) { index in
                    Rectangle()
                        .fill(Color.blue)
                        .frame(width: 4, height: audioLevels[index])
                }
            }
            .offset(x: -100) // Offset the wave to the left
            .padding(.leading, 150)
        }
        .onAppear {
            startUpdatingAudio()
        }
        .onDisappear {
            stopUpdatingAudio()
        }
    }

    // Simulate audio wave updates
    private func startUpdatingAudio() {
        timer = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { _ in
                withAnimation(.linear(duration: 0.1)) {
                    let newLevel = CGFloat.random(in: 10...100) // Random height for wave
                    audioLevels.removeLast()
                    audioLevels.insert(newLevel, at: 0)
                }
            }
    }

    private func stopUpdatingAudio() {
        timer?.cancel()
    }
}

struct AudioVisualizerView_Previews: PreviewProvider {
    static var previews: some View {
        AudioVisualizerView()
    }
}
