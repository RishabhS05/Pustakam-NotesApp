package com.app.pustakam.core.filesys.di

import com.app.pustakam.core.filesys.platform.AndroidDirectoryManager
import com.app.pustakam.core.filesys.platform.AndroidDocumentRenderer
import com.app.pustakam.core.filesys.platform.AndroidFileCopier
import com.app.pustakam.core.filesys.platform.AndroidFileDeleter
import com.app.pustakam.core.filesys.platform.AndroidFileReader
import com.app.pustakam.core.filesys.platform.AndroidFileWriter
import com.app.pustakam.core.filesys.platform.AndroidMetadataReader
import com.app.pustakam.core.filesys.platform.AndroidStoragePaths
import com.app.pustakam.core.filesys.platform.AndroidTextMeasurer
import com.app.pustakam.core.filesys.platform.AndroidThumbnailGenerator
import com.app.pustakam.core.filesys.platform.DirectoryManager
import com.app.pustakam.core.filesys.platform.DocumentRenderer
import com.app.pustakam.core.filesys.platform.FileCopier
import com.app.pustakam.core.filesys.platform.FileDeleter
import com.app.pustakam.core.filesys.platform.FileReader
import com.app.pustakam.core.filesys.platform.FileWriter
import com.app.pustakam.core.filesys.platform.MetadataReader
import com.app.pustakam.core.filesys.platform.StoragePaths
import com.app.pustakam.core.filesys.platform.TextMeasurer
import com.app.pustakam.core.filesys.platform.ThumbnailGenerator
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

// 🔧 30-Jul-2026 02:10 Phase 3 — all nine seams bound on Android. Each is a small stateless class
//   over context.filesDir, so `single` is safe and cheap.
actual fun getFileSystemModule(): Module = module {
    single<FileReader> { AndroidFileReader(androidContext()) }
    single<FileWriter> { AndroidFileWriter(androidContext()) }
    single<DirectoryManager> { AndroidDirectoryManager(androidContext()) }
    single<FileCopier> { AndroidFileCopier(androidContext()) }
    single<FileDeleter> { AndroidFileDeleter(androidContext()) }
    single<MetadataReader> { AndroidMetadataReader(androidContext()) }
    single<StoragePaths> { AndroidStoragePaths(androidContext()) }
    single<ThumbnailGenerator> { AndroidThumbnailGenerator(androidContext()) }
    single<DocumentRenderer> { AndroidDocumentRenderer(androidContext()) }
    single<TextMeasurer> { AndroidTextMeasurer() }
}
