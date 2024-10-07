package com.app.pustakam.domain.reporsitories

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result

class BaseRepository() : IRemoteRepository, ILocalRepository{
    private val apiClient : ApiCallClient = ApiCallClient()
    override suspend fun loginUser(login : Login): Result<BaseResponse<User>, Error>
    = apiClient.login(login)

    override suspend fun registerUser(user: User): Result<BaseResponse<User>, Error>
    = apiClient.register(user)

    override suspend fun getNotesForUser(userId: String): Result<BaseResponse<Notes>, Error>
    = apiClient.getNotes(userId)


}