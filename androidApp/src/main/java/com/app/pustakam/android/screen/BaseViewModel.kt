package com.app.pustakam.android.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.data.models.BaseResponse
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.util.Result
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent


abstract class BaseViewModel : ViewModel() , KoinComponent {
    abstract fun onSuccess (taskCode: TaskCode, result: Result.Success<BaseResponse<*>> )
    open fun onFailure (taskCode : TaskCode, error : Error){
        when(error as NetworkError){
            NetworkError.UNAUTHORIZED -> viewModelScope.launch { logoutUserForcefully() }
            else -> {}
        }
    }
    abstract suspend  fun logoutUserForcefully()
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