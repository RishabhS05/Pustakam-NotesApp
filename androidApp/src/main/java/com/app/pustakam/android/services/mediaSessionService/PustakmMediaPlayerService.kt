package com.app.pustakam.android.services.mediaSessionService

import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

@UnstableApi
class PustakmMediaPlayerService : MediaSessionService(), KoinComponent {
    val exoPlayer  = get<ExoPlayer>()
    private val session by lazy {
        MediaSession.Builder(this@PustakmMediaPlayerService ,exoPlayer ).build()
    }
    private  val notificationManager : MediaNotificationManager by inject<MediaNotificationManager>()
init {

}
    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.startNotificationService(
                mediaSession = session,
                mediaSessionService = this
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession = session
    // 🔧 14-Jul-2026: FIX (nothing plays after closing & reopening the app) — onDestroy used to
    //   RELEASE the ExoPlayer. The player is a Koin SINGLETON owned by the app graph, not by this
    //   service; when the task was swiped away the service died but the process stayed cached, so
    //   the next launch reused a released player and every play() was silently ignored. The service
    //   is only a lifecycle guest: release the session, stop playback, but NEVER release the player
    //   (MediaServiceListener re-prepares it from IDLE on the next play — see ensurePrepared()).
    override fun onDestroy() {
        super.onDestroy()
        session.apply {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
        }
    }
}