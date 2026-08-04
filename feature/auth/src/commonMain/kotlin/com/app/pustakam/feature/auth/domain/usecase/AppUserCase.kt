package com.app.pustakam.feature.auth.domain.usecase

import com.app.pustakam.core.common.config.AuthConfig
import com.app.pustakam.core.database.localdb.preferences.UserPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map


class AppUserCase : AuthBaseUseCase() {
    val authState: StateFlow<UserPreference> get() = authRepository.authState

    val isAuthenticated: Flow<Boolean>
        get() = authRepository.authState.map { AuthConfig.BYPASS_AUTH || it.isAuthenticated }
}
