package com.app.pustakam.android.usecases

import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.util.Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import  com.app.pustakam.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent

import org.koin.java.KoinJavaComponent.inject
abstract class BaseUseCase : KoinComponent {
    val repository : BaseRepository by inject(BaseRepository::class.java)

     fun <T> getBaseApiCall(
        apiCall: suspend () -> Result<T, Error>
    ): Flow<Result<T , Error>> = flow {
        emit(Result.Loading)
         emit(apiCall())
    }.flowOn(Dispatchers.IO)

   suspend fun getUserId() : String = flow{
       val userId =  runBlocking {
           repository.getUserFromPrefId()?: ""
       }
       emit(userId)
    }.flowOn(Dispatchers.IO).map{it}.first()



    suspend fun getToken() : String  = flow{
       val token = runBlocking { repository.getUserFromPrefToken()?: "" }
        emit(token)
    }.flowOn(Dispatchers.IO).map{it}.first()
}