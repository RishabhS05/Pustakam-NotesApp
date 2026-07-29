package com.app.pustakam.core.data.bridge

import com.app.pustakam.core.model.models.BaseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.bridge.BridgeError
import com.app.pustakam.core.common.bridge.Closeable
import com.app.pustakam.core.common.bridge.toBridgeError
import com.app.pustakam.core.common.util.Result

// 🔧 30-Jul-2026 02:10 internal->public: the feature-module bridges consume these across modules
fun <T> Flow<T>.watch(scope: CoroutineScope, onEach: (T) -> Unit): Closeable {
    val job = scope.launch { collectLatest { onEach(it) } }
    return Closeable(job)
}

/** Collects Flow<Result<BaseResponse<T>, Error>> from a use case and
 *  fans out to typed callbacks. One place that understands Result. */
fun <T> Flow<Result<BaseResponse<T>, Error>>.subscribe(
    scope: CoroutineScope,
    onLoading: () -> Unit,
    onSuccess: (T?) -> Unit,          // unwraps BaseResponse.data
    onError: (BridgeError) -> Unit
): Closeable {
    val job = scope.launch {
        collect { result ->
            when (result) {
                is Result.Loading -> onLoading()
                is Result.Success -> onSuccess(result.data?.data)
                is Result.Error -> onError(result.error.toBridgeError()) // 🔧 F1: typo fix
            }
            }
    }
    return Closeable(job)

}
fun <T> subscribeTo(
    scope: CoroutineScope,
    producer: suspend () -> Flow<Result<BaseResponse<T>, Error>>,
    onLoading: () -> Unit,
    onSuccess: (T?) -> Unit,
    onError: (BridgeError) -> Unit
): Closeable {
    val job = scope.launch {
        producer().collectLatest { result ->
            when (result) {
                is Result.Loading -> onLoading()
                is Result.Success -> onSuccess(result.data.data)
                is Result.Error   -> onError(result.error.toBridgeError()) // 🔧 F1: typo fix
            }
        }
    }
    return Closeable(job)
}