package com.app.pustakam.android.services.mediaPlayerServices

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.os.bundleOf

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.app.pustakam.android.notification.AppNotificationManager
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.permission.hasPermission
import org.koin.core.component.KoinComponent
import org.koin.core.component.get


open class BaseMediaSessionService : MediaLibraryService(), KoinComponent {
    private val notificationManager  = get<AppNotificationManager>()

    private lateinit var mediaLibrarySession: MediaLibrarySession
    private val player = get<ExoPlayer>()

    open fun getSingleTopActivity(): PendingIntent? = null
    open fun getBackStackedActivity(): PendingIntent? = null


    protected open fun createLibrarySessionCallback(): MediaLibrarySession.Callback {
        return MediaLibrarySessionCallback(this)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
       return mediaLibrarySession
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        initializeSessionAndPlayer()
        setListener(MediaSessionServiceListener())
    }

    // Remember to release the player and media session in onDestroy
    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        getBackStackedActivity()?.let { mediaLibrarySession.setSessionActivity(it) }
        mediaLibrarySession.release()
        mediaLibrarySession.player.release()
        clearListener()
        super.onDestroy()
    }
    private fun initializeSessionAndPlayer() {
        player.addAnalyticsListener(EventLogger())
        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, createLibrarySessionCallback())
                .also { builder -> getSingleTopActivity()?.let { builder.setSessionActivity(it) } }
                .build()
                .also { mediaLibrarySession ->
                    // The media session always supports skip, except at the start and end of the playlist.
                    // Reserve the space for the skip action in these cases to avoid custom actions jumping
                    // around when the user skips.
                    mediaLibrarySession.sessionExtras = bundleOf(
                        MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV to true,
                        MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT to true,
                    )
                }
    }
    // MediaSessionService.Listener
    @UnstableApi
    private inner class MediaSessionServiceListener : Listener {

        /**
         * This method is only required to be implemented on Android 12 or above when an attempt is made
         * by a media controller to resume playback when the {@link MediaSessionService} is in the
         * background.
         */
        override fun onForegroundServiceStartNotAllowedException() {
            if (
                Build.VERSION.SDK_INT >= 33 &&
               hasPermission(applicationContext,NeededPermission.POST_NOTIFICATIONS.permission)
            ) {

                return
            }
            notificationManager.initNotification(getBackStackedActivity())
        }
    }
}