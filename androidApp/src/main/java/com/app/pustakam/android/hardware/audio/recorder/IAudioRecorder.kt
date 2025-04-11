package com.app.pustakam.android.hardware.audio.recorder

import android.media.MediaRecorder
import java.io.File

interface IAudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun pause()
    fun resume()
    fun getRecorder() : MediaRecorder?
}