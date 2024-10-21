package com.app.pustakam.android.screen.login

import com.app.pustakam.android.usecases.BaseUseCase
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.util.Result
import com.app.pustakam.util.Error


class LoginUseCase  : BaseUseCase(){
    suspend operator fun invoke(login : Login): Flow<Result<BaseResponse<User>, Error>> = getBaseApiCall{
       repository.loginUser(login = login)
   }
}