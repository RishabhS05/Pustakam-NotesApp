package com.app.pustakam.android.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import com.app.pustakam.android.R
import org.koin.core.component.KoinComponent

class AppNotificationManager(private val applicationContext : Context) : KoinComponent{
    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "app_session_notification_channel_id"
    }
    private val notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
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
                getString(applicationContext,R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        notificationManagerCompat.createNotificationChannel(channel)
    }
    @SuppressLint("MissingPermission")
    fun initNotification(backStack : PendingIntent?){
        ensureNotificationChannel(notificationManagerCompat)
        val builder =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(applicationContext,R.string.notification_content_title))
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(getString(applicationContext,R.string.notification_content_text))
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .also { builder -> backStack?.let { builder.setContentIntent(it) } }
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }
}