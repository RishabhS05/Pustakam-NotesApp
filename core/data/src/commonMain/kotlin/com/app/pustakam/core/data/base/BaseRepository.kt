package com.app.pustakam.core.data.base

import com.app.pustakam.data.localdb.database.NotesDao
import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.UserPreference
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.network.ApiCallClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class BaseRepository : IRemoteRepository, ILocalRepository, KoinComponent {
    protected val apiClient by inject<ApiCallClient>()
    protected val notesDao by inject<NotesDao>()
    protected val userPrefs = get<BasePreferences>()
    val _userAuthState = (userPrefs).userPreferencesFlow
    // 🔧 AUTH-FIX: was `lateinit` populated by an async collector — any API call made
    //             before the first emission crashed with UninitializedPropertyAccessException.
    //             Defaults are safe: UserPreference("", "", false).
    protected var prefs: UserPreference = UserPreference()
    init {
        CoroutineScope(Dispatchers.IO).launch {
            _userAuthState.collect { pref ->
                prefs = pref
            }
        }
    }
    override suspend fun loginUser(login: Login): Result<BaseResponse<User>, Error>
            = apiClient.login(login).onSuccess {
        it.data?._id?.let { it1 ->
            userPrefs.setUserId(it1)
            userPrefs.setAuth(true)
        }
    }

    override suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error> = apiClient.register(user)

    /** user crud apis */
    override suspend fun updateUser(user: User): Result<BaseResponse<User>, Error> = apiClient.updateUser(user)

    // pass empty string to get current user
    override suspend fun getUser(userId: String): Result<BaseResponse<User>, Error> =
         apiClient.getUser(userId.ifEmpty { prefs.userId })


    override suspend fun deleteUser(): Result<BaseResponse<User>, Error>
            = apiClient.deleteUser(prefs.userId)

    override suspend fun profileImage(): Result<BaseResponse<User>, Error>
            = apiClient.profileImage()

    override suspend fun userLogout() {
        // 🔧 AUTH-FIX: old body collected the flow forever (caller hung) and DISCARDED
        //             the copy — logout never actually cleared anything.
        userPrefs.clear()
    }
}