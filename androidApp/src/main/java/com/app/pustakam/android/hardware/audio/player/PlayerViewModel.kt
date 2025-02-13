package com.app.pustakam.android.hardware.audio.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.extension.toMediaItem
import com.app.pustakam.android.services.mediaSessionService.MediaPlayingEvent
import com.app.pustakam.android.services.mediaSessionService.MediaServiceListener
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.noteContentRepo.NoteContentRepository
import com.app.pustakam.extensions.getReadableDuration
import com.app.pustakam.extensions.getTimerFormatedString
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.extensions.timerRemaining
import com.app.pustakam.util.log_d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

sealed interface AudioPlayingUIEvent {
    data class PlayOrPauseUIEvent(val mediaId: String) : AudioPlayingUIEvent
    data class ResumeUIEvent(val mediaId: String) : AudioPlayingUIEvent
    data class RestartUIEvent(val mediaId: String) : AudioPlayingUIEvent
    data class SeekToUIEvent(val position: Float, val mediaId: String) : AudioPlayingUIEvent
    data class SeekNextUIEvent(val mediaId: String) : AudioPlayingUIEvent
    data class StopUIEvent(val mediaId: String) : AudioPlayingUIEvent
    data class SeekToPrevious(val mediaId: String) : AudioPlayingUIEvent
    data class SelectedAudioChange(val mediaId: String) : AudioPlayingUIEvent
    data class UpdateProgress(val newProgress: Float, val mediaId: String) : AudioPlayingUIEvent
    data object Backward : AudioPlayingUIEvent
    data object Forward : AudioPlayingUIEvent

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
    val noteContent: NoteContentModel.MediaContent,
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
                            noteContent = it, duration = it.duration,
                            totalDuration = it.duration.getReadableDuration(),
                            progress = 0f,
                            timeRemaining = it.duration.timerRemaining(0),
                            timeElapsed = (0).toLong().getTimerFormatedString()
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
                        if(playerState.mediaId.isNullOrEmpty()) return@collectLatest
                        _playerUiState.update {
                            it.copy(
                                currentPlayingId = playerState.mediaId,
                                mediaStates = state.value.mediaStates + (playerState.mediaId to state.value.mediaStates[playerState.mediaId]!!.copy( duration = playerState.duration,
                                ))
                            )
                        }
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
                        timeRemaining = playingMedia!!.duration.timerRemaining(currentProgress),
                        timeElapsed = currentProgress.getTimerFormatedString()
                        ))
                )
            }
            log_d("log calculate : $mediaId : progress $progress currentProgress ",  currentProgress)
        }
    }


    /** media events triggered by view model */
    fun onPlayingIntent(audioPlayingIntent: AudioPlayingUIEvent) {
        viewModelScope.launch {
            when (audioPlayingIntent) {
                is AudioPlayingUIEvent.PlayOrPauseUIEvent -> play(audioPlayingIntent.mediaId)
                is AudioPlayingUIEvent.RestartUIEvent -> restartPlaying(audioPlayingIntent.mediaId)
                is AudioPlayingUIEvent.ResumeUIEvent -> resumePlaying(audioPlayingIntent.mediaId)
                is AudioPlayingUIEvent.SeekNextUIEvent -> seekNext(audioPlayingIntent.mediaId)
                is AudioPlayingUIEvent.SeekToUIEvent -> seekTo(audioPlayingIntent.position, audioPlayingIntent.mediaId)
                is AudioPlayingUIEvent.StopUIEvent ->    mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Stop)
                is AudioPlayingUIEvent.Backward ->    mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Backward)
                is AudioPlayingUIEvent.Forward ->  mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Forward)
                is AudioPlayingUIEvent.SelectedAudioChange -> selectionAudioId(audioPlayingIntent.mediaId)
                is AudioPlayingUIEvent.UpdateProgress -> {
                    mediaServiceListener.onPlayerEvents(MediaPlayingEvent.UpdateProgress(audioPlayingIntent.newProgress, audioPlayingIntent.mediaId))
                    _playerUiState.update {
                        it.copy(
                            currentPlayingId = audioPlayingIntent.mediaId,
                            mediaStates = state.value.mediaStates + (audioPlayingIntent.mediaId to state.value.mediaStates[audioPlayingIntent.mediaId]!!.copy(progress = audioPlayingIntent.newProgress,
                            ))
                        )
                    }
                }
                is AudioPlayingUIEvent.SeekToPrevious -> mediaServiceListener.onPlayerEvents(MediaPlayingEvent.SeekToPrevious)
            }
        }
    }

    private suspend fun selectionAudioId(id: String) {
        val indexOf = noteRepository.getIndexOfMedia(id)
        mediaServiceListener.onPlayerEvents(MediaPlayingEvent.SelectedAudioChange(id), indexOf)
        _playerUiState.update {
            it.copy(
                currentPlayingId = id,
            )
        }
    }
    private suspend fun play(mediaId: String) {
        mediaServiceListener.onPlayerEvents(MediaPlayingEvent.PlayOrPause(mediaId))
        _playerUiState.update { it.copy(isServiceIsRunning = true, currentPlayingId = mediaId) }
    }

    private suspend fun seekTo(position: Float, mediaId: String) {
       val duration =  getNoteContentPlayerState(mediaId)?.duration!!
        val sliderPosition = ((duration*position)/100f).toLong()
        mediaServiceListener.onPlayerEvents(MediaPlayingEvent.SeekTo(mediaId), position = sliderPosition)
        _playerUiState.update { it.copy( currentPlayingId = mediaId,) }
    }

    private suspend fun seekNext(mediaId: String) {
        mediaServiceListener.onPlayerEvents(MediaPlayingEvent.SeekNext(mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }


    private suspend fun resumePlaying(mediaId: String) {
        mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Resume(mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    private suspend fun restartPlaying(mediaId: String) {
        mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Restart(mediaId))
        _playerUiState.update { it.copy( currentPlayingId = mediaId) }
    }

    override fun onCleared() {
        _playerUiState.update { it.copy(isServiceIsRunning = false) }
        viewModelScope.launch {
            mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Stop)
        }
        super.onCleared()
    }

    fun getNoteContentPlayerState(id: String): PlayerUiState? = _playerUiState.value.mediaStates[id]
}