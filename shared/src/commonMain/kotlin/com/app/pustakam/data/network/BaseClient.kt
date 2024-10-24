package com.app.pustakam.data.network

import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.SerializationException

abstract class BaseClient {
    protected val httpClient: HttpClient = createHttpClient()
    var token : String =""
    suspend inline fun <reified T, E: Error> baseApiCall(actualApiCall :  suspend ()-> HttpResponse ) :
            Result<T, Error> {
        val response = try {
            CoroutineScope(Dispatchers.IO).run {
                actualApiCall()
            }
         } catch (e: UnresolvedAddressException){
             return Result.Error(NetworkError.NO_INTERNET)
         }
         catch (e: SerializationException){
             return Result.Error(NetworkError.SERIALIZATION)
         }
        if(token.isEmpty()) {
            token = response.headers["authorization"].toString()
            println("auth ${response.headers["authorization"]}")
            BaseRepository.token = token
        }
        print("Token ; $token")
         return when (response.status.value){
             in 200..299 -> Result.Success(response.body<T>())
             400-> Result.Error(NetworkError.NOT_FOUND)
             401 -> Result.Error(NetworkError.UNAUTHORIZED)
             409 -> Result.Error(NetworkError.CONFLICT)
             408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
             413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
             in 500 ..599 ->  Result.Error(NetworkError.SERVER_ERROR)
             else ->  Result.Error(NetworkError.UNKNOWN)
         }
     }
}