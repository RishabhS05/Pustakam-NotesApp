package com.app.pustakam.android.screen.signup

import com.app.pustakam.android.screen.AUTH
import com.app.pustakam.android.screen.BaseViewModel
import com.app.pustakam.android.screen.PROFILE
import com.app.pustakam.android.screen.SignupUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.ValidationError
import com.app.pustakam.util.checkLoginEmailPasswordValidity
import com.app.pustakam.util.checkRegisterFieldsValidity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RegisterViewModel : BaseViewModel() {
    private val useCase = SignUseCase()
    private val _signupUiState = MutableStateFlow(SignupUIState(isLoading = false))
    val signupUIState: StateFlow<SignupUIState> = _signupUiState.asStateFlow()
    fun registerUser(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        phoneNumber: String,
    ) {
        val req = RegisterReq(email = email, password = password, name = name, passwordConfirm = confirmPassword, phone = phoneNumber)
        val checkFieldsValidation = checkRegisterFieldsValidity(req = req)
        if (checkFieldsValidation == ValidationError.NONE)
            makeAWish(AUTH.SIGNUP, call = { useCase.invoke(user = req) },
        )
        else {
            _signupUiState.update {
                it.copy(
                    isRegistered = false, isLoading = false,
                    error = checkFieldsValidation.getError()
                )
            }
        }
    }

    override fun onLoading(taskCode: TaskCode) {
        _signupUiState.update {
            it.copy(isLoading = true)
        }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        val response = result.data
        when (taskCode) {
            AUTH.SIGNUP -> {
                _signupUiState.update {
                    it.copy(isRegistered = response.isSuccessful, isLoading = false)
                }
            }

            PROFILE.PROFILE_IMAGE -> {
                _signupUiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        _signupUiState.update {
            it.copy(
                error = (error as NetworkError).getError(), isLoading = false
            )
        }
    }

    override fun clearError() {
        _signupUiState.update {
            it.copy(
                error = null, isLoading = false
            )
        }
    }
}