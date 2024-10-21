package com.app.pustakam.android.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.domain.repositories.BaseRepository
import kotlinx.coroutines.flow.Flow
import com.app.pustakam.util.Result
import com.app.pustakam.util.Error
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    val baseRepository : BaseRepository = BaseRepository()
    abstract fun onSuccess (taskCode: TaskCode, result: Result.Success<BaseResponse<*>> )
    abstract fun onFailure (taskCode : TaskCode, error : Error)
    abstract fun clearError()
    open fun onLoading(taskCode : TaskCode) {
        println("Loading .....")
    }
    fun <T> makeAWish(
        taskCode : TaskCode,
        showLoader : Boolean = true,
        call: suspend () -> Flow<Result<BaseResponse<T>, Error>>
    ){
        viewModelScope.launch {
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