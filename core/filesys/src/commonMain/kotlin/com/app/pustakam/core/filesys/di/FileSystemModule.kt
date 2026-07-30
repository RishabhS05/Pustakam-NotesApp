package com.app.pustakam.core.filesys.di

import org.koin.core.module.Module

// 🔧 30-Jul-2026 02:10 Phase 3 — platform bindings for the filesys seams.
//   expect/actual because the Android implementations need a Context, which only the Android Koin
//   module can supply (androidContext()). Same idiom the project already uses for
//   getDatabaseModule() and getDataSourceFromPlatForm().
//
//   NOT YET COMPOSED into initKoin(): nothing calls these interfaces until Phase 4, and registering
//   unused singletons would only add cold-start work. Add to :shared/koin/Koin.kt in Phase 4.
expect fun getFileSystemModule(): Module
