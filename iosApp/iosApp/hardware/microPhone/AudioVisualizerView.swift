//
//  AudioVisualizerView.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 26/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//


import SwiftUI

struct AudioVisualizerView: View {
    @ObservedObject var audioLevelsMonitor : AudioLevelsMonitor
    var body: some View {
        ZStack {
                // Timeline with pulse
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .center, spacing: 2) {
                    ForEach(Array(audioLevelsMonitor.audioLevels.enumerated()), id: \.offset) { index, level in
                        Capsule()
                            .fill(Color.red)
                            .frame(width: 2, height: max(4, level))
                    }
                }
            } .frame(maxWidth: .infinity, maxHeight: 300)
        }
    }
}
