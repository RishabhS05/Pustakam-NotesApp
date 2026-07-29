package com.app.pustakam.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


class AndroidDispatcherIO : Dispatcher {
    override val io: CoroutineDispatcher
        get() = Dispatchers.IO
}
// 🔧 30-Jul-2026 02:10 internal->public: actual visibility must match the expect
actual fun provideDispatcher(): Dispatcher = AndroidDispatcherIO()