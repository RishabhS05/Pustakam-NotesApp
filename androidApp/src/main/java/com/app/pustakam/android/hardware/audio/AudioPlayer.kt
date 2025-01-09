package com.app.pustakam.android.hardware.audio

import androidx.media3.exoplayer.ExoPlayer
import java.io.File

class AudioPlayer : IAudioPlayer{
    private var exoPlayer : ExoPlayer? = null
    init {
        exoPlayer = configurePlayer()
    }
    override fun start(outputFile: File) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
    }
   private fun configurePlayer() : ExoPlayer =
       TODO("Not yet implemented")
       exoPlayer !!

}