package com.app.pustakam.core.filesys.model

import com.app.pustakam.core.common.util.ContentType

// 🔧 30-Jul-2026 02:10 Phase 1 — what we know about a file WITHOUT opening it. Produced by platform
//   MetadataReader implementations (Phase 3) and consumed by pure validation/naming rules.
data class FileMetadata(
    val fileName: String,
    val extension: String,
    val mimeType: String?,
    val contentType: ContentType,
    val sizeBytes: Long = 0L,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val durationMs: Long = 0L,
)
