package com.app.pustakam.feature.auth.domain.usecase

import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.RegisterReq
import com.app.pustakam.core.model.models.response.User
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.core.usecases.BaseUseCase
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

class SignUseCase : BaseUseCase() {
    suspend operator fun invoke(user: RegisterReq): Flow<Result<BaseResponse<User>, Error>> = getBaseApiCall {
        repository.registerUser(user = user)
    }
}

class DeleteUserUseCase : BaseUseCase() {
    suspend operator fun invoke() = repository.deleteUser()
}

class UpdateUserUseCase : BaseUseCase() {
    suspend operator fun invoke(user: User) = repository.updateUser(user)
}

class ReadUserUseCase : BaseUseCase() {
    suspend operator fun invoke(userId: User) = repository.getUser("")
}