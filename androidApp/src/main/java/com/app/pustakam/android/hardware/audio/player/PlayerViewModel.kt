package com.app.pustakam.android.hardware.audio.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.extension.toMediaItem
import com.app.pustakam.android.services.mediaSessionService.MediaPlayingEvent
import com.app.pustakam.android.services.mediaSessionService.MediaServiceListener
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import com.app.pustakam.extensions.getReadableHMS
import com.app.pustakam.extensions.readableTimer
import com.app.pustakam.extensions.timerRemaining
import com.app.pustakam.util.log_d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

sealed interface MediaPlayingUIEvent {
    data class PlayOrPauseUIEvent(val mediaId: String) : MediaPlayingUIEvent
    data class ResumeUIEvent(val mediaId: String) : MediaPlayingUIEvent
    data class RestartUIEvent(val mediaId: String) : MediaPlayingUIEvent
    data class SeekToUIEvent(val position: Float, val mediaId: String) : MediaPlayingUIEvent
    data class SeekNextUIEvent(val mediaId: String) : MediaPlayingUIEvent
    data class StopUIEvent(val mediaId: String) : MediaPlayingUIEvent
    data class SeekToPrevious(val mediaId: String) : MediaPlayingUIEvent
    data class SelectedMediaChange(val mediaId: String) : MediaPlayingUIEvent
    data class UpdateProgress(val newProgress: Float, val mediaId: String) : MediaPlayingUIEvent
    data class  Backward(val mediaId: String) : MediaPlayingUIEvent
    data class  Forward(val mediaId: String) : MediaPlayingUIEvent

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
        // 🔧 14-Jul-2026: FIX (screen-switch persistence) — this VM is re-created per screen while the
        //   player is a standalone singleton. Seed selection/playing state from the player so the UI
        //   comes back "as is" (previously it started blank until the next 1s progress tick).
        _playerUiState.update {
            it.copy(
                currentPlayingId = mediaServiceListener.getCurrentMediaId(),
                isPlaying = mediaServiceListener.isPlaying()
            )
        }
        viewModelScope.launch {
            noteRepository.selectedNoteMediaContent.collectLatest { notesContents ->
                if (notesContents.isNotEmpty()) {
                    mediaServiceListener.addMediaItemList(notesContents.map { it.toMediaItem() })
                    // 🔧 14-Jul-2026: FIX — keep the live state (progress/duration/isPlaying) of ids we
                    //   already track; a repo re-emission used to reset every card back to 0.
                    // 🔧 14-Jul-2026: FIX (time calculation) — content.duration is MILLISECONDS
                    //   (recordings now store ms). getReadableHMS/getTimerFormatedString expect
                    //   SECONDS, readableTimer expects ms — feed each the unit it wants. The mixed
                    //   units here were why times were wrong before the first Ready event.
                    val map = notesContents.associate { content ->
                        val existing = _playerUiState.value.mediaStates[content.id]
                        content.id to (existing?.copy(noteContent = content) ?: PlayerUiState(
                            noteContent = content, duration = content.duration,
                            totalDuration = (content.duration / 1000).getReadableHMS(),
                            progress = 0f,
                            timeRemaining = content.duration.readableTimer(),
                            timeElapsed = (0).toLong().readableTimer()
                        ))
                    }
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
                        // 🔧 14-Jul-2026: CRASH FIX — was mediaStates[id]!! → NPE when the player
                        //   emitted a state for a media id not yet in the map (e.g. a just-recorded
                        //   video). Now we skip the update instead of crashing.
                        val existing = state.value.mediaStates[playerState.mediaId] ?: return@collectLatest
                        _playerUiState.update {
                        it.copy(
                            currentPlayingId = playerState.mediaId,
                            isPlaying = playerState.isPlaying,
                            // 🔧 14-Jul-2026: FIX — only ONE card may show the playing state; when the
                            //   player switches tracks the previous card's pause icon used to stick.
                            mediaStates = state.value.mediaStates.mapValues { (id, mediaState) ->
                                if (id == playerState.mediaId) existing.copy(isPlaying = playerState.isPlaying)
                                else mediaState.copy(isPlaying = false)
                            }
                        )
                    } }
                    is PlayerState.Progress -> {
                          calculateTimeline(playerState.progress, playerState.mediaId)
                    }
                    is PlayerState.Ready -> {
                        if(playerState.mediaId.isNullOrEmpty()) return@collectLatest
                        // 🔧 14-Jul-2026: FIX (time calculation) — ExoPlayer reports C.TIME_UNSET
                        //   (negative) while the duration is unknown; writing that into the state
                        //   produced garbage labels. Skip until a real duration arrives.
                        if (playerState.duration <= 0) return@collectLatest
                        // 🔧 14-Jul-2026: CRASH FIX — null-safe map access (was mediaStates[id]!!).
                        val existing = state.value.mediaStates[playerState.mediaId] ?: return@collectLatest
                        _playerUiState.update {
                            it.copy(
                                currentPlayingId = playerState.mediaId,
                                // 🔧 14-Jul-2026: FIX — recorded media carries duration=0 metadata; when
                                //   the real duration arrives from the player, refresh the timer labels
                                //   too (they used to stay "--:--" for fresh recordings). Player duration
                                //   is MILLISECONDS: /1000 for the seconds-based HMS label, raw ms for
                                //   the ms-based readableTimer.
                                mediaStates = state.value.mediaStates + (playerState.mediaId to existing.copy(
                                    duration = playerState.duration,
                                    totalDuration = (playerState.duration / 1000).getReadableHMS(),
                                    timeRemaining = playerState.duration.readableTimer(),
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
        // 🔧 14-Jul-2026: CRASH FIX — single null-safe lookup (was mediaStates[id]!!). If the id
        //   isn't in the map yet, skip the timeline update instead of crashing.
        val playingMedia = getNoteContentPlayerState(mediaId) ?: return
        // 🔧 14-Jul-2026: FIX — guard duration<=0 (fresh recordings) so progress can't become
        //   Infinity/NaN and break the slider.
        // 🔧 14-Jul-2026: FIX (time calculation) — clamp to 0..100 so a stale metadata duration
        //   (legacy notes recorded in seconds) can never push the slider past the end.
        val progress =
            (if (currentProgress > 0 && playingMedia.duration > 0) ((currentProgress.toFloat() / playingMedia.duration.toFloat()) * 100f)
            else 0f).coerceIn(0f, 100f)
        _playerUiState.update {
            it.copy(
                currentPlayingId = mediaId,
                mediaStates = state.value.mediaStates + (mediaId to playingMedia.copy(progress = progress,
                    timeRemaining = playingMedia.duration.timerRemaining(currentProgress),
                    timeElapsed = currentProgress.readableTimer()
                    ))
            )
        }
        log_d("log calculate : $mediaId : duration ${playingMedia.duration} progress $progress currentProgress ",  currentProgress)
    }


    /** media events triggered by view model */
    fun onPlayingIntent(audioPlayingIntent: MediaPlayingUIEvent) {
        viewModelScope.launch {
            when (audioPlayingIntent) {
                is MediaPlayingUIEvent.PlayOrPauseUIEvent -> play(audioPlayingIntent.mediaId)
                is MediaPlayingUIEvent.RestartUIEvent -> restartPlaying(audioPlayingIntent.mediaId)
                is MediaPlayingUIEvent.ResumeUIEvent -> resumePlaying(audioPlayingIntent.mediaId)
                is MediaPlayingUIEvent.SeekNextUIEvent -> seekNext(audioPlayingIntent.mediaId)
                is MediaPlayingUIEvent.SeekToUIEvent -> seekTo(audioPlayingIntent.position, audioPlayingIntent.mediaId)
                is MediaPlayingUIEvent.StopUIEvent ->    mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Stop)
                is MediaPlayingUIEvent.Backward ->    mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Backward)
                is MediaPlayingUIEvent.Forward ->  mediaServiceListener.onPlayerEvents(MediaPlayingEvent.Forward)
                is MediaPlayingUIEvent.SelectedMediaChange -> selectionAudioId(audioPlayingIntent.mediaId)
                is MediaPlayingUIEvent.UpdateProgress -> {
                    mediaServiceListener.onPlayerEvents(MediaPlayingEvent.UpdateProgress(audioPlayingIntent.newProgress, audioPlayingIntent.mediaId))
                    // 🔧 14-Jul-2026: CRASH FIX — null-safe map access (was mediaStates[id]!!).
                    val existing = state.value.mediaStates[audioPlayingIntent.mediaId]
                    if (existing != null) {
                        _playerUiState.update {
                            it.copy(
                                currentPlayingId = audioPlayingIntent.mediaId,
                                mediaStates = state.value.mediaStates + (audioPlayingIntent.mediaId to existing.copy(progress = audioPlayingIntent.newProgress,
                                ))
                            )
                        }
                    }
                }
                is MediaPlayingUIEvent.SeekToPrevious -> mediaServiceListener.onPlayerEvents(MediaPlayingEvent.SeekToPrevious)
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
        // 🔧 14-Jul-2026: CRASH FIX — was `?.duration!!` which still NPEs when the state is null.
       val duration =  getNoteContentPlayerState(mediaId)?.duration ?: return
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
    fun getExoPlayer() = mediaServiceListener.getExoPlayer()
    fun getMediaId(id : String)= mediaServiceListener.getMedia(id)
}