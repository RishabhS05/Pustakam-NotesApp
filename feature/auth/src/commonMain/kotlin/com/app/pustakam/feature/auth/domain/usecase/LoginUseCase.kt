package com.app.pustakam.feature.auth.domain.usecase

import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.Login
import com.app.pustakam.core.model.models.response.User
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.core.usecases.BaseUseCase
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

class LoginUseCase : BaseUseCase(){
    suspend operator fun invoke(login : Login): Flow<Result<BaseResponse<User>, Error>> =
        getBaseApiCall{ repository.loginUser(login = login)}
}