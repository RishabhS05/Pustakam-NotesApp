package com.app.pustakam.data.network

import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.log_d
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

abstract class BaseClient(val userPrefs : IAppPreferences ) {
    protected val httpClient: HttpClient = createHttpClient()
    suspend inline fun < reified T, E: Error> baseApiCall(
        crossinline actualApiCall : suspend  () -> HttpResponse
    ) : Result<T, Error> = flow {
           val response : HttpResponse = try {
               actualApiCall.invoke()
           } catch (e: UnresolvedAddressException) {
               log_d("Error", "$e")
               emit(Result.Error(NetworkError.NO_INTERNET))
               return@flow
           }
           catch (e: SerializationException){
               log_d("Error", "$e")
               emit( Result.Error(NetworkError.SERIALIZATION))
               return@flow
           }
        log_d("auth"," ${response.headers["authorization"]}")
        if(userPrefs.getAuthToken().isNullOrEmpty()) {
           val token = response.headers["authorization"].toString()
            log_d("auth","${response.headers["authorization"]}")
            userPrefs.setToken(token)
            log_d("Token" ," $token")
        }
           when (response.status.value){
               in 200..299 -> emit(Result.Success(response.body<T>()))
               400 -> emit(Result.Error(NetworkError.NOT_FOUND))
               401 -> emit(Result.Error(NetworkError.UNAUTHORIZED))
               409 -> emit(Result.Error(NetworkError.CONFLICT))
               408 -> emit(Result.Error(NetworkError.REQUEST_TIMEOUT))
               413 -> emit(Result.Error(NetworkError.PAYLOAD_TOO_LARGE))
               in 500 ..599 -> emit(Result.Error(NetworkError.SERVER_ERROR))
               else ->  emit(Result.Error(NetworkError.UNKNOWN))
           }

       }.flowOn(Dispatchers.IO).map { it }.first()
     }
