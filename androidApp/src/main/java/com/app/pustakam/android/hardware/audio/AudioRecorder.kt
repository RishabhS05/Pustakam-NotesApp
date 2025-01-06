package com.app.pustakam.android.hardware.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(
    private  val context  : Context
) : IAudioRecorder {
    private var recorder: MediaRecorder? = null
    override fun start(outputFile: File) {
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(outputFile).fd)
            prepare()
            start()
            recorder = this
        }

    }
    override fun stop() {
        recorder?.stop()
        recorder?.reset()
        recorder = null
    }

    override fun pause() {
      recorder?.pause()
    }

    override fun resume() {
        recorder?.resume()
    }

    private fun createRecorder() : MediaRecorder{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }
}