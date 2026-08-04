package com.app.pustakam.android.screen

import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.feature.auth.domain.usecase.AppUserCase
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

class AppViewModel : BaseViewModel() {
    private val appUserCase by inject<AppUserCase>()
    val authState = appUserCase.authState

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {}

    override fun onFailure(taskCode: TaskCode, error: Error) {
        super.onFailure(taskCode, error)
    }


    override fun clearError() {}
}
