package com.app.pustakam.android.screen.signup

import com.app.pustakam.android.usecases.BaseUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.flow.Flow

class SignUseCase : BaseUseCase() {
    suspend operator fun invoke(user: RegisterReq): Flow<Result<BaseResponse<User>, Error>> = getBaseApiCall {
        repository.registerUser(user = user)
    }
}

class DeleteUserUseCase : BaseUseCase() {
    suspend operator fun invoke(userId: String) = repository.deleteUser("")
}

class UpdateUserUseCase : BaseUseCase() {
    suspend operator fun invoke(user: User) = repository.updateUser(user)
}

class ReadUserUseCase : BaseUseCase() {
    suspend operator fun invoke(userId: User) = repository.getUser("")
}