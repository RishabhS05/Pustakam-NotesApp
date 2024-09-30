package com.app.pustakam.data.network

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.onSuccess
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException

class ApiCallClient() : BaseClient() {
    suspend fun login(login: Login) {
        val response = baseApiCall<BaseResponse<User>, NetworkError> {
            httpClient.post("${getUrl()}${ApiRoute.LOGIN.getName()}") {
                contentType(ContentType.Application.Json)
                setBody(login)
            }
        }
    }

    suspend fun register(user: User) {
        val response = baseApiCall<BaseResponse<User>, NetworkError> {
            httpClient.post(urlString = "${getUrl()}${ApiRoute.REGISTER.getName()}") {
                contentType(ContentType.Application.Json)
                setBody(user)
            }
        }
    }

    suspend fun getNotes(userId: String) {
        val response = baseApiCall<BaseResponse<Notes>, NetworkError> {
            httpClient.get(urlString = "${getUrl()}${ApiRoute.NOTES.getName()}/$userId") {
                contentType(ContentType.Application.Json)
                headers.apply {
                    set(headerAuth, token)
                    build()
                }
            }
        }
    }
}

