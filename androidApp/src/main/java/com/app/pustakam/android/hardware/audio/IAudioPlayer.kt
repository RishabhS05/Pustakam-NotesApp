package com.app.pustakam.android.hardware.audio

import java.io.File

interface IAudioPlayer {
    fun start(outputFile : File)
    fun stop()
    fun pause()
    fun seekTo(progress: Long)
    fun seekNext()
    fun seekForward()
    fun seekBackward()
}