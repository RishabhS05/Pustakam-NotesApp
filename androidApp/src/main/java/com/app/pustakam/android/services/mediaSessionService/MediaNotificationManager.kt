package com.app.pustakam.android.services.mediaSessionService

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.app.pustakam.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

private const val NOTIFICATION_ID = 101
private const val NOTIFICATION_CHANNEL_NAME = "notification channel 101"
private const val NOTIFICATION_CHANNEL_ID = "notification channel id 101"

@UnstableApi
class MediaNotificationManager(private val context: Context,
    private val exoPlayer: ExoPlayer) : KoinComponent {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @UnstableApi
    fun startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession,
    ) {
        buildNotification(mediaSession)
        startForeGroundNotificationService(mediaSessionService)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForeGroundNotificationService(mediaSessionService: MediaSessionService) {
        val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setCategory(Notification.CATEGORY_SERVICE).build()
        mediaSessionService.startForeground(NOTIFICATION_ID, notification)
    }

    @UnstableApi
    private fun buildNotification(mediaSession: MediaSession) {
      PlayerNotificationManager.Builder(
            context, NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID
        ).setMediaDescriptionAdapter(
                MediaAdapter(
                    context = context,
                    pendingIntent = mediaSession.sessionActivity
                )
            ).setSmallIconResourceId(R.drawable.ic_mic)
            .build()
            .also {
                it.setMediaSessionToken (mediaSession.platformToken)
                it.setUseNextActionInCompactView(true)
                it.setUseRewindAction(true)
                it.setUseFastForwardAction(true)
               it.setUseRewindActionInCompactView(true)
               it.setUseFastForwardActionInCompactView(true)
               it.setUseRewindActionInCompactView(true)
               it.setUseFastForwardActionInCompactView(true)
                it.setPriority(NotificationCompat.PRIORITY_DEFAULT)
               it.setPlayer(exoPlayer)
            }
    }


    /**
     * Hides the notification.
     */
//    fun hideNotification() {
//        notificationMediaManager.setPlayer(null)
//    }
//
//    /**
//     * Shows the notification for the given player.
//     * @param player The player instance for which the notification is shown.
//     */
//    fun showNotificationForPlayer(player: Player) {
//        notificationMediaManager.setPlayer(player)
//    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }
    @UnstableApi
    private inner class MediaAdapter(
        private val context: Context,
        private val pendingIntent: PendingIntent?,
    ) : PlayerNotificationManager.MediaDescriptionAdapter{
        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null
        override fun getCurrentContentTitle(player: Player): CharSequence =
            player.mediaMetadata.albumTitle ?: "Unknown"

        override fun createCurrentContentIntent(player: Player): PendingIntent? = pendingIntent

        override fun getCurrentContentText(player: Player): CharSequence =
            player.mediaMetadata.displayTitle ?: "Unknown"

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? {
            val iconUri = player.mediaMetadata.artworkUri
            return if (currentIconUri != iconUri || currentBitmap == null) {
                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }
        private suspend fun resolveBitmap(uri: Uri): Bitmap?{
            return  withContext(Dispatchers.IO) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .placeholder(R.mipmap.ic_launcher)
                    .data(uri)
                    .allowHardware(false) // Disable hardware bitmaps.
                    .build()

                val result = (loader.execute(request) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                return@withContext bitmap
            }
        }
    }
}