package com.app.pustakam.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.darwin.NSObject

class ObservableStateFlow<T>(private val stateFlow: StateFlow<T>) : NSObject() {
    val value: T
        get() = stateFlow.value

    init {
        CoroutineScope(Dispatchers.Main).launch {
            stateFlow.collect {
                // Notify observers here if necessary
            }
        }
    }
}