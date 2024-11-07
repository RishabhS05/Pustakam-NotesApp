package com.app.pustakam.koinDI

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class IosDispatcher: Dispatcher {
    override val io: CoroutineDispatcher
        get() = Dispatchers.Unconfined
}
internal actual fun provideDispatcher(): Dispatcher = IosDispatcher()