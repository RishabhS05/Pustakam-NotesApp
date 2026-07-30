package com.app.pustakam.core.filesys.path

import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.export.ExportFormat
import com.app.pustakam.core.filesys.model.CaptureDestination
import com.app.pustakam.core.filesys.naming.FileNameGenerator

// 🔧 30-Jul-2026 02:10 Phase 1 — every folder convention in the app, in ONE place.
//   Returns paths RELATIVE to the platform's private app-storage root (filesDir / Documents).
//   Pure: no File, no NSURL, no Context. The roots below are the ones already on user devices —
//   changing any constant here orphans existing files, so PathPolicyTest pins them.
object PathPolicy {

    const val IMPORT_ROOT = "imported"
    const val EXPORT_ROOT = "exports"
    const val THUMBNAIL_ROOT = "thumbnails"

    /**
     * Capture folder for a content type. LOWERCASE is canonical because that is what Android has
     * written since day one and what exists on user devices today.
     * NOTE: iOS currently writes the UPPERCASE variant from its own copy of this rule. Phase 1 does
     * NOT touch the Swift call site, so nothing changes yet — aligning it, plus the one-time
     * migration that needs, is Phase 4. See 30jul2026-FIA doc §2.3b.
     */
    fun folderForContentType(type: ContentType): String = type.name.lowercase()

    /** Captured media: "image/<noteId>/<timestamp>.png" — matches NoteContentProvider.addContent. */
    fun capturePath(type: ContentType, noteId: String, timestamp: Long): CaptureDestination =
        CaptureDestination(
            folder = "${folderForContentType(type)}/$noteId",
            fileName = FileNameGenerator.generate(type, timestamp),
        )

    /** Imported file: "imported/<noteId>/<name>" — matches FileImportManager.destinationDir. */
    fun importPath(noteId: String, fileName: String): CaptureDestination =
        CaptureDestination(
            folder = "$IMPORT_ROOT/$noteId",
            fileName = FileNameGenerator.sanitize(fileName),
        )

    /** Thumbnail: "thumbnails/<sourceBase>_thumb.jpg" — matches FileOps.generateThumbnail. */
    fun thumbnailPath(sourceFileName: String): CaptureDestination =
        CaptureDestination(
            folder = THUMBNAIL_ROOT,
            fileName = FileNameGenerator.thumbnailName(sourceFileName),
        )

    /** Export: "exports/<safeTitle>-<timestamp>.pdf" — matches NoteExporter.export. */
    fun exportPath(noteTitle: String?, format: ExportFormat, timestamp: Long): CaptureDestination =
        CaptureDestination(
            folder = EXPORT_ROOT,
            fileName = FileNameGenerator.suggestExportName(noteTitle, format, timestamp),
        )

    /** Join a folder and a name the same way everywhere. */
    fun relativePath(folder: String, fileName: String): String =
        if (folder.isEmpty()) fileName else "$folder/$fileName"

    /** True when [relativePath] sits under a folder this app owns — a guard for deletes. */
    fun isManaged(relativePath: String): Boolean {
        val head = relativePath.trimStart('/').substringBefore('/')
        if (head == IMPORT_ROOT || head == EXPORT_ROOT || head == THUMBNAIL_ROOT) return true
        return ContentType.entries.any { it.name.lowercase() == head }
    }
}
