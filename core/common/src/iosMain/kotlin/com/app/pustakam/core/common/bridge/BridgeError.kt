package com.app.pustakam.core.common.bridge

import com.app.pustakam.util.ErrorMessage
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.ValidationError

data class BridgeError(
    val code: String,        // e.g. "NOT_FOUND", "SERVER_ERROR", "UNKNOWN"
    val message: String
)
// 🔧 F1: renamed toBrigeError → toBridgeError (typo)
internal fun Error.toBridgeError(): BridgeError = when (this) {
    // 🔧 F1: code = enum entry name — was hardcoded "SERVER_ERROR", which made
    //       NOT_FOUND/NO_INTERNET/etc. indistinguishable to Swift ViewModels
    is NetworkError -> BridgeError(code = name, message = getError())
    is ErrorMessage -> BridgeError(code = "UNKNOWN", message = message)
    is ValidationError -> BridgeError(code = "VALIDATION_$name", message = getError())
    else -> BridgeError(code = "UNKNOWN", message = toString())
}
