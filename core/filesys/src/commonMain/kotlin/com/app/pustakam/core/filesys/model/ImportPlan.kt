package com.app.pustakam.core.filesys.model

import com.app.pustakam.core.common.util.ContentType

// 🔧 30-Jul-2026 02:10 Phase 2 — the DECISION about an import, with no IO attached.
//   ImportCoordinator produces this; platform code executes it. Holding no stream or file handle
//   is what makes the whole import rule unit-testable.
data class ImportPlan(
    val destination: CaptureDestination,
    val contentType: ContentType,
    /** Resolved mime — never null, falls back to octet-stream. */
    val mimeType: String,
    /** Original name as offered by the picker/server, before uniquing. Used as the media title. */
    val displayName: String,
    val sizeBytes: Long,
    /** Set only for link imports; empty for device picks. */
    val sourceUrl: String,
) {
    /** "imported/note-42/Report.pdf" — the one string platform code needs to write the file. */
    val relativePath: String get() = destination.relativePath
}

// 🔧 30-Jul-2026 02:10 Phase 2 — outcome of PLANNING an import (not of performing one).
//   Deliberately mirrors the existing androidApp ImportResult cases so the Android call site maps
//   1:1 with no new UI state and no ViewModel change.
sealed interface ImportDecision {

    /** Go ahead and copy the bytes to [plan]. */
    data class Accepted(val plan: ImportPlan) : ImportDecision

    /** The response was a web page, or carried nothing we can treat as a file.
     *  Maps to the existing "No file found at this link." message. */
    data object NotAFile : ImportDecision

    /** Structurally unusable input (blank name, empty payload). [reason] is developer-facing. */
    data class Rejected(val reason: String) : ImportDecision
}
