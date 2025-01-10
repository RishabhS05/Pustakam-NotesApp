package com.app.pustakam.android.hardware.audio

import androidx.lifecycle.ViewModel
import com.app.pustakam.android.services.mediaPlayerServices.MediaItemTree
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
    val audioMode : AudioMode= AudioMode.none,
    val noteContentModel: NoteContentModel.AudioContent? = null
)

fun NoteContentModel.AudioContent.toMediaItem() : MediaItemTree? {
    return null
}

enum class AudioMode {
    start ,stop, playing, resume, pause, none
}
sealed interface AudioRecordingIntent {
    data object StartRecordingIntent : AudioRecordingIntent
    data class StopRecordingIntent(val duration : Long) : AudioRecordingIntent
    data object PauseRecordingIntent : AudioRecordingIntent
    data object ResumeRecordingIntent : AudioRecordingIntent
    data object DeleteRecordingIntent : AudioRecordingIntent
}

class AudioViewModel : ViewModel(), KoinComponent {
    private val _audioState = MutableStateFlow(AudioState())
    val state = _audioState.asStateFlow()
    private val audioRecorder = get<IAudioRecorder>()
    fun updateContent(noteContentModel: NoteContentModel.AudioContent){
        _audioState.update { it.copy(noteContentModel = noteContentModel) }
    }
    fun showDeleteAlert( value : Boolean){
        _audioState.update { it.copy(showDeleteDialog = value) }
    }
    fun handleIntent(audioRecordingIntent: AudioRecordingIntent) {
        when (audioRecordingIntent) {
            AudioRecordingIntent.DeleteRecordingIntent -> deleteRecording()

            AudioRecordingIntent.PauseRecordingIntent -> pauseRecording()

            AudioRecordingIntent.ResumeRecordingIntent -> resumeRecording()

            AudioRecordingIntent.StartRecordingIntent -> _audioState.value.noteContentModel?.let { startRecording(it) }

           is AudioRecordingIntent.StopRecordingIntent ->{
               stopRecording(audioRecordingIntent.duration)
           }
        }
    }

  private fun startRecording(noteContentModel: NoteContentModel.AudioContent) {
        val file = noteContentModel.localPath?.let { File(it) }
        _audioState.update { it.copy(file = file, audioMode = AudioMode.start) }
        with(audioRecorder) { file?.let { start(it) } }
    }

  private  fun stopRecording(duration: Long) {
        audioRecorder.stop()
        _audioState.update { it.copy(audioMode = AudioMode.stop,
            noteContentModel = it.noteContentModel?.copy(isRecorded = true, duration = duration),) }
    }

   private fun pauseRecording() {
       _audioState.update { it.copy(audioMode = AudioMode.pause) }
        audioRecorder.pause()
    }

   private fun deleteRecording() {
        _audioState.value.file?.delete()
    }

    private fun resumeRecording() {
        audioRecorder.resume()
        _audioState.update { it.copy(audioMode = AudioMode.resume) }
    }
}

sealed interface AudioPlayingIntent{
    data object PlayIntent : AudioPlayingIntent
    data object ResumeIntent : AudioPlayingIntent
    data object RestartIntent : AudioPlayingIntent
    data object SeekToIntent : AudioPlayingIntent
    data object SeekNextIntent : AudioPlayingIntent
    data object StopIntent : AudioPlayingIntent
    data object PauseIntent : AudioPlayingIntent
    data object DeleteRecordingIntent : AudioPlayingIntent
}
class PlayMediaViewModel: ViewModel(),KoinComponent {
    private val _audioState = MutableStateFlow(AudioState())
    val state = _audioState.asStateFlow()
    private val player: IAudioPlayer = get<IAudioPlayer>()
    fun onPlayingIntent(audioPlayingIntent: AudioPlayingIntent){
        when(audioPlayingIntent){
            AudioPlayingIntent.PauseIntent -> stopPlaying()
            AudioPlayingIntent.PlayIntent -> play()
            AudioPlayingIntent.RestartIntent -> restartPlaying()
            AudioPlayingIntent.ResumeIntent -> resumePlaying()
            AudioPlayingIntent.SeekNextIntent -> seekNext()
            AudioPlayingIntent.SeekToIntent -> seekTo()
            AudioPlayingIntent.StopIntent -> stopPlaying()
            AudioPlayingIntent.DeleteRecordingIntent -> deleteMedia()
        }
    }
    private fun play(){}
    private fun seekTo(){}
    private fun seekNext(){}
    private fun stopPlaying() {}
    private fun resumePlaying(){}
    private fun restartPlaying(){}
    private fun deleteMedia() {
        _audioState.value.file?.delete()
    }

    fun updateContent(noteContentModel: NoteContentModel.AudioContent){
        _audioState.update { it.copy(noteContentModel = noteContentModel) }
    }

    fun showDeleteAlert(value: Boolean) {
        _audioState.update { it.copy(showDeleteDialog = value) }
    }

}