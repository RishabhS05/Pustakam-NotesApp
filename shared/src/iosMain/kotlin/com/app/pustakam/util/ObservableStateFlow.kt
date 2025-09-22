package com.app.pustakam.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import platform.darwin.NSObject

class ObservableStateFlow<T>(private val stateFlow: StateFlow<T>) : NSObject() {
    val value: T
        get() = stateFlow.value
    var observer: ((T) -> Unit)? = null

    init {
        CoroutineScope(Dispatchers.Main).launch {
            stateFlow.collectLatest {
               observer?.invoke(it)
            }
        }
    }
    fun observe(block: (T) -> Unit) {
        this.observer = block
        block(stateFlow.value) // Emit initial value
    }
}