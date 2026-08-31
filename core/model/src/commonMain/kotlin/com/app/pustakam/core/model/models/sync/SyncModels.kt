package com.app.pustakam.core.model.models.sync

import com.app.pustakam.core.model.models.response.notes.Note
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class SyncPushRequest(
    val notes: List<Note>,
    val lastPulledAt: Long? = null,
)

@Serializable
data class SyncPushResponse(
    val serverTime: Long = 0,
    val accepted: List<SyncAccepted> = emptyList(),
    val conflicts: List<SyncConflict> = emptyList(),
    val rejected: List<SyncRejected> = emptyList(),
)

@Serializable
data class SyncAccepted(
    val id: String,
    val version: String = "",
    val serverUpdatedAt: Long? = null,
)

// 🔄 the server ALWAYS wins a conflict, and archives the losing edit to note_versions before answering
@Serializable
data class SyncConflict(
    val id: String,
    val reason: String? = null,
    val server: Note? = null,
)

// 🔄 a rejected note stays dirty on purpose — marking it clean would lose the edit silently
@Serializable
data class SyncRejected(
    val id: String? = null,
    val code: String? = null,
    val message: String? = null,
    // 🔄 29-Aug-2026 — { "contents.0._id": ["Invalid id format"] }. Without this a rejection was
    //   an unexplained "the server would not take it", which is how one bad block hid for weeks.
    val fields: Map<String, List<String>>? = null,
)

@Serializable
data class SyncPullResponse(
    val serverTime: Long = 0,
    val notes: List<Note> = emptyList(),
    val hasMore: Boolean = false,
    val nextSince: Long = 0,
    val nextSinceId: String? = null,
)

// 🖼️ 20-Aug-2026 sync phase 2: POST /images answers with one entry per uploaded file
@Serializable
data class MediaUploadResponse(
    val files: List<MediaAsset> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class MediaAsset(
    val assetId: String,
    val url: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val checksum: String? = null,
)

// 🔄 the server's own error code for "your watermark predates the tombstone window" — means: full resync
const val PULL_WATERMARK_TOO_OLD = "PULL_WATERMARK_TOO_OLD"
