package com.app.pustakam.domain.repositories

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result

open class BaseRepository : IRemoteRepository, ILocalRepository{
    private val apiClient : ApiCallClient = ApiCallClient()

    override suspend fun loginUser(login : Login): Result<BaseResponse<User>, Error> {
       return apiClient.login(login)
    }
    override suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error>
    = apiClient.register(user)

    override suspend fun getNotesForUser(userId: String): Result<BaseResponse<Notes>, Error>
    = apiClient.getNotes(userId)
}