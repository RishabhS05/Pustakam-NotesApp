package com.app.pustakam.data.network

import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login

import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.DeleteDataModel
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApiCallClient(userPrefs : IAppPreferences) : BaseClient(userPrefs) {

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
            httpClient.get(urlString = "${ApiRoute.NOTES.getName()}/$userId"){
                contentType(ContentType.Application.Json)
                headers.apply {
                    val token = userPrefs.getAuthToken()?:""
                    println(token)
                    append(headerAuth,token)
                }
            }
        }
    suspend fun getNote(userId : String, noteId :String): Result<BaseResponse<Note>, Error> =
        baseApiCall <BaseResponse<Note>, NetworkError>  {
        httpClient.get(urlString = "${ApiRoute.NOTES.getName()}/$userId/$noteId"){
            headers.apply {
                val token = userPrefs.getAuthToken() ?: ""
                append(headerAuth,token)
            }
        }
    }
 suspend fun updateNote(userId: String,note: Note): Result<BaseResponse<Note>, Error> =
     baseApiCall<BaseResponse<Note>, NetworkError> {
     httpClient.post(urlString = "${ApiRoute.NOTES.getName()}/$userId/${note._id}"){
         contentType(ContentType.Application.Json)
         headers.apply {
             val token = userPrefs.getAuthToken()?:""
             append(headerAuth,token)
         }
         setBody(note)
     }
 }
    suspend fun deleteNote(userId: String,noteId: String) : Result<BaseResponse<DeleteDataModel>, Error> =
        baseApiCall<BaseResponse<DeleteDataModel>, NetworkError> {
        httpClient.delete(urlString = "${ApiRoute.NOTES.getName()}/$userId/$noteId"){
            contentType(ContentType.Application.Json)
            headers.apply {
                val token = userPrefs.getAuthToken()?:""
                append(headerAuth,token)
            }
        }
    }
    suspend fun addNewNote(userId: String,note : Note) : Result<BaseResponse<Note>, Error> =
        baseApiCall<BaseResponse<Note>, NetworkError> {
        httpClient.post(urlString = "${ApiRoute.NOTES.getName()}/$userId") {
            contentType(ContentType.Application.Json)
            headers.apply {
                val token = userPrefs.getAuthToken()?:""
                append(headerAuth,token)
            }
            setBody(note)
        }
    }
    suspend fun getUser(userId : String): Result<BaseResponse<User>, Error> = baseApiCall <BaseResponse<User>, NetworkError>  {
        httpClient.get(urlString = "${ApiRoute.USERS.getName()}/$userId"){
            contentType(ContentType.Application.Json)
            headers.apply {
                val token = userPrefs.getAuthToken()?:""
                append(headerAuth,token)
            }
        }
    }

    suspend fun updateUser(user : User) : Result<BaseResponse<User>, Error> =
        baseApiCall<BaseResponse<User>, NetworkError> {
            httpClient.post(urlString = "${ApiRoute.USERS.getName()}/${user._id}") {
                contentType(ContentType.Application.Json)
                headers.apply {
                    val token = userPrefs.getAuthToken()?:""
                    append(headerAuth,token)
                }
                setBody(user)
            }
        }
    suspend fun deleteUser(userId : String) : Result<BaseResponse<User>, Error> =
        baseApiCall<BaseResponse<User>, NetworkError> {
            httpClient.delete(urlString = "${ApiRoute.USERS.getName()}/$userId"){
                contentType(ContentType.Application.Json)
                headers.apply {
                    val token = userPrefs.getAuthToken() ?: ""
                    append(headerAuth,token)
                }
            }
        }
    suspend fun profileImage() : Result<BaseResponse<User>, Error> =
        baseApiCall<BaseResponse<User>, NetworkError> {
            httpClient.post(urlString = ApiRoute.PROFILE.getName()){
                contentType(ContentType.Application.FormUrlEncoded)
            }
        }
}

