package com.app.pustakam.android.services.mediaSessionService

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.pustakam.android.hardware.audio.player.PlayerState
import com.app.pustakam.util.log_d
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

sealed interface MediaPlayingEvent {
    data class PlayOrPause(val mediaId: String) : MediaPlayingEvent
    data class Resume(val mediaId: String) : MediaPlayingEvent
    data class Restart(val mediaId: String) : MediaPlayingEvent
    data class SeekTo(val mediaId: String) : MediaPlayingEvent
    data class SeekNext(val mediaId: String) : MediaPlayingEvent
    data object Stop : MediaPlayingEvent
    data class SelectedAudioChange(val mediaId: String) : MediaPlayingEvent
    data class UpdateProgress(val newProgress: Float, val mediaId: String) : MediaPlayingEvent
    data object Backward : MediaPlayingEvent
    data object Forward : MediaPlayingEvent
    data object SeekToPrevious : MediaPlayingEvent
}
class MediaServiceListener(
    private  val exoPlayer: ExoPlayer
) : Player.Listener, KoinComponent {
    private val _audioState: MutableStateFlow<PlayerState> =
        MutableStateFlow(PlayerState.Initial)
    val audioState: StateFlow<PlayerState> = _audioState.asStateFlow()
    private val media : MutableList<MediaItem>   = emptyList<MediaItem>().toMutableList()
    private var job: Job? = null
    init {
        exoPlayer.addListener(this)
    }
    fun addMediaItem(media : MediaItem){
        exoPlayer.setMediaItem(media)
        exoPlayer.prepare()
    }
    fun getMedia(id: String) = media.find { id == it.mediaId }
    fun addMediaItemList(mediaList : List<MediaItem>){
        media.clear()
        media.addAll(mediaList)
        exoPlayer.setMediaItems(mediaList)
        exoPlayer.prepare()
    }

    suspend fun onPlayerEvents(
        playerEvent : MediaPlayingEvent,
        selectedAudioIndex : Int =-1,
        position : Long = 0
    ){
        when(playerEvent){
            is MediaPlayingEvent.Backward -> exoPlayer.seekBack()
            is MediaPlayingEvent.Forward -> exoPlayer.seekForward()
            is MediaPlayingEvent.PlayOrPause -> playOrPause(playerEvent.mediaId)
            is MediaPlayingEvent.Restart -> {}
            is MediaPlayingEvent.Resume -> exoPlayer.play()
            is MediaPlayingEvent.SeekNext -> exoPlayer.seekToNext()
            is MediaPlayingEvent.SeekTo -> exoPlayer.seekTo(position)
            is MediaPlayingEvent.SeekToPrevious -> exoPlayer.seekToPrevious()
            is MediaPlayingEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    exoPlayer.currentMediaItemIndex -> {
                        playOrPause(playerEvent.mediaId)
                    }

                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedAudioIndex)
                        _audioState.value = PlayerState.Playing(
                            isPlaying = true, playerEvent.mediaId
                        )
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }
            is MediaPlayingEvent.Stop -> stopProgressUpdate()
            is MediaPlayingEvent.UpdateProgress -> {
               val seekto=  (exoPlayer.duration * playerEvent.newProgress).toLong()
                exoPlayer.seekTo(seekto)
                log_d("log MediaPlayingIntent: ${playerEvent.mediaId} :",seekto )
            }


        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val mediaId = getId()
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> {
                log_d("log MediaServiceListener ",exoPlayer.currentPosition )
                _audioState.value =
                    PlayerState.Buffering(exoPlayer.currentPosition, mediaId)
            }
            ExoPlayer.STATE_READY ->
                _audioState.value = PlayerState.Ready(exoPlayer.duration,mediaId)
            ExoPlayer.STATE_ENDED -> {
                // no-op
            }

            ExoPlayer.STATE_IDLE -> {
                // no-op
            }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val mediaId= getId()
        _audioState.value = PlayerState.Playing(isPlaying = isPlaying, mediaId = mediaId )
        _audioState.value = PlayerState.CurrentPlaying(mediaId)
        if (isPlaying) {
            GlobalScope.launch(Dispatchers.Main) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }


    private suspend fun playOrPause(mediaId: String) {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        } else {
            exoPlayer.play()
            _audioState.value = PlayerState.Playing(
                isPlaying = true,mediaId
            )
            startProgressUpdate()
        }
    }
    private suspend fun startProgressUpdate() = job.run {
        while (true) {
         delay(1000)
            _audioState.value = PlayerState.Progress(exoPlayer.currentPosition,getId())
        }
    }
    private fun stopProgressUpdate() {
        job?.cancel()
        _audioState.value = PlayerState.Playing(isPlaying = false, mediaId = getId())
    }

    //always call getId method to update the data otherwise ui will cause issues
private fun getId() = media[exoPlayer.currentMediaItemIndex].mediaId
    fun getExoPlayer(): ExoPlayer = exoPlayer
}