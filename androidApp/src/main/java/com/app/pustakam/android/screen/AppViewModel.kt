package com.app.pustakam.android.screen

import androidx.lifecycle.viewModelScope
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel : BaseViewModel() {
   private val appUserCase = AppUserCase()
    private val _appState = MutableStateFlow(BaseUIState(isLoading = false))
    val appUIState: StateFlow<BaseUIState> = _appState.asStateFlow()
 fun updateUserAuth(){
    viewModelScope.launch {
        val token =  appUserCase.getToken()
        val userId  = appUserCase.getUserId()
        _appState.update {
            print("userId: $userId : \n token: $token")
            BaseUIState(token = token, isLoading = false, userId = userId )
        }
    }
}
    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {}

    override fun onFailure(taskCode: TaskCode, error: Error) {

    }
    override fun clearError() {}

}