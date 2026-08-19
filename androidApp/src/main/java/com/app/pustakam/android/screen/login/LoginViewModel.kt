package com.app.pustakam.android.screen.login

import com.app.pustakam.core.common.util.displayMessage
import com.app.pustakam.android.screen.AUTH
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.android.screen.LoginUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.request.Login
import com.app.pustakam.feature.auth.domain.usecase.LoginUseCase
import com.app.pustakam.core.common.util.NetworkError
import com.app.pustakam.core.common.util.ValidationError
import com.app.pustakam.core.model.validation.checkLoginEmailPasswordValidity
import com.app.pustakam.core.common.extensions.isValidEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.common.util.onSuccess


class LoginViewModel : BaseViewModel() {
 private val loginUseCase = LoginUseCase()
    private val _loginUiState = MutableStateFlow(LoginUIState(isLoading = false ))
    val loginUIState: StateFlow<LoginUIState> = _loginUiState.asStateFlow()
    fun login(email : String, password :String ){
        // 🔧 19-Aug-2026 — field carries either an email or a phone number; route it accordingly
        val req = if (email.isValidEmail()) Login(email = email, password = password)
                  else Login(email = null, phone = email, password = password)
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
            it.copy(error = error.displayMessage(), isLoading = false)
        }
    }

}