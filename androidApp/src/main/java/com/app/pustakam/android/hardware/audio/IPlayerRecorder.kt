package com.app.pustakam.android.hardware.audio

import java.io.File

interface IPlayerRecorder {
    fun start()
    fun stop()
    fun pause()
    fun resume()
}