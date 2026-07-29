package com.app.pustakam.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

interface Dispatcher {
    val io: CoroutineDispatcher
}

// 🔧 30-Jul-2026 02:10 internal->public: :core:database now calls this across a module boundary
expect fun provideDispatcher(): Dispatcher