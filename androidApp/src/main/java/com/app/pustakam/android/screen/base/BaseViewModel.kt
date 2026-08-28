package com.app.pustakam.android.screen.base

import com.app.pustakam.core.common.config.AuthConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.core.common.coroutines.Dispatcher
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlin.coroutines.CoroutineContext


abstract class BaseViewModel : ViewModel() , KoinComponent {

    private val logoutUseCase by inject<LogoutUseCase>()

    abstract fun onSuccess (taskCode: TaskCode, result: Result.Success<BaseResponse<*>> )
    open fun onFailure (taskCode : TaskCode, error : Error){
        // 🔐 20-Aug-2026 sync: SESSION_EXPIRED only. A bare 401 is now retried once with a
        //   refreshed token inside BaseClient, so logging out on it would end a recoverable session.
        if (error is NetworkError && error == NetworkError.SESSION_EXPIRED && !AuthConfig.BYPASS_AUTH) {
           logout()
        }
    }

    suspend fun logoutUserForcefully() = logoutUseCase()

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

     fun <T> makeAPIWish(
        showLoader : Boolean = true,
         onLoading : () -> Unit = {},
         onFailure : suspend (Error) -> Unit = {},
         call: () -> Flow<Result<BaseResponse<T>, Error>>,
         onSuccess :suspend (Result.Success<BaseResponse<*>>)  -> Unit,
    ){
        viewModelScope.launch(Dispatchers.IO) {
            call().collect{
                when (it){
                    is Result.Success -> onSuccess(it)
                    is Result.Error -> onFailure(it.error)
                    is Result.Loading -> if (showLoader) onLoading()
                }
            }
        }
    }
}
/** call without taskCode and collect and update data models*/
fun <T>Flow<Result<BaseResponse<T>, Error>>.apiWithCollect(
     showLoader : Boolean = true,
     scope: CoroutineScope,
      onLoading : () -> Unit = {},
      onFailure : suspend (Error) -> Unit,
      onSuccess :suspend (Result.Success<BaseResponse<*>>)  -> Unit, )=
     scope.launch {
    this@apiWithCollect.flowOn(Dispatchers.IO).collect {
        when (it){
            is Result.Success -> onSuccess(it)
            is Result.Error ->  onFailure(it.error)
            is Result.Loading -> if (showLoader) onLoading()
        }
    }
}