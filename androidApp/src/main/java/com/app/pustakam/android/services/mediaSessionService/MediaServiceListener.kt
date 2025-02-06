package com.app.pustakam.android.services.mediaSessionService

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.pustakam.android.hardware.audio.player.AudioPlayingIntent
import com.app.pustakam.android.hardware.audio.player.PlayerState
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
    fun addMediaItemList(mediaList : List<MediaItem>){
        media.clear()
        media.addAll(mediaList)
        exoPlayer.setMediaItems(mediaList)
        exoPlayer.prepare()
    }

    suspend fun onPlayerEvents(
        playerEvent : AudioPlayingIntent,
        selectedAudioIndex : Int =-1,
        position : Long = 0
    ){
        when(playerEvent){
            AudioPlayingIntent.Backward -> exoPlayer.seekBack()
            is AudioPlayingIntent.Forward -> exoPlayer.seekForward()
            is AudioPlayingIntent.PlayOrPauseIntent -> playOrPause(playerEvent.mediaId)
            is AudioPlayingIntent.RestartIntent -> {}
            is AudioPlayingIntent.ResumeIntent -> exoPlayer.play()
            is AudioPlayingIntent.SeekNextIntent -> exoPlayer.seekToNext()
            is AudioPlayingIntent.SeekToIntent -> exoPlayer.seekTo(position)
            is AudioPlayingIntent.SelectedAudioChange -> {
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
            is AudioPlayingIntent.StopIntent -> exoPlayer.stop()
            is AudioPlayingIntent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_ENDED ->

            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                PlayerState.Buffering(exoPlayer.currentPosition, getId())

            ExoPlayer.STATE_READY -> _audioState.value =
                PlayerState.Ready(exoPlayer.duration,getId())
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val id= getId()
        _audioState.value = PlayerState.Playing(isPlaying = isPlaying, mediaId = id )
        _audioState.value = PlayerState.CurrentPlaying(id)
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
//            delay(500)
            _audioState.value = PlayerState.Progress(exoPlayer.currentPosition,getId())
        }
    }
    private fun stopProgressUpdate() {
        job?.cancel()
        _audioState.value = PlayerState.Playing(isPlaying = false, mediaId = getId())
    }
private fun getId() = media[exoPlayer.currentMediaItemIndex].mediaId
}