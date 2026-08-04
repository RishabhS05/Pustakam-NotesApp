package com.app.pustakam.feature.auth.domain.usecase

class LogoutUseCase : AuthBaseUseCase() {
    suspend operator fun invoke() = authRepository.userLogout()
}
