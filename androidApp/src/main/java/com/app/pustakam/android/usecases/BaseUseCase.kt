package com.app.pustakam.android.usecases

import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.util.Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import  com.app.pustakam.util.Result
import kotlinx.coroutines.flow.flowOn

abstract class BaseUseCase {
    val repository = BaseRepository()
     fun <T> getBaseApiCall(
        apiCall: suspend () -> Result<T, Error>
    ): Flow<Result<T , Error>> = flow {
        emit(Result.Loading)
         emit(apiCall())
    } .flowOn(Dispatchers.IO)
}