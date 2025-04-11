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
        ScrollView(.horizontal, showsIndicators: false) {
            ScrollViewReader { scrollView in
                HStack(alignment: .center, spacing: 2) {
                    ForEach(Array(audioLevelsMonitor.audioLevels.enumerated()), id: \.offset) { index, level in
                        Capsule()
                            .fill(Color.red)
                            .frame(width: 2, height: max(4, level))
                            .id(index)
                    }
                }
                .onChange(of: audioLevelsMonitor.audioLevels.count) { newValue in
                                        if let lastIndex = audioLevelsMonitor.audioLevels.indices.last {
                                            DispatchQueue.main.async {
                                                withAnimation {
                                                    scrollView.scrollTo(lastIndex, anchor: .center)
                                                }
                                            }
                                        }
                                    }
            }
        }
    }
}
