package com.app.pustakam.feature.auth.domain.usecase

import com.app.pustakam.core.database.localdb.preferences.UserPreference
import kotlinx.coroutines.flow.StateFlow


class AppUserCase : AuthBaseUseCase() {
    val authState: StateFlow<UserPreference> get() = authRepository.authState
}
