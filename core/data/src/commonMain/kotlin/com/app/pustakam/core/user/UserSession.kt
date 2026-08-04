package com.app.pustakam.core.user

import com.app.pustakam.core.common.coroutines.provideDispatcher
import com.app.pustakam.core.database.localdb.preferences.BasePreferences
import com.app.pustakam.core.database.localdb.preferences.UserPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserSession(prefsStore: BasePreferences) {

    private val scope = CoroutineScope(SupervisorJob() + provideDispatcher().io)
    private val _state = MutableStateFlow(UserPreference())

    val state: StateFlow<UserPreference> = _state.asStateFlow()
    val userId: String get() = _state.value.userId

    val isAuthenticated: Boolean get() = _state.value.isAuthenticated

    init {
        scope.launch { prefsStore.userPreferencesFlow.collect { _state.value = it } }
    }
}
