package com.app.pustakam.android.hardware.audio.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

data class AudioState(
    val file: File? = null,
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
    private val _audioLevels = MutableStateFlow<List<Float>>(emptyList())
    val audioLevels: StateFlow<List<Float>> = _audioLevels.asStateFlow()
    fun updateContent(noteContentModel: NoteContentModel.MediaContent){
        _audioState.update { it.copy(noteContentModel = noteContentModel, audioLifecycle = AudioLifecycle.start) }
        handleIntent(AudioRecordingIntent.StartRecordingIntent)
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

    suspend fun updateAudioLevels() {
        while (true) {
            val amplitude = audioRecorder.getRecorder()?.maxAmplitude ?: 0
            val normalizedLevel = amplitude / 32768f  // Normalize (0 to 1)
            _audioLevels.value = (_audioLevels.value + normalizedLevel).takeLast(50)  // Keep last 50 samples
            delay(100)  // Update every 100ms
        }
    }
  private fun startRecording(filePath: String? = null) {
        val file = filePath?.let { File(it) }
        _audioState.update { it.copy(file = file, audioLifecycle = AudioLifecycle.start) }
        with(audioRecorder) { file?.let {
            start(it)
            viewModelScope.launch { updateAudioLevels() }
        } }
    }

  private fun stopRecording(duration: Long) {
        audioRecorder.stop()
        _audioState.update { it.copy(audioLifecycle = AudioLifecycle.stop,
            noteContentModel = it.noteContentModel?.copy(duration = duration),) }
    }

    // 🔧 15-Jul-2026: CRASH FIX (duplicate LazyColumn key) — the View calls this right after it
    //   hands the finished recording to onStop. Resetting to idle guarantees the retained
    //   ViewModel's stale `stop` state can never re-deliver the same content on a later
    //   recomposition or when the recorder is opened again.
    fun consumeStop() {
        _audioState.update { it.copy(audioLifecycle = AudioLifecycle.idle, noteContentModel = null) }
    }

   private fun pauseRecording() {
       _audioState.update { it.copy(audioLifecycle = AudioLifecycle.pause) }
        audioRecorder.pause()
    }
    private fun resumeRecording() {
        audioRecorder.resume()
        _audioState.update { it.copy(audioLifecycle = AudioLifecycle.resume) }
        viewModelScope.launch { updateAudioLevels() }
    }
}

