package com.app.pustakam.feature.auth.domain.usecase

import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.RegisterReq
import com.app.pustakam.core.model.models.response.User
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

class SignUseCase : AuthBaseUseCase() {
    suspend operator fun invoke(user: RegisterReq): Flow<Result<BaseResponse<User>, Error>> = getBaseApiCall {
        authRepository.registerUser(user = user)
    }
}

class DeleteUserUseCase : AuthBaseUseCase() {
    suspend operator fun invoke() = authRepository.deleteUser()
}

class UpdateUserUseCase : AuthBaseUseCase() {
    suspend operator fun invoke(user: User) = authRepository.updateUser(user)
}

class ReadUserUseCase : AuthBaseUseCase() {
    suspend operator fun invoke(userId: User) = authRepository.getUser("")
}
