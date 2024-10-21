package com.app.pustakam.android.screen

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel : BaseViewModel() {
    private val _appErrorState = MutableStateFlow(BaseUIState(isLoading = false))
    val appUIState: StateFlow<BaseUIState> = _appErrorState.asStateFlow()
    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {}

    override fun onFailure(taskCode: TaskCode, error: Error) {

    }

    override fun clearError() {

    }
}