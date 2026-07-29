package com.app.pustakam.core.data.base

import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.Login
import com.app.pustakam.core.model.models.request.RegisterReq
import com.app.pustakam.core.model.models.response.User
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

interface IRemoteRepository {
    //user Apis
    suspend fun loginUser(login : Login) : Result<BaseResponse<User>, Error>
    suspend fun registerUser(user  : RegisterReq) : Result<BaseResponse<User>, Error>
    suspend fun updateUser(user : User) : Result<BaseResponse<User>, Error>
    suspend fun getUser(userId : String) : Result<BaseResponse<User>, Error>
    suspend fun deleteUser() : Result<BaseResponse<User>, Error>
    suspend fun userLogout()
    suspend fun profileImage() : Result<BaseResponse<User>,Error>

}