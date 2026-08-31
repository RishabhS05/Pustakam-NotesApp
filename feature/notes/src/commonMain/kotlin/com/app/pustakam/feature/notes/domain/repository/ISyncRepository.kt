package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.sync.SyncRunState
import com.app.pustakam.core.model.models.sync.SyncSummary
import kotlinx.coroutines.flow.StateFlow

// 🔄 20-Aug-2026 sync: server sync. NOT to be confused with INoteSyncRepository, which is the
//   in-app content event bus between the editor and the canvas.
interface ISyncRepository {

    val syncState: StateFlow<SyncRunState>

    /** Starts the in-app loop: sync on sign-in, and on a timer while the app is open. Idempotent. */
    fun start()

    /** Platform connectivity seam — Android WorkManager / iOS NWPathMonitor push into this. */
    fun onConnectivityChanged(isOnline: Boolean)

    /** 🔄 29-Aug-2026 — app on screen or not. On screen the engine polls every
     *  [com.app.pustakam.core.model.models.sync.SyncConfig.FOREGROUND_INTERVAL_MILLIS], which is
     *  how another device's edit reaches this one without the user pulling to refresh. */
    fun setForeground(isForeground: Boolean)

    /** Fire-and-forget; safe to call from anywhere, coalesces with a run already in flight. */
    fun requestSync()

    /** 🔄 28-Aug-2026 — "the user just saved". Debounced, so a burst of keystrokes in the editor
     *  becomes ONE push instead of one per character. This is the sync-on-save entry point. */
    fun requestSyncSoon()

    /** Runs one full push-then-pull cycle and waits for it. */
    suspend fun syncNow(): Result<BaseResponse<SyncSummary>, Error>
}
