package com.app.pustakam.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// 🔧 30-Jul-2026 02:10 internal->public: leaked through the now-public provideDispatcher()
class IosDispatcher: Dispatcher {
    override val io: CoroutineDispatcher
        get() = Dispatchers.Unconfined
}
// 🔧 30-Jul-2026 02:10 internal->public: actual visibility must match the expect
actual fun provideDispatcher(): Dispatcher = IosDispatcher()