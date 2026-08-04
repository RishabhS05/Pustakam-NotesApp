package com.app.pustakam.core.data.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

abstract class BaseUseCase : KoinComponent {
    fun <T> getBaseApiCall(
        apiCall: suspend () -> Result<T, Error>
    ): Flow<Result<T, Error>> = flow {
        emit(Result.Loading)
        emit(apiCall())
    }.flowOn(Dispatchers.IO)
}
