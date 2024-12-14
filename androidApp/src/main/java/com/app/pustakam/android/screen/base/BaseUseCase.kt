package com.app.pustakam.android.screen.base

import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.util.Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import  com.app.pustakam.util.Result
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent

import org.koin.core.component.get

abstract class BaseUseCase : KoinComponent {
    val repository = get<BaseRepository>()
     fun <T> getBaseApiCall(
        apiCall: suspend () -> Result<T, Error>
    ): Flow<Result<T , Error>> = flow {
         emit(Result.Loading)
         emit(apiCall())
    }.flowOn(Dispatchers.IO)
    suspend fun logoutUser(){
        repository.userLogout()
    }
}
