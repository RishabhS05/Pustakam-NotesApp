package com.app.pustakam.android.services.locationService

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.app.pustakam.android.R
import com.app.pustakam.android.hardware.location.LocationClient
import com.app.pustakam.core.common.util.log_d
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent

/**
 * A foreground service that continuously receives location updates in the background.
 * Required for background location tracking as per Android's restrictions (especially on Android 8.0+).
 */
class LocationService : Service(), KoinComponent {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    // Used to access the fused location provider which gives access to location APIs.
    /**
     * Called once when the service is first created.
     * Initializes the location client and starts requesting location updates.
     */
    override fun onCreate() {
        super.onCreate()
        // Initialize fused location client for location updates
        locationClient = DefaultLocationClient(
            applicationContext, // application context
            client = LocationServices.getFusedLocationProviderClient(applicationContext) // fused location provider client
        )
        startUpdatingLocation()
    }

    /**
     * Starts location updates using high-accuracy mode.
     * This is where the service starts collecting location updates from the device.
     */
    fun startUpdatingLocation() {

    }

    /**
     * Called when the service is started.
     * Starts it as a foreground service by showing a persistent notification.
     * Foreground service is required to run background location updates on Android 8.0+.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> createAndStartNotification()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    /**
     * Creates and returns the notification required to keep the service running in the foreground.
     * Android requires all foreground services to display a notification.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAndStartNotification() {
        val channelId = "location"
        val channelName = "location"

        // Create a notification channel for Android 8.0+ (required)
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT // Low importance so it doesn't show sound/vibration
        )
        val notificationManager =
            getSystemService(NotificationManager::class.java) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pustakm Location Service Running") // Title in notification bar
            .setPriority(NotificationCompat.PRIORITY_LOW) // Set low priority to avoid interrupting user
            .setSmallIcon(R.drawable.ic_stop) // Small icon (required)
        locationClient.getLocationUpdates(10000L).catch { e ->
            e.printStackTrace()
        }.onEach { location ->
            location.latitude.toString().takeLast(3)
            location.longitude.toString().takeLast(3)

            log_d("Location ", location)
            val updatedNotification =
                notificationBuilder.setContentText("Pustakm got your App location in background\n This App can access your location")
            notificationManager.notify(1, updatedNotification.build())
        }.launchIn(serviceScope)
        // Build the actual notification
        notificationBuilder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}