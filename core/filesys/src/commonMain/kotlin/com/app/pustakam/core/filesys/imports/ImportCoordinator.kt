package com.app.pustakam.core.filesys.imports

import com.app.pustakam.core.filesys.mime.MimeCatalog
import com.app.pustakam.core.filesys.model.ImportDecision
import com.app.pustakam.core.filesys.model.ImportPlan
import com.app.pustakam.core.filesys.naming.FileNameGenerator
import com.app.pustakam.core.filesys.path.PathPolicy
import com.app.pustakam.core.filesys.validation.ImportValidator

// 🔧 30-Jul-2026 02:10 Phase 2 — decides WHERE an imported file goes and WHAT it is called.
//   Performs NO file copying: it returns a plan and the caller does the IO. Every input that would
//   normally require a filesystem (does this name already exist?) is injected as a lambda, which is
//   what keeps this object pure and testable.
//
//   Package is `imports` rather than `import` because `import` is a hard keyword in Kotlin and
//   cannot be used as a package segment without backticks at every call site.
object ImportCoordinator {

    /**
     * Plan a single import.
     *
     * @param requestedName name offered by the picker or the server (Content-Disposition / URL tail)
     * @param exists probe for "is this file name already taken in the destination folder"
     */
    fun planImport(
        noteId: String,
        requestedName: String,
        mime: String? = null,
        sizeBytes: Long = 1L,
        sourceUrl: String = "",
        timestamp: Long,
        exists: (String) -> Boolean = { false },
    ): ImportDecision {
        if (requestedName.isBlank()) return ImportDecision.Rejected("blank file name")
        if (sizeBytes <= 0L) return ImportDecision.Rejected("empty payload")
        // Same rule that drives the user-visible "No file found at this link." message.
        if (!ImportValidator.isDownloadable(mime, requestedName)) return ImportDecision.NotAFile

        val folder = PathPolicy.importPath(noteId, requestedName).folder
        val uniqueName = FileNameGenerator.generateUnique(requestedName, timestamp) { candidate ->
            exists(PathPolicy.relativePath(folder, candidate))
        }
        val contentType = MimeCatalog.contentTypeFor(uniqueName, mime)

        return ImportDecision.Accepted(
            ImportPlan(
                destination = PathPolicy.importPath(noteId, uniqueName),
                contentType = contentType,
                mimeType = MimeCatalog.normalize(mime) ?: MimeCatalog.mimeFor(contentType),
                displayName = requestedName,
                sizeBytes = sizeBytes,
                sourceUrl = sourceUrl,
            ),
        )
    }

    /**
     * Plan a batch (multi-pick). Names chosen earlier in the batch are treated as taken, so two
     * picked files sharing a name cannot both resolve to the same destination — a collision the
     * per-file path could not see, because nothing has been written to disk yet.
     *
     * Rejected/not-a-file entries are returned in [ImportBatch.skipped] rather than dropped, so the
     * caller can tell the user how many of their files did not make it.
     */
    fun planImports(
        noteId: String,
        requests: List<ImportRequest>,
        timestamp: Long,
        exists: (String) -> Boolean = { false },
    ): ImportBatch {
        val reserved = mutableSetOf<String>()
        val accepted = mutableListOf<ImportPlan>()
        val skipped = mutableListOf<ImportDecision>()

        requests.forEach { request ->
            val decision = planImport(
                noteId = noteId,
                requestedName = request.fileName,
                mime = request.mime,
                sizeBytes = request.sizeBytes,
                sourceUrl = request.sourceUrl,
                timestamp = timestamp,
                exists = { path -> path in reserved || exists(path) },
            )
            when (decision) {
                is ImportDecision.Accepted -> {
                    reserved += decision.plan.relativePath
                    accepted += decision.plan
                }
                else -> skipped += decision
            }
        }
        return ImportBatch(accepted = accepted, skipped = skipped)
    }
}

/** One file offered for import, described without any platform handle. */
data class ImportRequest(
    val fileName: String,
    val mime: String? = null,
    val sizeBytes: Long = 1L,
    val sourceUrl: String = "",
)

/** Result of planning a batch: what to write, and what could not be. */
data class ImportBatch(
    val accepted: List<ImportPlan>,
    val skipped: List<ImportDecision>,
) {
    val hasWork: Boolean get() = accepted.isNotEmpty()
}
