package com.app.pustakam.core.database.localdb.preferences

import com.app.pustakam.core.common.config.AuthConfig
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.app.pustakam.core.database.localdb.preferences.BasePreferences.PreferencesKeys.IS_USER_AUTHENTIC
import com.app.pustakam.core.database.localdb.preferences.BasePreferences.PreferencesKeys.TOKEN
import com.app.pustakam.core.database.localdb.preferences.BasePreferences.PreferencesKeys.USER_ID
import com.app.pustakam.core.common.coroutines.provideDispatcher
import com.app.pustakam.core.common.util.log_d
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


data class UserPreference(val token: String = "",
                          val userId: String = "",
                          val isAuthenticated : Boolean = false,
                          val themeMode : String = "system")

open class BasePreferences(private val dataStore: DataStore<Preferences>) : IAppPreferences {

    object PreferencesKeys {
        val USER_ID = stringPreferencesKey("userId")
        val TOKEN = stringPreferencesKey("token")
        val IS_USER_AUTHENTIC = booleanPreferencesKey("isAuthenticated")
        val THEME_MODE = stringPreferencesKey("granth.themeMode")
        val READING_MODE = stringPreferencesKey("granth.readingMode")
    }

    val userPreferencesFlow: Flow<UserPreference> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                log_d("Prefesrnce",exception.message.toString())
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapAppPreferences(preferences).apply {
                println(this)
            }
        }
    val userPreferenceStateFlow =
        userPreferencesFlow.stateIn(scope = CoroutineScope(provideDispatcher().io),
            initialValue = UserPreference(),
            started =  SharingStarted.WhileSubscribed())


    suspend fun fetchInitialPreferences() =
        mapAppPreferences(dataStore.data.first().toPreferences())

    override suspend fun setUserId(userId: String) {
        dataStore.edit {
            it[USER_ID] = userId
        }
    }
    override suspend fun setToken(token: String) {
        dataStore.edit {
            it[TOKEN] = token
        }
    }
    override suspend fun setAuth(isAuth: Boolean) {
        dataStore.edit {
            it[IS_USER_AUTHENTIC] = isAuth
        }
    }
    // 📖 23-Jul-2026 — reader preferences. Reading mode is global (one choice for the app);
    //   the resume page is stored per book so each document reopens where it was left.
    override suspend fun setReadingMode(mode: String) {
        dataStore.edit {
            it[PreferencesKeys.READING_MODE] = mode
        }
    }

    val readingModeFlow: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.READING_MODE] ?: "page" }

    // 🎨 22-Jul-2026 — persist the Appearance tile pick (spec §6)
    override suspend fun setThemeMode(mode: String) {
        dataStore.edit {
            it[PreferencesKeys.THEME_MODE] = mode
        }
    }
    override suspend fun getAuthToken(): String? {
        return dataStore.data.map { it[TOKEN] }.firstOrNull()
    }
    override suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }
    private fun mapAppPreferences(preferences: Preferences): UserPreference {
        val storedUserId = preferences[USER_ID] ?: ""
        val token = preferences[TOKEN] ?: ""
        val storedAuth = preferences[IS_USER_AUTHENTIC] ?: false
        val themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system"
        return UserPreference(
            userId = if (AuthConfig.BYPASS_AUTH) storedUserId.ifBlank { AuthConfig.LOCAL_USER_ID } else storedUserId,
            token = token,
            isAuthenticated = if (AuthConfig.BYPASS_AUTH) true else storedAuth,
            themeMode = themeMode
        )
    }
    override fun currentTokenOrNull(): String? =  userPreferenceStateFlow.value.token.ifBlank { null }
}