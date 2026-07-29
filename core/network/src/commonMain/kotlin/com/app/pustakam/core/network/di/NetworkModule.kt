package com.app.pustakam.core.network.di

import com.app.pustakam.core.network.ApiCallClient
import org.koin.core.module.Module
import org.koin.dsl.module

// 🔧 30-Jul-2026 02:10 — lifted VERBATIM out of :shared/koin/Koin.kt
fun networkModule(): Module = module {
    single<ApiCallClient> { ApiCallClient() }
}
