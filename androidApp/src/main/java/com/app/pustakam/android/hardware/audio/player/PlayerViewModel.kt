package com.app.pustakam.android.hardware.audio.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.app.pustakam.android.services.mediaSessionService.MediaServiceListener
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.noteContentRepo.NoteContentRepository
import com.app.pustakam.extensions.getTimerFormatedString
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

sealed interface AudioPlayingIntent{
    data object PlayOrPauseIntent : AudioPlayingIntent
    data object ResumeIntent : AudioPlayingIntent
    data object RestartIntent : AudioPlayingIntent
    data class SeekToIntent(val position : Float) : AudioPlayingIntent
    data object SeekNextIntent : AudioPlayingIntent
    data object StopIntent : AudioPlayingIntent
    data object DeleteRecordingIntent : AudioPlayingIntent
    data class SelectedAudioChange(val index : Int) : AudioPlayingIntent
    data class UpdateProgress(val newProgress : Float) : AudioPlayingIntent
    data object Backward : AudioPlayingIntent
    data object Forward : AudioPlayingIntent

}

sealed class PlayerUiState {
    data object Initial : PlayerUiState()
    data class Ready(val duration: Long) : PlayerUiState()
    data class Progress(val progress: Long) : PlayerUiState()
    data class Buffering(val progress: Long) : PlayerUiState()
    data class Playing(val isPlaying: Boolean) : PlayerUiState()
    data class CurrentPlaying(val mediaItemIndex: Int) : PlayerUiState()
}

data class AudioUiState(
    val duration : Long = 0,
    val progress: Long = 0,
    val timerRemaining : String = "--:--",
    val isPlaying : Boolean = false ,
    val isServiceIsRunning : Boolean = false,
    val currentPlayingAudio : NoteContentModel? = null,
    val showDeleteDialog : Boolean = false,
    val contentList:MutableList<NoteContentModel> = mutableListOf()
)
@Suppress("IMPLICIT_CAST_TO_ANY")
class PlayMediaViewModel : ViewModel(), KoinComponent {
    private val noteRepository = get<NoteContentRepository>()
    private val mediaServiceListener = get<MediaServiceListener>()
    private  val _playerUiState  = MutableStateFlow(AudioUiState())
    val state = _playerUiState.asStateFlow()
    init {
        viewModelScope.launch {
            noteRepository.selectedNote.collectLatest {notesContents->
            if(notesContents.isNotEmpty()) {
                _playerUiState.update {
                    it.copy(contentList = notesContents,)
                }
                updateContent(noteContentModel = notesContents[0])
            }
            }
        }
    }
    init {
        viewModelScope.launch {
            mediaServiceListener.audioState.collectLatest {
                when (it) {
                    PlayerUiState.Initial -> {}
                    is PlayerUiState.Buffering -> progressCalculation(it.progress)
                    is PlayerUiState.Playing ->  _playerUiState.update { it.copy(isPlaying = it.isPlaying) }
                    is PlayerUiState.Progress -> progressCalculation(it.progress)
                    is PlayerUiState.CurrentPlaying -> { selectedAudioChanged(it.mediaItemIndex) }
                    is PlayerUiState.Ready -> {
                        _playerUiState.update { it.copy(duration = it.duration) }
                    }
                }
            }
        }
    }


    private fun progressCalculation(currentProgress: Long) {
       val  progress =
            if (currentProgress > 0) ((currentProgress.toFloat() / _playerUiState.value.duration.toFloat()) * 100f)
            else 0f
        _playerUiState.update { it.copy(
            progress =  progress.toLong(),
            timerRemaining = currentProgress.getTimerFormatedString()
            ) }
    }
    fun onPlayingIntent(audioPlayingIntent: AudioPlayingIntent){
        viewModelScope.launch {
        when (audioPlayingIntent) {
            AudioPlayingIntent.PlayOrPauseIntent -> play()
            AudioPlayingIntent.RestartIntent -> restartPlaying()
            AudioPlayingIntent.ResumeIntent -> resumePlaying()
            AudioPlayingIntent.SeekNextIntent -> seekNext()
            is AudioPlayingIntent.SeekToIntent -> { seekTo(audioPlayingIntent.position) }
            AudioPlayingIntent.StopIntent -> stopPlaying()
            AudioPlayingIntent.DeleteRecordingIntent -> deleteMedia()
            AudioPlayingIntent.Backward -> backward()
            AudioPlayingIntent.Forward -> forward()
            is AudioPlayingIntent.SelectedAudioChange -> selectedAudioChanged(audioPlayingIntent.index)
            is AudioPlayingIntent.UpdateProgress -> updateProgress(audioPlayingIntent.newProgress)
        }
    } }
    private suspend fun  selectedAudioChanged(index : Int){
        _playerUiState.update { it.copy(currentPlayingAudio = it.contentList[index]) }
    }

    private suspend fun backward(){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.Backward)
    }

    private suspend fun  forward(){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.Forward)
    }
    private suspend fun updateProgress(newProgress: Float){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.UpdateProgress(newProgress))
        _playerUiState.update { it.copy(progress = newProgress.toLong()) }
    }

    private suspend fun play(){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.PlayOrPauseIntent)
        _playerUiState.update { it.copy(isPlaying = true, isServiceIsRunning =  true) }
    }

    private suspend fun seekTo(position: Float) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.SeekToIntent(position))
        _playerUiState.update { it.copy() }
    }
    private suspend fun seekNext(){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.SeekNextIntent)
    }
    private suspend fun stopPlaying() {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.StopIntent)
    }
    private suspend fun resumePlaying(){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.ResumeIntent)
    }
    private suspend fun restartPlaying(){
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.RestartIntent)
    }
    private suspend fun deleteMedia() {
//        _audioState.value.file?.delete()
    }

    fun updateContent(noteContentModel: NoteContentModel){
        _playerUiState.update {
            it.copy(currentPlayingAudio=  noteContentModel,
            contentList = it.contentList
            )
        }
        when {
            noteContentModel is NoteContentModel.AudioContent-> {
                val mediaItem = MediaItem.Builder().setMediaId(noteContentModel.id)
                    .setUri(noteContentModel.localPath)
                    .setTag(noteContentModel.title)
                    .build()
                mediaServiceListener.addMediaItem(media = mediaItem)
            }
            noteContentModel is NoteContentModel.VideoContent -> {
                val mediaItem = MediaItem.Builder().setMediaId(noteContentModel.id)
                    .setUri(noteContentModel.localPath)
                    .setTag(noteContentModel.title)
                    .build()
                mediaServiceListener.addMediaItem(media = mediaItem)
            }
        }
    }

    fun showDeleteAlert(value: Boolean) {
        _playerUiState.update { it.copy(showDeleteDialog = value) }
    }

    override fun onCleared() {
        _playerUiState.update { it.copy(isPlaying = false, isServiceIsRunning =  false) }
        viewModelScope.launch {
            mediaServiceListener.onPlayerEvents(AudioPlayingIntent.StopIntent)
        }
        super.onCleared()
    }
}