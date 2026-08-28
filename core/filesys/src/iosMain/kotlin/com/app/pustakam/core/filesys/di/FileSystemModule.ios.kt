package com.app.pustakam.core.filesys.di

import com.app.pustakam.core.filesys.platform.DirectoryManager
import com.app.pustakam.core.filesys.platform.FileCopier
import com.app.pustakam.core.filesys.platform.FileDeleter
import com.app.pustakam.core.filesys.platform.FileReader
import com.app.pustakam.core.filesys.platform.FileWriter
import com.app.pustakam.core.filesys.platform.IosDirectoryManager
import com.app.pustakam.core.filesys.platform.IosFileCopier
import com.app.pustakam.core.filesys.platform.IosFileDeleter
import com.app.pustakam.core.filesys.platform.IosFileReader
import com.app.pustakam.core.filesys.platform.IosFileWriter
import com.app.pustakam.core.filesys.platform.IosMetadataReader
import com.app.pustakam.core.filesys.platform.IosStoragePaths
import com.app.pustakam.core.filesys.platform.MetadataReader
import com.app.pustakam.core.filesys.platform.StoragePaths
import org.koin.core.module.Module
import org.koin.dsl.module

// 🔧 30-Jul-2026 02:10 Phase 3 — the six file-IO seams bound on iOS via NSFileManager.
//
//   ThumbnailGenerator, DocumentRenderer and TextMeasurer are deliberately NOT bound here yet.
//   iOS already implements all three in Swift (FileOps.generateThumbnail, PDFDocument in
//   BookReaderView, UIFont measurement in NoteExporter). Re-writing them as Kotlin/Native UIKit +
//   PDFKit + AVFoundation interop before anything calls them would be speculative code that cannot
//   be exercised. They are bound in Phase 4, when the Swift call sites move across together.
actual fun getFileSystemModule(): Module = module {
    single<FileReader> { IosFileReader() }
    single<FileWriter> { IosFileWriter() }
    single<DirectoryManager> { IosDirectoryManager() }
    single<FileCopier> { IosFileCopier() }
    single<FileDeleter> { IosFileDeleter() }
    single<MetadataReader> { IosMetadataReader() }
    single<StoragePaths> { IosStoragePaths() }
}
