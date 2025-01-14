package com.app.pustakam.android.hardware.audio

import java.io.File

interface IAudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun pause()
    fun resume()

}