package com.app.pustakam.android.hardware.audio.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.extension.toMediaItem
import com.app.pustakam.android.hardware.audio.recorder.AudioLifecycle
import com.app.pustakam.android.services.mediaSessionService.MediaServiceListener
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.noteContentRepo.NoteContentRepository
import com.app.pustakam.extensions.getReadableDuration
import com.app.pustakam.extensions.getTimerFormatedString
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.extensions.timerRemaining
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

sealed interface AudioPlayingIntent {
    data class PlayOrPauseIntent(val mediaId: String) : AudioPlayingIntent
    data class ResumeIntent(val mediaId: String) : AudioPlayingIntent
    data class RestartIntent(val mediaId: String) : AudioPlayingIntent
    data class SeekToIntent(val position: Float, val mediaId: String) : AudioPlayingIntent
    data class SeekNextIntent(val mediaId: String) : AudioPlayingIntent
    data object StopIntent : AudioPlayingIntent
    data class SelectedAudioChange(val mediaId: String) : AudioPlayingIntent
    data class UpdateProgress(val newProgress: Float, val mediaId: String) : AudioPlayingIntent
    data object Backward : AudioPlayingIntent
    data object Forward : AudioPlayingIntent

}

sealed class PlayerState {
    data object Initial : PlayerState()
    data class Ready(val duration: Long,val mediaId: String?) : PlayerState()
    data class Progress(val progress: Long,val mediaId: String?) : PlayerState()
    data class Buffering(val progress: Long,val mediaId: String?) : PlayerState()
    data class Playing(val isPlaying: Boolean,val mediaId: String?) : PlayerState()
    data class CurrentPlaying(val mediaId: String) : PlayerState()

}

data class AudioUiState(
    val isServiceIsRunning: Boolean = false,
    val isPlaying : Boolean = false,
    val currentPlayingId: String? = null,
    val showDeleteDialog: Boolean = false,
    val mediaStates: Map<String, PlayerUiState> = emptyMap(),
)

data class PlayerUiState(
    val progress: Float = 0f,
    val duration: Long = 0,
    val timeRemaining: String = "--:--",
    val totalDuration: String = "--:--",
    val timeElapsed: String = "--:--",
    val isPlaying:  Boolean = false,
    val noteContent: NoteContentModel.MediaContent? = null,
)

class PlayMediaViewModel : ViewModel(), KoinComponent {
    private val noteRepository = get<NoteContentRepository>()
    private val mediaServiceListener = get<MediaServiceListener>()
    private val _playerUiState = MutableStateFlow(AudioUiState())
    val state = _playerUiState.asStateFlow()

    init {
        viewModelScope.launch {
            noteRepository.selectedNote.collectLatest { notesContents ->
                if (notesContents.isNotEmpty()) {
                    mediaServiceListener.addMediaItemList(notesContents.map { it.toMediaItem() })
                    val map = notesContents.map {
                        it.id to PlayerUiState(
                            noteContent = it, duration = it.duration, totalDuration = it.duration.getReadableDuration(), progress = 0f
                        )
                    }.toMap()
                    _playerUiState.update { it.copy(mediaStates = map) }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            mediaServiceListener.audioState.collectLatest { playerState ->
                when (playerState) {
                    PlayerState.Initial -> {}

                    is PlayerState.CurrentPlaying -> {
                        _playerUiState.update { it1 -> it1.copy(currentPlayingId = playerState.mediaId) }
                    }
                    is PlayerState.Buffering -> {
                        calculateTimeline(playerState.progress, playerState.mediaId)
                    }
                    is PlayerState.Playing -> { if(playerState.mediaId.isNullOrEmpty()) return@collectLatest
                        _playerUiState.update {
                        it.copy(
                            currentPlayingId = playerState.mediaId,
                            mediaStates = state.value.mediaStates + (playerState.mediaId to state.value.mediaStates[playerState.mediaId]!!.copy( isPlaying = playerState.isPlaying
                            ))
                        )
                    } }

                    is PlayerState.Progress -> {
                          calculateTimeline(playerState.progress, playerState.mediaId)
                    }

                    is PlayerState.Ready -> {

                    }
                }
            }
        }
    }
    private fun calculateTimeline(currentProgress: Long, mediaId : String?) {
        if (mediaId.isNullOrEmpty()) return
        val playingMedia  = getNoteContentPlayerState(mediaId)
        if(playingMedia.isNotnull()) {
            val progress =
                if (currentProgress > 0) ((currentProgress.toFloat() / playingMedia!!.duration.toFloat()) * 100f)
                else 0f

            _playerUiState.update {
                it.copy(
                    currentPlayingId = mediaId,
                    mediaStates = state.value.mediaStates + (mediaId to state.value.mediaStates[mediaId]!!.copy(progress = progress,
                      timeRemaining = playingMedia!!.duration.timerRemaining(currentProgress), timeElapsed = currentProgress.getTimerFormatedString()
                        ))
                )
            }
        }
    }

    /** media events triggered by view model */
    fun onPlayingIntent(audioPlayingIntent: AudioPlayingIntent) {
        viewModelScope.launch {
            when (audioPlayingIntent) {
                is AudioPlayingIntent.PlayOrPauseIntent -> play(audioPlayingIntent.mediaId)
                is AudioPlayingIntent.RestartIntent -> restartPlaying(audioPlayingIntent.mediaId)
                is AudioPlayingIntent.ResumeIntent -> resumePlaying(audioPlayingIntent.mediaId)
                is AudioPlayingIntent.SeekNextIntent -> seekNext(audioPlayingIntent.mediaId)
                is AudioPlayingIntent.SeekToIntent -> seekTo(audioPlayingIntent.position, audioPlayingIntent.mediaId)
                is AudioPlayingIntent.StopIntent -> stopPlaying()
                AudioPlayingIntent.Backward -> backward()
                AudioPlayingIntent.Forward -> forward()
                is AudioPlayingIntent.SelectedAudioChange -> selectionAudioId(audioPlayingIntent.mediaId)
                is AudioPlayingIntent.UpdateProgress -> updateProgress(audioPlayingIntent.newProgress, audioPlayingIntent.mediaId)
            }
        }
    }

    private suspend fun selectionAudioId(id: String) {
        val indexOf = noteRepository.getIndexOfMedia(id)
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.SelectedAudioChange(id), indexOf)
        _playerUiState.update {
            it.copy(
                currentPlayingId = id,
            )
        }
    }
    private suspend fun backward() {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.Backward)
    }

    private suspend fun forward() {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.Forward)
    }

    private suspend fun updateProgress(newProgress: Float, mediaId: String) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.UpdateProgress(newProgress, mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    private suspend fun play(mediaId: String) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.PlayOrPauseIntent(mediaId))
        _playerUiState.update { it.copy(isServiceIsRunning = true, currentPlayingId = mediaId) }
    }

    private suspend fun seekTo(position: Float, mediaId: String) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.SeekToIntent(position,mediaId),)
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    private suspend fun seekNext(mediaId: String) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.SeekNextIntent(mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    private suspend fun stopPlaying() {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.StopIntent)
    }

    private suspend fun resumePlaying(mediaId: String) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.ResumeIntent(mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    private suspend fun restartPlaying(mediaId: String) {
        mediaServiceListener.onPlayerEvents(AudioPlayingIntent.RestartIntent(mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    override fun onCleared() {
        _playerUiState.update { it.copy(isServiceIsRunning = false) }
        viewModelScope.launch {
            mediaServiceListener.onPlayerEvents(AudioPlayingIntent.StopIntent)
        }
        super.onCleared()
    }

    fun getNoteContentPlayerState(id: String): PlayerUiState? = _playerUiState.value.mediaStates[id]
}