package com.app.pustakam.android.screen

import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import org.koin.core.component.get


import org.koin.core.component.inject

class AppViewModel : BaseViewModel() {
   private val repository = get<BaseRepository>()
     val authState =  repository._userAuthState
    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {}

    override fun onFailure(taskCode: TaskCode, error: Error) {
      super.onFailure(taskCode, error)
    }

    override suspend fun logoutUserForcefully() {

    }

    override fun clearError() {}
}