package com.app.pustakam.core.common.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DomainEventBus {

    private val _events = MutableSharedFlow<DomainEvent>(replay = 0, extraBufferCapacity = 64)

    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    fun publish(event: DomainEvent) {
        _events.tryEmit(event)
    }
}
