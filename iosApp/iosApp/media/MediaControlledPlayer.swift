//
//  MediaControlledPlayer.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 26/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import AVKit
class MediaControlledPlayer: AVPlayer {
    var mediaCallback: ((Bool)-> Void)? // true = play, false = pause

    override func play() {
        super.play()
        mediaCallback?(true) // 🔥 call your manager
    }
    
    override func pause() {
        super.pause()
        print("🎯 Hijacked system PAUSE")
        mediaCallback?(false) // 🔥 call your manager
    }
}
