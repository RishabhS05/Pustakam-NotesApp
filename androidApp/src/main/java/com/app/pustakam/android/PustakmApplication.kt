package com.app.pustakam.android

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.app.pustakam.android.di.getAndroidSpecifics
import com.app.pustakam.android.sync.SyncWorker
import com.app.pustakam.feature.notes.domain.usecase.NotifyConnectivityUseCase
import com.app.pustakam.feature.notes.domain.usecase.StartSyncUseCase
import com.app.pustakam.koin.initKoin
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import org.koin.core.logger.Level

class PustakmApplication : Application(), KoinComponent {

    // 🔄 20-Aug-2026 sync: lazy, so nothing is resolved before initKoin() has run
    private val startSync: StartSyncUseCase by inject()
    private val notifyConnectivity: NotifyConnectivityUseCase by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            FirebaseApp.initializeApp(this@PustakmApplication)
            androidLogger(level = Level.INFO)
            androidContext(this@PustakmApplication)
            modules(getAndroidSpecifics())
        }
        // 🔄 in-app loop (sign-in, debounced saves, timer) + the network-constrained background worker
        startSync()
        watchConnectivity()
        SyncWorker.schedulePeriodic(this)
    }

    /** 🔄 28-Aug-2026 — Android's NWPathMonitor. iOS has had this since day one; Android had
     *  nothing in-process, so "I turned airplane mode off" was only noticed by the 15-minute
     *  WorkManager run. Now the engine hears it immediately and flushes what was queued offline. */
    private fun watchConnectivity() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            manager.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = notifyConnectivity(true)
                    override fun onLost(network: Network) = notifyConnectivity(false)
                }
            )
        }
    }
}
