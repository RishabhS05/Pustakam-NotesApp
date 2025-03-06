package com.app.pustakam.android.hardware.audio.recorder

import androidx.lifecycle.ViewModel
import com.app.pustakam.data.models.response.notes.NoteContentModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

data class AudioState(
    val file: File? = null,
    val showDeleteDialog : Boolean = false,
    val audioLifecycle : AudioLifecycle = AudioLifecycle.idle,
    val noteContentModel: NoteContentModel.MediaContent? = null
)
enum class AudioLifecycle {
    start ,stop, playing, resume, pause, idle
}
sealed interface AudioRecordingIntent {
    data object StartRecordingIntent : AudioRecordingIntent
    data class StopRecordingIntent(val duration : Long) : AudioRecordingIntent
    data object PauseRecordingIntent : AudioRecordingIntent
    data object ResumeRecordingIntent : AudioRecordingIntent
}


class AudioViewModel : ViewModel(), KoinComponent {
    private val _audioState = MutableStateFlow(AudioState())
    val state = _audioState.asStateFlow()
    private val audioRecorder = get<IAudioRecorder>()
    fun updateContent(noteContentModel: NoteContentModel.MediaContent){
        _audioState.update { it.copy(noteContentModel = noteContentModel) }
    }
    fun showDeleteAlert( value : Boolean){
        _audioState.update { it.copy(showDeleteDialog = value) }
    }
    fun handleIntent(audioRecordingIntent: AudioRecordingIntent) {
        when (audioRecordingIntent) {
            AudioRecordingIntent.PauseRecordingIntent -> pauseRecording()

            AudioRecordingIntent.ResumeRecordingIntent -> resumeRecording()

            AudioRecordingIntent.StartRecordingIntent ->
                _audioState.value.noteContentModel?.let { startRecording(it.localPath) }

           is AudioRecordingIntent.StopRecordingIntent ->{
               stopRecording(audioRecordingIntent.duration)
           }
        }
    }

  private fun startRecording(filePath: String? = null) {
        val file = filePath?.let { File(it) }
        _audioState.update { it.copy(file = file, audioLifecycle = AudioLifecycle.start) }
        with(audioRecorder) { file?.let { start(it) } }
    }

  private fun stopRecording(duration: Long) {
        audioRecorder.stop()
        _audioState.update { it.copy(audioLifecycle = AudioLifecycle.stop,
            noteContentModel = it.noteContentModel?.copy(duration = duration),) }
    }

   private fun pauseRecording() {
       _audioState.update { it.copy(audioLifecycle = AudioLifecycle.pause) }
        audioRecorder.pause()
    }
    private fun resumeRecording() {
        audioRecorder.resume()
        _audioState.update { it.copy(audioLifecycle = AudioLifecycle.resume) }
    }
}

