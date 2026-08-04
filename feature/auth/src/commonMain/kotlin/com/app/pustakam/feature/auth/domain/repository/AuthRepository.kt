package com.app.pustakam.feature.auth.domain.repository

import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.common.util.onSuccess
import com.app.pustakam.core.data.base.BaseRepository
import com.app.pustakam.core.database.localdb.preferences.UserPreference
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.Login
import com.app.pustakam.core.model.models.request.RegisterReq
import com.app.pustakam.core.model.models.response.User
import kotlinx.coroutines.flow.StateFlow

internal class AuthRepository : BaseRepository(), IAuthRepository {

    override val authState: StateFlow<UserPreference> = session.state

    override suspend fun loginUser(login: Login): Result<BaseResponse<User>, Error> =
        apiClient.login(login).onSuccess {
            it.data?._id?.let { id ->
                userPrefs.setUserId(id)
                userPrefs.setAuth(true)
            }
        }

    override suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error> =
        apiClient.register(user)

    override suspend fun updateUser(user: User): Result<BaseResponse<User>, Error> =
        apiClient.updateUser(user)

    // pass empty string to get current user
    override suspend fun getUser(userId: String): Result<BaseResponse<User>, Error> =
        apiClient.getUser(userId.ifEmpty { session.userId })

    override suspend fun deleteUser(): Result<BaseResponse<User>, Error> =
        apiClient.deleteUser(session.userId)

    override suspend fun profileImage(): Result<BaseResponse<User>, Error> =
        apiClient.profileImage()

    override suspend fun userLogout() {
        userPrefs.clear()
    }
}
