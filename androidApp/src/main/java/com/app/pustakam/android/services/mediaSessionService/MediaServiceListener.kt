package com.app.pustakam.android.services.mediaSessionService

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.pustakam.android.hardware.audio.player.AudioPlayingIntent
import com.app.pustakam.android.hardware.audio.player.PlayerUiState
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
    private val _audioState: MutableStateFlow<PlayerUiState> =
        MutableStateFlow(PlayerUiState.Initial)
    val audioState: StateFlow<PlayerUiState> = _audioState.asStateFlow()
    private var job: Job? = null
    init {
        exoPlayer.addListener(this)
    }
    fun addMediaItem(media : MediaItem){
        exoPlayer.setMediaItem(media)
        exoPlayer.prepare()
    }
    fun addMediaItemList(mediaList : List<MediaItem>){
        exoPlayer.setMediaItems(mediaList)
        exoPlayer.prepare()
    }

    suspend fun onPlayerEvents(
        playerEvent : AudioPlayingIntent,
        selectedAudioIndex : Int =-1,
        position : Long = 0
    ){
        when(playerEvent){
            AudioPlayingIntent.Backward ->exoPlayer.seekBack()
            AudioPlayingIntent.DeleteRecordingIntent -> {}
            AudioPlayingIntent.Forward -> exoPlayer.seekForward()
            AudioPlayingIntent.PlayOrPauseIntent -> playOrPause()
            AudioPlayingIntent.RestartIntent -> {}
            AudioPlayingIntent.ResumeIntent -> exoPlayer.play()
            AudioPlayingIntent.SeekNextIntent -> exoPlayer.seekToNext()
            is AudioPlayingIntent.SeekToIntent -> exoPlayer.seekTo(position)
            is AudioPlayingIntent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    exoPlayer.currentMediaItemIndex -> {
                        playOrPause()
                    }

                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedAudioIndex)
                        _audioState.value = PlayerUiState.Playing(
                            isPlaying = true
                        )
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }
            AudioPlayingIntent.StopIntent -> exoPlayer.stop()
            is AudioPlayingIntent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                PlayerUiState.Buffering(exoPlayer.currentPosition)

            ExoPlayer.STATE_READY -> _audioState.value =
                PlayerUiState.Ready(exoPlayer.duration)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _audioState.value = PlayerUiState.Playing(isPlaying = isPlaying)
        _audioState.value = PlayerUiState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
        if (isPlaying) {
            GlobalScope.launch(Dispatchers.Main) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }


    private suspend fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopProgressUpdate()
        } else {
            exoPlayer.play()
            _audioState.value = PlayerUiState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }
    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(500)
            _audioState.value = PlayerUiState.Progress(exoPlayer.currentPosition)
        }
    }
    private fun stopProgressUpdate() {
        job?.cancel()
        _audioState.value = PlayerUiState.Playing(isPlaying = false)
    }

}