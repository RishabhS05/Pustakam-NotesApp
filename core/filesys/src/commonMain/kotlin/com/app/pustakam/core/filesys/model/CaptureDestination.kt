package com.app.pustakam.core.filesys.model

// 🔧 30-Jul-2026 02:10 Phase 1 — where a file belongs, decided in commonMain. Holds NO file handle:
//   platform code turns `relativePath` into a java.io.File / NSURL under its own app-storage root.
data class CaptureDestination(
    /** Folder relative to the platform's private app-storage root, e.g. "image/note-42". */
    val folder: String,
    /** File name including extension, e.g. "1753832400000.png". */
    val fileName: String,
) {
    /** "image/note-42/1753832400000.png" — the only string platform code needs. */
    val relativePath: String get() = if (folder.isEmpty()) fileName else "$folder/$fileName"
}
