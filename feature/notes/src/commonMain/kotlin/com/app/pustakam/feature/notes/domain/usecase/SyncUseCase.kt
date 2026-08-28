package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.data.usecases.BaseUseCase
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.sync.SyncRunState
import com.app.pustakam.core.model.models.sync.SyncSummary
import com.app.pustakam.feature.notes.domain.repository.ISyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

// 🔄 20-Aug-2026 sync: server sync. NoteSyncBaseUseCase (NoteSyncUseCase.kt) is the in-app event bus.
abstract class SyncBaseUseCase : BaseUseCase() {
    protected val remoteSync: ISyncRepository by inject()
}

/** 🔄 called once at app start on both platforms; idempotent. */
class StartSyncUseCase : SyncBaseUseCase() {
    operator fun invoke() = remoteSync.start()
}

/** 🔄 pull-to-refresh and "sync now". Waits for the cycle and reports what moved. */
class SyncNowUseCase : SyncBaseUseCase() {
    suspend operator fun invoke(): Flow<Result<BaseResponse<SyncSummary>, Error>> =
        getBaseApiCall { remoteSync.syncNow() }
}

/** 🔄 fire-and-forget nudge — after a save, or when a background worker wakes up. */
class RequestSyncUseCase : SyncBaseUseCase() {
    operator fun invoke() = remoteSync.requestSync()
}

/** 🔄 the platform connectivity seam: Android WorkManager / iOS NWPathMonitor call this. */
class NotifyConnectivityUseCase : SyncBaseUseCase() {
    operator fun invoke(isOnline: Boolean) = remoteSync.onConnectivityChanged(isOnline)
}

class ObserveSyncStateUseCase : SyncBaseUseCase() {
    operator fun invoke(): StateFlow<SyncRunState> = remoteSync.syncState
}
