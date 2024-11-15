package com.app.pustakam.android.screen.login

import com.app.pustakam.android.screen.AUTH
import com.app.pustakam.android.screen.BaseViewModel
import com.app.pustakam.android.screen.LoginUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.ValidationError
import com.app.pustakam.util.checkLoginEmailPasswordValidity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class LoginViewModel : BaseViewModel() {
 private val loginUseCase = LoginUseCase()
    private val _loginUiState = MutableStateFlow(LoginUIState(isLoading = false ))
    val loginUIState: StateFlow<LoginUIState> = _loginUiState.asStateFlow()
    fun login(email : String, password :String ){
        val req = Login(email = email,password= password)
        val isValidationCred = checkLoginEmailPasswordValidity(req = req)
        if( isValidationCred == ValidationError.NONE)
             makeAWish(AUTH.LOGIN, showLoader = true, call = {
                    loginUseCase.invoke(login = req) },
                 )
        else {
            _loginUiState.update { it.copy(isLogging = false , isLoading = false ,
                error = isValidationCred.getError()) }
        }
    }
    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        val response = result.data as BaseResponse
       when (taskCode){
           AUTH.LOGIN -> {
          _loginUiState.update{
              it.copy(isLogging =  response.isSuccessful,
                  error = null,
                  successMessage = null,
                  isLoading = false)
          }
           }
       }
    }
    override fun clearError(){
        _loginUiState.update {it.copy(error= null) }
    }

    override fun onLoading(taskCode: TaskCode) {
        _loginUiState.update {
            it.copy(isLoading = true)
        }
    }
    override fun onFailure(taskCode: TaskCode, error: Error) {
        super.onFailure(taskCode, error)
        _loginUiState.update{
            it.copy(error = (error as NetworkError).getError(), isLoading = false)
        }
    }

    override suspend fun logoutUserForcefully() {
        loginUseCase.logoutUser()
    }
}