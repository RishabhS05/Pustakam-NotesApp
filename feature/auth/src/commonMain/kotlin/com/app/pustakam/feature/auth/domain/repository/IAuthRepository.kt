package com.app.pustakam.feature.auth.domain.repository

import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.database.localdb.preferences.UserPreference
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.Login
import com.app.pustakam.core.model.models.request.RegisterReq
import com.app.pustakam.core.model.models.response.User
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    suspend fun loginUser(login: Login): Result<BaseResponse<User>, Error>
    suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error>
    suspend fun updateUser(user: User): Result<BaseResponse<User>, Error>
    suspend fun getUser(userId: String): Result<BaseResponse<User>, Error>
    suspend fun deleteUser(): Result<BaseResponse<User>, Error>
    suspend fun profileImage(): Result<BaseResponse<User>, Error>
    suspend fun userLogout()

    val authState: StateFlow<UserPreference>
}
