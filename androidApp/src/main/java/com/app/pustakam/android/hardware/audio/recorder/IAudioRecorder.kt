package com.app.pustakam.android.hardware.audio.recorder

import java.io.File

interface IAudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun pause()
    fun resume()
}