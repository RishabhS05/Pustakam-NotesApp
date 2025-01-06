package com.app.pustakam.android.services.mediaPlayerServices

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.app.pustakam.android.R


open class BaseMediaSessionService : MediaLibraryService() {
    private var mediaSession: MediaSession? = null
    private lateinit var mediaLibrarySession: MediaLibraryService.MediaLibrarySession
    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "demo_session_notification_channel_id"
    }

    open fun getSingleTopActivity(): PendingIntent? = null
    open fun getBackStackedActivity(): PendingIntent? = null


    protected open fun createLibrarySessionCallback(): MediaLibrarySession.Callback {
        return MediaLibrarySessionCallback(this)
    }
    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaLibrarySession? {
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
        val player =
            ExoPlayer.Builder(this)
                .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
                .build()
        player.addAnalyticsListener(EventLogger())

        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, createLibrarySessionCallback())
                .also { builder -> getSingleTopActivity()?.let { builder.setSessionActivity(it) } }
                .build()
                .also { mediaLibrarySession ->
                    // The media session always supports skip, except at the start and end of the playlist.
                    // Reserve the space for the skip action in these cases to avoid custom actions jumping
                    // around when the user skips.
                    mediaLibrarySession.setSessionExtras(
                        bundleOf(
                            MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV to true,
                            MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT to true,
                        )
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
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Notification permission is required but not granted
                return
            }
            val notificationManagerCompat = NotificationManagerCompat.from(this@BaseMediaSessionService)
            ensureNotificationChannel(notificationManagerCompat)
//            val builder =
//                NotificationCompat.Builder(this@BaseMediaSessionService, CHANNEL_ID)
//                    .setSmallIcon(R.drawable.media3_notification_small_icon)
//                    .setContentTitle(getString(R.string.notification_content_title))
//                    .setStyle(
//                        NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_content_text))
//                    )
//                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                    .setAutoCancel(true)
//                    .also { builder -> getBackStackedActivity()?.let { builder.setContentIntent(it) } }
//            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (
            Build.VERSION.SDK_INT < 26 ||
            notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        notificationManagerCompat.createNotificationChannel(channel)
    }
}