package com.app.pustakam.android.hardware.audio

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

import java.io.File

class AudioPlayer : IAudioPlayer, KoinComponent{
    private var exoPlayer : ExoPlayer = get<ExoPlayer>()
    override fun play(outputFile: String) {
        exoPlayer.apply {
            setMediaItem(MediaItem.fromUri(outputFile))
            prepare()
            play()
        }
    }

    override fun stop() {
        exoPlayer.stop()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun seekTo(progress :Long) {
        exoPlayer.seekTo(progress)
    }

    override fun seekNext() {
       exoPlayer.seekToNext()
    }

    override fun seekForward() {
        exoPlayer.seekForward()
    }

    override fun seekBackward() {
        exoPlayer.seekBack()
    }
    override fun dispose(){
        exoPlayer.release()
    }
}