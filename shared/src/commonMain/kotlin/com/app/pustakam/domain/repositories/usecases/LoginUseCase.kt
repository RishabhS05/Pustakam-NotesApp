package com.app.pustakam.domain.repositories.usecases

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.response.User
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.flow.Flow

class LoginUseCase : BaseUseCase(){
    suspend operator fun invoke(login : Login): Flow<Result<BaseResponse<User>, Error>> =
        getBaseApiCall{ repository.loginUser(login = login)}
}