package com.app.pustakam.feature.auth.domain.usecase

import com.app.pustakam.core.data.usecases.BaseUseCase
import com.app.pustakam.feature.auth.domain.repository.IAuthRepository
import org.koin.core.component.inject


abstract class AuthBaseUseCase : BaseUseCase() {
    protected val authRepository: IAuthRepository by inject()
    suspend fun logoutUser() = authRepository.userLogout()
}
