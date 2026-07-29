package com.app.pustakam.core.common.bridge

import com.app.pustakam.core.common.util.ErrorMessage
import com.app.pustakam.core.common.util.NetworkError
import com.app.pustakam.core.common.util.ValidationError
import com.app.pustakam.core.common.util.Error

data class BridgeError(
    val code: String,        // e.g. "NOT_FOUND", "SERVER_ERROR", "UNKNOWN"
    val message: String
)
// 🔧 F1: renamed toBrigeError → toBridgeError (typo)
// 🔧 30-Jul-2026 02:10 internal->public: FlowBridge lives in :core:data now, internal no longer reaches it
fun Error.toBridgeError(): BridgeError = when (this) {
    // 🔧 F1: code = enum entry name — was hardcoded "SERVER_ERROR", which made
    //       NOT_FOUND/NO_INTERNET/etc. indistinguishable to Swift ViewModels
    is NetworkError -> BridgeError(code = name, message = getError())
    is ErrorMessage -> BridgeError(code = "UNKNOWN", message = message)
    is ValidationError -> BridgeError(code = "VALIDATION_$name", message = getError())
    else -> BridgeError(code = "UNKNOWN", message = toString())
}
