package com.app.pustakam.domain.repositories

import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import com.app.pustakam.util.onSuccess
import org.koin.core.component.KoinComponent

open class BaseRepository(private val userPrefs :  IAppPreferences) : IRemoteRepository, ILocalRepository, KoinComponent{

    private val apiClient : ApiCallClient = ApiCallClient(userPrefs)
    override suspend fun loginUser(login : Login): Result<BaseResponse<User>, Error>
    = apiClient.login(login)
    override suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error>
    = apiClient.register(user)
    override suspend fun getNotesForUser(userId: String, page : Int ,): Result<BaseResponse<Notes>, Error>
    = apiClient.getNotes(userId)
    override suspend fun addNewNote(userId: String, note: NoteRequest): Result<BaseResponse<Note>, Error>
    = apiClient.addNewNote(userId,note)
    override suspend fun updateNote(userId: String,note: NoteRequest): Result<BaseResponse<Note>, Error>
    = apiClient.updateNote(userId,note)
    override suspend fun updateUser(user: User): Result<BaseResponse<User>, Error>
    = apiClient.updateUser(user)
    override suspend fun getUser(userId: String): Result<BaseResponse<User>, Error>
    = apiClient.getUser(userId)

    override suspend fun deleteUser(userId: String): Result<BaseResponse<User>, Error>
    = apiClient.deleteUser(userId)

    override suspend fun deleteNote(userId: String,noteId: String): Result<BaseResponse<Note>, Error>
    = apiClient.deleteNote(userId,noteId)

    override suspend fun getNote(userId: String, noteId : String): Result<BaseResponse<User>, Error>
            = apiClient.getNote(userId, noteId)
    override suspend fun profileImage(): Result<BaseResponse<User>, Error> =
        apiClient.profileImage()

    override suspend fun getUserFromPrefToken() : String? =
     userPrefs.getUserId()


    override suspend fun setUserFromPrefToken(token : String) {
     userPrefs.setToken(token)
    }

    override suspend fun setUserFromPrefId(userId: String) {
      userPrefs.setUserId(userId)
    }

    override suspend fun getUserFromPrefId()  :String? = userPrefs.getUserId()


}