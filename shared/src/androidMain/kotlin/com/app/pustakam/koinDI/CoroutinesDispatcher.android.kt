package com.app.pustakam.koinDI

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidDispatcherIO : Dispatcher {
    override val io: CoroutineDispatcher
        get() = Dispatchers.IO
}
internal actual fun provideDispatcher(): Dispatcher = AndroidDispatcherIO()