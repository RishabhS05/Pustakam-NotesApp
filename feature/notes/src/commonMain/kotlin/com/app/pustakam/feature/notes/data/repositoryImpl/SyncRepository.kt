package com.app.pustakam.feature.notes.data.repositoryImpl

import com.app.pustakam.core.common.coroutines.provideDispatcher
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.NetworkError
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.common.util.displayMessage
import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.common.util.log_d
import com.app.pustakam.core.data.base.BaseRepository
import com.app.pustakam.core.database.localdb.database.SYNC_STATUS_SYNCED
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.sync.SyncConfig
import com.app.pustakam.core.model.models.sync.SyncPushRequest
import com.app.pustakam.core.model.models.sync.SyncRunState
import com.app.pustakam.core.model.models.sync.SyncSummary
import com.app.pustakam.feature.notes.data.sync.MediaSyncer
import com.app.pustakam.feature.notes.data.sync.NoteWireMapper
import com.app.pustakam.feature.notes.domain.repository.ILocalNotesRepository
import com.app.pustakam.feature.notes.domain.repository.ISyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.koin.core.component.inject

private const val TAG = "SyncRepository"

internal class SyncRepository : BaseRepository(), ISyncRepository {

    private val localNotes: ILocalNotesRepository by inject()

    private val mediaSyncer: MediaSyncer by inject()

    private val scope = CoroutineScope(SupervisorJob() + provideDispatcher().io)

    // 🔄 single-flight: overlapping triggers coalesce instead of pushing the same note twice
    private val runLock = Mutex()

    private val _syncState = MutableStateFlow<SyncRunState>(SyncRunState.Idle)
    override val syncState: StateFlow<SyncRunState> = _syncState.asStateFlow()

    private var started = false
    private var online = true
    private var failures = 0
    private var mediaUploaded = 0

    // 🔄 28-Aug-2026 — a pull stops at the first note it may not overwrite, so an unpushed local
    //   edit is never lost. These bound that wait: a note push can NEVER clean (a rejected one)
    //   would otherwise block every incoming change on this device forever.
    private var stalledNoteId: String? = null
    private var stalledCycles = 0
    private var sawConnectivity = false
    private var saveNudge: Job? = null

    override fun start() {
        if (started) return
        started = true

        // 🔄 sign-in is a trigger. distinctUntilChanged matters: a theme change also emits here.
        scope.launch {
            session.state
                .map { it.isAuthenticated && it.userId.isNotBlank() }
                .distinctUntilChanged()
                .collect { signedIn -> if (signedIn) requestSync() }
        }

        // 🔄 28-Aug-2026 — the save trigger used to be "the summaries flow moved", which only
        //   fires when a save happens to go through insertOrUpdateNote(). Every other write path
        //   saved silently and never synced. NoteRepository now calls requestSyncSoon() outright.

        scope.launch {
            while (isActive) {
                delay(nextDelayMillis())
                requestSync()
            }
        }
    }

    override fun onConnectivityChanged(isOnline: Boolean) {
        // 🔄 28-Aug-2026 — `online` starts true, so the FIRST callback after a cold start used to
        //   look like "nothing changed" and skipped the sync. A first report is a trigger too.
        val cameBack = isOnline && (!online || !sawConnectivity)
        sawConnectivity = true
        online = isOnline
        if (!isOnline) {
            if (!_syncState.value.isBusy()) _syncState.value = SyncRunState.Offline
            return
        }
        // 🔄 THE trigger the whole feature exists for: work queued offline leaves as soon as there is a line
        if (cameBack) requestSync()
    }

    override fun requestSync() {
        scope.launch { syncNow() }
    }

    // 🔄 28-Aug-2026 — one pending nudge at a time: each save cancels the previous timer, so a
    //   burst of keystrokes lands as a single push once the typing stops.
    override fun requestSyncSoon() {
        saveNudge?.cancel()
        saveNudge = scope.launch {
            delay(SyncConfig.SAVE_DEBOUNCE_MILLIS)
            syncNow()
        }
    }

    override suspend fun syncNow(): Result<BaseResponse<SyncSummary>, Error> {
        val userId = session.userId
        if (!session.isAuthenticated || userId.isBlank()) {
            return Result.Error(NetworkError.SESSION_EXPIRED)
        }
        if (!online) {
            _syncState.value = SyncRunState.Offline
            return Result.Error(NetworkError.NO_INTERNET)
        }
        // 🔄 a run is already in flight — its result covers this caller too
        if (!runLock.tryLock()) {
            return Result.Success(BaseResponse(data = SyncSummary(), isSuccessful = true))
        }
        return try {
            runCycle(userId)
        } finally {
            runLock.unlock()
        }
    }

    /** 🔄 push BEFORE pull, always: a local edit must reach the server before the server's copy of
     *  that note can come back and overwrite it. Media bytes go up BEFORE their note, so a note
     *  never references an asset the server does not have yet. */
    private suspend fun runCycle(userId: String): Result<BaseResponse<SyncSummary>, Error> {
        _syncState.value = SyncRunState.Syncing
        mediaUploaded = 0
        healWatermarkIfStale(userId)

        // 🔄 28-Aug-2026 — outbound and inbound are INDEPENDENT halves. Returning here on a push
        //   error (the old behaviour) meant one note the server would not take stopped every
        //   incoming change from ever arriving again — sync looked dead while the network was fine.
        var failure: Error? = null

        val pushed = when (val result = pushDirtyNotes(userId)) {
            is Result.Error -> 0.also { failure = result.error }
            is Result.Success -> result.data
            is Result.Loading -> 0
        }

        val pulled = when (val result = pullChanges(userId)) {
            is Result.Error -> 0.also { if (failure == null) failure = result.error }
            is Result.Success -> result.data
            is Result.Loading -> 0
        }

        // 🖼️ eager download: everything the server has and this device does not, budget permitting
        val mediaDownloaded = if (failure == null) backfillMedia(userId) else 0

        if (pulled > 0 || pushed > 0 || mediaDownloaded > 0) localNotes.refreshFromDb()

        failure?.let { return failed(it) }

        failures = 0
        _syncState.value = SyncRunState.Success(getCurrentTimestamp(), pushed, pulled)
        log_d(TAG, "sync done: pushed=$pushed pulled=$pulled mediaUp=$mediaUploaded mediaDown=$mediaDownloaded")
        return Result.Success(
            BaseResponse(
                data = SyncSummary(pushed, pulled, mediaUploaded, mediaDownloaded),
                isSuccessful = true
            )
        )
    }

    /** 🔄 28-Aug-2026 — a watermark written by the first build can sit PAST a note that build
     *  declined to write, and the server never offers the same note twice. One full re-pull is the
     *  only way back, so every device does exactly one, the first time it runs this engine. */
    private suspend fun healWatermarkIfStale(userId: String) {
        if (userPrefs.getSyncGeneration() >= SyncConfig.RESYNC_GENERATION) return
        notesDao.resetSyncWatermark(userId)
        userPrefs.setSyncGeneration(SyncConfig.RESYNC_GENERATION)
        log_d(TAG, "watermark reset for sync generation ${SyncConfig.RESYNC_GENERATION} — re-pulling everything once")
    }

    /** 🖼️ one pass over the notes whose bytes are on the server but not here. Bounded per cycle;
     *  whatever does not fit is picked up next run, and the cap is logged rather than hidden. */
    private suspend fun backfillMedia(userId: String): Int {
        var budget = SyncConfig.MEDIA_FILES_PER_CYCLE
        var downloaded = 0

        for (noteId in notesDao.selectNoteIdsNeedingMedia(SyncConfig.MEDIA_NOTES_PER_CYCLE)) {
            if (budget <= 0) break
            val note = notesDao.selectNoteByIdIncludingDeleted(noteId) ?: continue

            val outcome = mediaSyncer.downloadMissing(userId, note, budget)
            if (outcome.moved > 0) {
                // 🖼️ insertOrUpdateNoteFromDb, NOT applyServerNote — this must preserve whatever
                //   syncStatus the note already had, or a locally edited note would be marked clean.
                notesDao.insertOrUpdateNoteFromDb(outcome.note)
                downloaded += outcome.moved
                budget -= outcome.moved
            }
            if (outcome.skipped > 0) log_d(TAG, "note $noteId: ${outcome.skipped} file(s) not downloaded")
        }

        if (budget <= 0) log_d(TAG, "media budget spent this cycle; the rest follows on the next run")
        return downloaded
    }

    private suspend fun pushDirtyNotes(userId: String): Result<Int, Error> {
        var pushed = 0
        // 🔄 28-Aug-2026 — every note gets exactly ONE attempt per cycle. Without this, a full
        //   batch the server keeps conflicting on made selectDirtyNotes() return the same rows
        //   forever and this loop hammered the server until the rate limiter cut the device off.
        val attempted = mutableSetOf<String>()
        while (true) {
            val dirty = notesDao.selectDirtyNotes(SyncConfig.PUSH_BATCH).filterNot { it.id in attempted }
            if (dirty.isEmpty()) return Result.Success(pushed)
            attempted += dirty.map { it.id }

            val (watermark, _) = notesDao.readSyncWatermark(userId)

            // 🖼️ bytes first. A note whose media upload failed still pushes — it just keeps no
            //   assetId, so the next cycle retries the file rather than the whole note.
            val prepared = dirty.map { note ->
                val outcome = mediaSyncer.uploadPending(note)
                if (outcome.moved > 0) {
                    notesDao.insertOrUpdateNoteFromDb(outcome.note)
                    mediaUploaded += outcome.moved
                }
                if (outcome.skipped > 0) {
                    log_d(TAG, "note ${note.id}: ${outcome.skipped} media file(s) not uploaded")
                }
                outcome.note
            }

            val request = SyncPushRequest(
                notes = prepared.map { NoteWireMapper.toWire(it) },
                lastPulledAt = watermark,
            )

            val response = when (val result = apiClient.syncPush(userId, request)) {
                is Result.Error -> return Result.Error(result.error)
                is Result.Success -> result.data.data
                is Result.Loading -> null
            } ?: return Result.Success(pushed)

            response.accepted.forEach { accepted ->
                notesDao.markNoteSynced(accepted.id, accepted.version, accepted.serverUpdatedAt)
                pushed++
            }

            // 🔄 28-Aug-2026 — the server won, and it ALREADY archived our losing edit to
            //   note_versions. This has to be a forced write: a conflicted note is dirty by
            //   definition, so the canApplyOverLocal() guard used on a pull would refuse it and
            //   leave the note dirty — re-pushed, re-conflicted, forever.
            response.conflicts.forEach { conflict ->
                conflict.server?.let { serverNote -> applyServerWins(serverNote) }
            }

            // 🔄 a rejected note stays DIRTY on purpose — marking it clean would lose the edit silently
            response.rejected.forEach { rejected ->
                log_d(TAG, "push rejected ${rejected.id}: ${rejected.code} ${rejected.message}")
            }
        }
    }

    private suspend fun pullChanges(userId: String): Result<Int, Error> {
        var (since, sinceId) = notesDao.readSyncWatermark(userId)
        var pulled = 0
        var page = 0

        while (page++ < SyncConfig.MAX_PULL_PAGES) {
            val result = apiClient.syncPull(userId, since, sinceId, SyncConfig.PULL_LIMIT)

            val body = when (result) {
                is Result.Error -> {
                    // 🔄 the only thing a 400 on pull can mean is a watermark older than the server's
                    //   tombstone window (PULL_WATERMARK_TOO_OLD). Answer: resync from the beginning.
                    if (result.error == NetworkError.BAD_REQUEST && since > 0L) {
                        log_d(TAG, "watermark too old — restarting a full resync")
                        notesDao.resetSyncWatermark(userId)
                        since = 0L
                        sinceId = null
                        continue
                    }
                    return Result.Error(result.error)
                }
                is Result.Success -> result.data.data
                is Result.Loading -> null
            } ?: return Result.Success(pulled)

            // 🔄 28-Aug-2026 — THE data-loss fix. applyIncoming() refuses to overwrite a note
            //   with unpushed local edits, but the watermark used to advance past it anyway: the
            //   server never sends that note again, so the incoming change was gone for good.
            //   Now the page stops at the first note we may not write, and the watermark stops
            //   with it — the next cycle, after push has cleaned that note, pulls it again.
            val applicable = body.notes.takeWhile { canApplyOver(it) }
            val stalled = applicable.size < body.notes.size
            val lastApplied = applicable.lastOrNull()

            val pageSince = if (stalled) (lastApplied?.serverUpdatedAt ?: since) else body.nextSince
            val pageSinceId = if (stalled) (lastApplied?.id ?: sinceId) else body.nextSinceId

            // 🔄 the page and the watermark it produced are committed together, so a crash re-pulls
            //   the page rather than skipping it
            notesDao.commitPulledPage(userId, pageSince, pageSinceId) {
                applicable.forEach { applyServerWins(it) }
            }

            pulled += applicable.size
            since = pageSince
            sinceId = pageSinceId

            if (stalled) {
                noteStalledOn(body.notes[applicable.size].id)
                break
            }
            clearStall()
            if (!body.hasMore) break
        }

        // 🔄 tombstones we have provably pulled past can finally go
        notesDao.purgeAckedTombstones(since)
        return Result.Success(pulled)
    }

    /** 🔄 may the server's copy of this note replace ours yet? No, while ours still holds an edit
     *  the server has not seen — that edit has not had its turn at the push. */
    private fun canApplyOver(incoming: Note): Boolean {
        val local = notesDao.selectNoteByIdIncludingDeleted(incoming.id)
        if (NoteWireMapper.canApplyOverLocal(local, SYNC_STATUS_SYNCED)) return true
        // 🔄 …unless push has already failed on it for several cycles. A note the server will
        //   never accept must not hold this device's whole inbound stream hostage.
        if (incoming.id == stalledNoteId && stalledCycles >= SyncConfig.MAX_STALLED_CYCLES) {
            log_d(TAG, "note ${incoming.id} could not be pushed in $stalledCycles cycles — taking the server copy")
            return true
        }
        log_d(TAG, "kept local unpushed edit for ${incoming.id}")
        return false
    }

    /** 🔄 keeps this device's media file paths. The caller has already decided the server wins. */
    private fun applyServerWins(incoming: Note) {
        val local = notesDao.selectNoteByIdIncludingDeleted(incoming.id)
        notesDao.applyServerNote(NoteWireMapper.mergeLocalMedia(incoming, local))
    }

    private fun noteStalledOn(noteId: String) {
        if (noteId == stalledNoteId) stalledCycles++ else { stalledNoteId = noteId; stalledCycles = 1 }
        log_d(TAG, "pull paused at $noteId (cycle $stalledCycles); it resumes once that note pushes")
    }

    private fun clearStall() {
        stalledNoteId = null
        stalledCycles = 0
    }

    private fun failed(error: Error): Result<BaseResponse<SyncSummary>, Error> {
        // 🔄 offline is a resting state, not a failure: no backoff, nothing to alarm the user with
        if (error == NetworkError.NO_INTERNET) {
            _syncState.value = SyncRunState.Offline
            return Result.Error(error)
        }
        failures++
        _syncState.value = SyncRunState.Failed(error.displayMessage(), currentBackoffMillis())
        log_d(TAG, "sync failed (${failures}x): ${error.displayMessage()}")
        return Result.Error(error)
    }

    private fun currentBackoffMillis(): Long {
        var backoff = SyncConfig.BASE_BACKOFF_MILLIS
        repeat((failures - 1).coerceAtLeast(0)) {
            if (backoff < SyncConfig.MAX_BACKOFF_MILLIS) backoff *= 2
        }
        return backoff.coerceAtMost(SyncConfig.MAX_BACKOFF_MILLIS)
    }

    private fun nextDelayMillis(): Long =
        if (_syncState.value is SyncRunState.Failed) currentBackoffMillis() else SyncConfig.INTERVAL_MILLIS
}
