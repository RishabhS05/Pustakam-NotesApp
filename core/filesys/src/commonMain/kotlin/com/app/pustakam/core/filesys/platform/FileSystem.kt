package com.app.pustakam.core.filesys.platform

import com.app.pustakam.core.filesys.model.FileMetadata

// 🔧 30-Jul-2026 02:10 Phase 3 — the platform seam, as a set of SMALL single-responsibility
//   interfaces rather than one god-object FileStore. Every path below is RELATIVE to the platform's
//   private app-storage root (Android filesDir / iOS Documents); resolving that root is the
//   implementation's job, so no caller ever handles an absolute path or a Context.
//
//   Implementations: androidMain uses java.io, iosMain uses NSFileManager.
//   Bound through Koin by `getFileSystemModule()` (expect/actual, per platform).

/** Read bytes out of app storage. */
interface FileReader {
    /** Whole file, or null when it does not exist / cannot be read. */
    fun read(relativePath: String): ByteArray?

    /**
     * Decoded text, truncated at [maxBytes] so a huge document cannot exhaust memory.
     * The cap exists because the book reader loads text files whole.
     */
    fun readText(relativePath: String, maxBytes: Long = Long.MAX_VALUE): String?
}

/** Write bytes into app storage, creating parent folders as needed. */
interface FileWriter {
    fun write(relativePath: String, bytes: ByteArray): Boolean
}

/** Folder existence and listing. */
interface DirectoryManager {
    /** Create [folder] and any missing parents. Returns false only on failure. */
    fun ensure(folder: String): Boolean

    /** True when a file OR folder exists at [relativePath]. */
    fun exists(relativePath: String): Boolean

    /** File names directly inside [folder]; empty when it does not exist. */
    fun list(folder: String): List<String>

    /** Size in bytes, or 0 when absent. */
    fun sizeOf(relativePath: String): Long
}

/**
 * Copy a file that lives OUTSIDE app storage into it — a SAF uri on Android, a picked
 * security-scoped URL on iOS. [sourceHandle] is an opaque platform string (uri / absolute path);
 * only the implementation interprets it.
 */
interface FileCopier {
    fun copyIn(sourceHandle: String, destinationRelativePath: String): Boolean
}

/** Delete from app storage. Deleting something that is already gone counts as success. */
interface FileDeleter {
    fun delete(relativePath: String): Boolean
}

/** Describe a file without the caller opening it. */
interface MetadataReader {
    // 🔧 30-Jul-2026 02:10 Phase 3 — named describe(), NOT read(): read(String) would collide with
    //   FileReader.read(String) and make it impossible for one class to implement both interfaces.
    fun describe(relativePath: String): FileMetadata?

    /** Name and size for a handle outside app storage (SAF uri / picked URL), pre-import. */
    fun describeHandle(sourceHandle: String): FileMetadata?
}
