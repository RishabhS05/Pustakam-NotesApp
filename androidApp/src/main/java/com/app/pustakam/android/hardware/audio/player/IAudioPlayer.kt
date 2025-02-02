package com.app.pustakam.android.hardware.audio.player


interface IAudioPlayer {
    fun play(outputFile : String)
    fun stop()
    fun pause()
    fun seekTo(progress: Long)
    fun seekNext()
    fun seekForward()
    fun seekBackward()
    fun dispose()
}