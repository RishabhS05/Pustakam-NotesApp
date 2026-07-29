package com.app.pustakam.core.network

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login

import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.DeleteDataModel
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.NetworkError
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApiCallClient : BaseClient() {

    suspend fun login(login: Login): Result<BaseResponse<User>, Error> =
     post(ApiRoute.LOGIN.getName(), requestData = login)

    suspend fun register(user: RegisterReq): Result<BaseResponse<User>, Error> =
    post(url = ApiRoute.REGISTER.getName(), requestData = user)

    suspend fun getNotes(userId: String): Result<BaseResponse<Notes>, Error> =
        get(url = "${ApiRoute.NOTES.getName()}/$userId")

    suspend fun getNote(userId: String, noteId: String): Result<BaseResponse<Note>, Error> =
        get(url = "${ApiRoute.NOTES.getName()}/$userId/$noteId")


    suspend fun updateNote(userId: String, note: Note): Result<BaseResponse<Note>, Error> =
        post(url = "${ApiRoute.NOTES.getName()}/$userId/${note.id}", requestData = note)


    suspend fun deleteNote(
        userId: String, noteId: String
    ): Result<BaseResponse<DeleteDataModel>, Error> =

            delete(url = "${ApiRoute.NOTES.getName()}/$userId/$noteId")

    suspend fun addNewNote(userId: String, note: Note): Result<BaseResponse<Note>, Error> =
        post(url= "${ApiRoute.NOTES.getName()}/$userId", requestData = note)


    suspend fun getUser(userId: String): Result<BaseResponse<User>, Error> =
        get(url = "${ApiRoute.USERS.getName()}/$userId")



    suspend fun updateUser(user: User): Result<BaseResponse<User>, Error> =
         post(url = "${ApiRoute.USERS.getName()}/${user._id}", requestData = user)


    suspend fun deleteUser(userId: String): Result<BaseResponse<User>, Error> =
        delete(url = "${ApiRoute.USERS.getName()}/$userId")


    suspend fun profileImage(): Result<BaseResponse<User>, Error> =
        post(url = ApiRoute.PROFILE.getName(),contentType = ContentType.Application.FormUrlEncoded, requestData = null)


    // actual api calls
    private suspend inline fun <reified T> get(
        url: String, contentType: ContentType = ContentType.Application.Json
    ) = baseApiCall<T, NetworkError> {
        httpClient.get(urlString = url) {
            contentType(contentType)
        }
    }

    private suspend inline fun <reified T> post(
        url: String, contentType: ContentType = ContentType.Application.Json, requestData: Any?
    ) = baseApiCall<T, NetworkError> {
        httpClient.post (urlString = url) {
            contentType(contentType)
            setBody(requestData)
        }
    }

    private suspend inline fun <reified T> delete(url: String, contentType: ContentType = ContentType.Application.Json)
    = baseApiCall<T, NetworkError> {
        httpClient.delete(urlString = url) {
            contentType(contentType)
        }
    }
}