package com.app.pustakam.bridge

import com.app.pustakam.data.localdb.preferences.UserPreference
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.domain.repositories.usecases.LoginUseCase
import com.app.pustakam.domain.repositories.usecases.SignUseCase
import com.app.pustakam.koinDI.KoinHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// 🔧 AUTH-FIX: auth entry point for iOS — use-cases only, same pattern as NotesBridge.
//             Replaces LoginHandler/SignUpHandler's direct baseRepositary access and
//             the `as! NetworkError` casting path in Swift.
class AuthBridge : KoinComponent {

    /** Observers: cancelled by dispose() when the screen dies. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private companion object {
        // 🔧 REALTIME-FIX: login/signup/logout are WRITES — they must survive View-struct
        //   recreation and navigation-away (dispose() was able to cancel them mid-flight).
        private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    private val loginUseCase: LoginUseCase by inject()
    private val signUseCase: SignUseCase by inject()

    /** Login with email/phone + password. Repo stores userId + auth flag on success. */
    fun login(
        login: Login,
        onLoading: () -> Unit,
        onSuccess: (User?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { loginUseCase(login) }, onLoading, onSuccess, onError)

    /** Register a new user. */
    fun signup(
        request: RegisterReq,
        onLoading: () -> Unit,
        onSuccess: (User?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { signUseCase(request) }, onLoading, onSuccess, onError)

    /** Clears stored credentials (fixed userLogout underneath). */
    fun logout(onDone: () -> Unit) {
        writeScope.launch {
            loginUseCase.logoutUser()
            onDone()
        }
    }

    /** Auth state (token/userId/isAuthenticated) — drives auto-login / route guards. */
    fun observeAuthState(onChange: (UserPreference) -> Unit): Closeable =
        KoinHelper.getPreference().userPreferencesFlow.watch(scope) { onChange(it) }

    fun dispose() = scope.cancel()
}
