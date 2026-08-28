package com.app.pustakam.feature.notes.data.sync

import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.common.util.log_d
import com.app.pustakam.core.common.util.resolveLocalFilePath
import com.app.pustakam.core.filesys.mime.MimeCatalog
import com.app.pustakam.core.filesys.path.PathPolicy
import com.app.pustakam.core.filesys.platform.FileReader
import com.app.pustakam.core.filesys.platform.FileWriter
import com.app.pustakam.core.filesys.platform.StoragePaths
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.sync.SyncConfig
import com.app.pustakam.core.network.ApiCallClient
import com.app.pustakam.core.network.MediaUpload
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "MediaSyncer"

/**
 * 🖼️ 20-Aug-2026 — the bytes half of sync.
 *
 * A media block travels as `assetId` + `checksum` + `mimeType`/`sizeBytes`. `localPath` is this
 * device's own copy and never leaves it: the server strips it (backend.md S1-11/E22) and
 * [NoteWireMapper] does not send it.
 *
 * Nothing here throws. A file that cannot move is SKIPPED and counted, because one oversized video
 * must not stop a hundred notes from syncing.
 */
internal class MediaSyncer : KoinComponent {

    private val apiClient: ApiCallClient by inject()
    private val fileReader: FileReader by inject()
    private val fileWriter: FileWriter by inject()
    private val storagePaths: StoragePaths by inject()

    /** [note] is unchanged (same instance) when nothing moved, so callers can skip the DB write. */
    data class Outcome(val note: Note, val moved: Int, val skipped: Int)

    /** 🖼️ runs BEFORE the note is pushed, so a note never references an asset the server lacks. */
    suspend fun uploadPending(note: Note): Outcome {
        if (note.deleted) return Outcome(note, 0, 0)
        var moved = 0
        var skipped = 0

        val contents = note.contents.map { content ->
            if (content !is NoteContentModel.MediaContent || !content.needsUpload()) return@map content

            val absolute = resolveLocalFilePath(content.localPath)
                ?: return@map content.also { skipped++ }

            // 🖼️ null means the file sits outside app storage — not ours to read
            val relative = storagePaths.toRelative(absolute)
                ?: return@map content.also {
                    skipped++
                    log_d(TAG, "skipped ${content.id}: outside app storage")
                }

            val bytes = fileReader.read(relative)
                ?: return@map content.also {
                    skipped++
                    log_d(TAG, "skipped ${content.id}: file is gone from disk")
                }

            if (bytes.size > SyncConfig.MAX_MEDIA_BYTES) {
                skipped++
                log_d(TAG, "skipped ${content.id}: ${bytes.size} bytes exceeds the ${SyncConfig.MAX_MEDIA_BYTES} limit")
                return@map content
            }

            val upload = MediaUpload(
                contentId = content.id,
                fileName = relative.substringAfterLast('/'),
                mimeType = content.mimeType.ifBlank { MimeCatalog.mimeFor(content.type) },
                bytes = bytes,
            )

            val asset = when (val result = apiClient.uploadMedia(upload)) {
                is Result.Success -> result.data.data?.files?.firstOrNull()
                else -> null
            }

            if (asset == null) {
                skipped++
                log_d(TAG, "upload failed for ${content.id}; it stays local and retries next cycle")
                return@map content
            }

            moved++
            // 🖼️ withAsset does NOT touch updatedAt — gaining a server identity is not a user edit
            content.withAsset(asset.assetId, asset.url, asset.checksum)
        }

        // copy(): stamping an assetId must not re-stamp updatedAt the way withContents() would
        return Outcome(if (moved > 0) note.copy(contents = contents) else note, moved, skipped)
    }

    /** 🖼️ eager, but [budget] bounded so a first sync on a large library cannot stall the cycle. */
    suspend fun downloadMissing(userId: String, note: Note, budget: Int): Outcome {
        if (note.deleted) return Outcome(note, 0, 0)
        var moved = 0
        var skipped = 0
        var remaining = budget

        val contents = note.contents.map { content ->
            if (content !is NoteContentModel.MediaContent || !content.needsDownload()) return@map content
            val assetId = content.assetId ?: return@map content

            if (remaining <= 0) {
                skipped++
                return@map content
            }
            remaining--

            val bytes = when (val result = apiClient.downloadMedia(userId, assetId)) {
                is Result.Success -> result.data
                else -> return@map content.also {
                    skipped++
                    log_d(TAG, "download failed for asset $assetId; retries next cycle")
                }
            }

            val destination = PathPolicy.importPath(note.id, fileNameFor(assetId, content))
            if (!fileWriter.write(destination.relativePath, bytes)) {
                skipped++
                log_d(TAG, "could not write ${destination.relativePath}")
                return@map content
            }

            moved++
            // 🖼️ ABSOLUTE — that is the form getMediaUrl()/resolveLocalFilePath and every
            //   playback call site expect. Storing the relative path here breaks rendering.
            content.withLocalPath(storagePaths.toAbsolute(destination.relativePath))
        }

        return Outcome(if (moved > 0) note.copy(contents = contents) else note, moved, skipped)
    }

    // 🖼️ the asset id IS unique, so no collision handling is needed; the extension keeps the
    //   existing type-detection-by-name path working (MimeCatalog.contentTypeFor).
    private fun fileNameFor(assetId: String, content: NoteContentModel.MediaContent): String {
        val type = MimeCatalog.contentTypeForMime(content.mimeType) ?: content.type
        return "$assetId${MimeCatalog.extensionFor(type)}"
    }
}
