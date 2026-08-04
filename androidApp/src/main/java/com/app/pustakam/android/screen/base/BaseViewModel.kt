package com.app.pustakam.android.screen.base

import com.app.pustakam.core.common.config.AuthConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.core.model.models.BaseResponse
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.NetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.feature.auth.domain.usecase.LogoutUseCase



abstract class BaseViewModel : ViewModel() , KoinComponent {

    private val logoutUseCase by inject<LogoutUseCase>()

    abstract fun onSuccess (taskCode: TaskCode, result: Result.Success<BaseResponse<*>> )
    open fun onFailure (taskCode : TaskCode, error : Error){
        if (error is NetworkError && error == NetworkError.UNAUTHORIZED && !AuthConfig.BYPASS_AUTH) {
            viewModelScope.launch { logoutUserForcefully() }
        }
    }

    open suspend fun logoutUserForcefully() = logoutUseCase()

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            logoutUserForcefully()
            onDone()
        }
    }
    abstract fun clearError()
    open fun onLoading(taskCode : TaskCode) {}
    fun <T> makeAWish(
        taskCode : TaskCode,
        showLoader : Boolean = true,
        call: suspend () -> Flow<Result<BaseResponse<T>, Error>>
    ){
        viewModelScope.launch(Dispatchers.IO) {
            call().collect{
                when (it){
                    is Result.Success -> onSuccess(taskCode, it)
                    is Result.Error -> onFailure(taskCode, it.error)
                    is Result.Loading -> if (showLoader) onLoading(taskCode)
                }
            }
        }
    }
}