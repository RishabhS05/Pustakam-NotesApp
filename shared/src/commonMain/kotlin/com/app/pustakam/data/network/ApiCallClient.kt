package com.app.pustakam.data.network

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApiCallClient() : BaseClient() {
    suspend fun login(login: Login): Result<BaseResponse<User>, Error> =
        baseApiCall<BaseResponse<User>, NetworkError> {
                httpClient.post(ApiRoute.LOGIN.getName()) {
                    contentType(ContentType.Application.Json)
                    setBody(login)
                }
            }


    suspend fun register(user: RegisterReq) : Result<BaseResponse<User>, Error> =
        baseApiCall<BaseResponse<User>, NetworkError> {
            httpClient.post(urlString = ApiRoute.REGISTER.getName()) {
                contentType(ContentType.Application.Json)
                setBody(user)
            }
        }

    suspend fun getNotes(userId: String): Result<BaseResponse<Notes>, Error> =
        baseApiCall<BaseResponse<Notes>, NetworkError> {
            httpClient.get(urlString = "${ApiRoute.NOTES.getName()}/$userId") {
                contentType(ContentType.Application.Json)
                headers.apply {
                    set(headerAuth, token)
                    build()
                }
            }
        }

}

