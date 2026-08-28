package com.app.pustakam.feature.notes.domain.bridge

import com.app.pustakam.core.common.bridge.BridgeError
import com.app.pustakam.core.common.bridge.Closeable
import com.app.pustakam.core.data.bridge.subscribeTo
import com.app.pustakam.core.data.bridge.watch
import com.app.pustakam.core.model.models.sync.SyncRunState
import com.app.pustakam.core.model.models.sync.SyncSummary
import com.app.pustakam.feature.notes.domain.usecase.NotifyConnectivityUseCase
import com.app.pustakam.feature.notes.domain.usecase.ObserveSyncStateUseCase
import com.app.pustakam.feature.notes.domain.usecase.RequestSyncUseCase
import com.app.pustakam.feature.notes.domain.usecase.StartSyncUseCase
import com.app.pustakam.feature.notes.domain.usecase.SyncNowUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 🔄 20-Aug-2026 — the ONLY sync entry point for iOS. Swift owns connectivity (NWPathMonitor) and
 * background scheduling (BGTaskScheduler) and pushes into this; the cycle itself is shared Kotlin.
 *
 * Same shape as NotesBridge: own Main-dispatcher scope, every function returns a Closeable,
 * dispose() cancels the lot.
 */
class SyncBridge : KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val startSyncUseCase: StartSyncUseCase by inject()
    private val syncNowUseCase: SyncNowUseCase by inject()
    private val requestSyncUseCase: RequestSyncUseCase by inject()
    private val notifyConnectivityUseCase: NotifyConnectivityUseCase by inject()
    private val observeSyncStateUseCase: ObserveSyncStateUseCase by inject()

    /** Call once, from iOSApp on launch. Idempotent. */
    fun start() = startSyncUseCase()

    /** Fire-and-forget nudge — scene became active, or a BGTask woke us. */
    fun nudge() = requestSyncUseCase()

    /** NWPathMonitor result. Passing true after a drop is what flushes offline work. */
    fun setOnline(isOnline: Boolean) = notifyConnectivityUseCase(isOnline)

    /** Pull-to-refresh: waits for the cycle and reports what moved. */
    fun syncNow(
        onLoading: () -> Unit,
        onSuccess: (SyncSummary?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { syncNowUseCase() }, onLoading, onSuccess, onError)

    fun observeSyncState(onEach: (SyncRunState) -> Unit): Closeable =
        observeSyncStateUseCase().watch(scope, onEach)

    fun dispose() {
        scope.cancel()
    }
}
