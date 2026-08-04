package com.app.pustakam.core.data.bridge

import com.app.pustakam.core.common.bridge.Closeable
import com.app.pustakam.core.common.events.DomainEvent
import com.app.pustakam.core.common.events.DomainEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EventsBridge : KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val bus: DomainEventBus by inject()

    fun observe(onEvent: (DomainEvent) -> Unit): Closeable =
        bus.events.watch(scope) { onEvent(it) }

    fun dispose() {
        scope.cancel()
    }
}
