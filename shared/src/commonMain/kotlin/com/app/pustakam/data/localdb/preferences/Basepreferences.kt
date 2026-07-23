package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.app.pustakam.data.localdb.preferences.BasePreferences.PreferencesKeys.IS_USER_AUTHENTIC
import com.app.pustakam.data.localdb.preferences.BasePreferences.PreferencesKeys.TOKEN
import com.app.pustakam.data.localdb.preferences.BasePreferences.PreferencesKeys.USER_ID
import com.app.pustakam.koinDI.provideDispatcher
import com.app.pustakam.util.log_d
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class UserPreference(val token: String = "",
                          val userId: String = "",
                          val isAuthenticated : Boolean = false,
                          // 🎨 22-Jul-2026 — Granth spec §6 Appearance. Mirrors iOS ThemeManager's
                          //   persisted "granth.themeMode"; "system" follows the OS appearance.
                          val themeMode : String = "system")

open class BasePreferences(private val dataStore: DataStore<Preferences>) : IAppPreferences {

    object PreferencesKeys {
        val USER_ID = stringPreferencesKey("userId")
        val TOKEN = stringPreferencesKey("token")
        val IS_USER_AUTHENTIC = booleanPreferencesKey("isAuthenticated")
        // 🎨 22-Jul-2026 — persisted Appearance pick (spec §6)
        val THEME_MODE = stringPreferencesKey("granth.themeMode")
        // 📖 23-Jul-2026 — reader: "page" (curl) or "scroll" (continuous). Shared by both platforms.
        val READING_MODE = stringPreferencesKey("granth.readingMode")
        // 📖 23-Jul-2026 — per-document resume USED to live here as one preference key per book.
        //   It now lives on the NoteContent row itself (MediaContent.progressPage/totalPages) so it
        //   travels with the document and survives sync. Nothing to keep here.
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
        .map { it[BasePreferences.PreferencesKeys.READING_MODE] ?: "page" }

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
        val userId = preferences[USER_ID] ?: "UserID!2333q3w3"
        val token = preferences[TOKEN] ?: ""
        val isAuthenticated = preferences[IS_USER_AUTHENTIC] ?: false
        // 🎨 22-Jul-2026 — restore Appearance pick; defaults to "system" like iOS
        val themeMode = preferences[BasePreferences.PreferencesKeys.THEME_MODE] ?: "system"
        return UserPreference(userId = userId, token = token, isAuthenticated = true,
//        isAuthenticated
            themeMode = themeMode
        )
    }
    override fun currentTokenOrNull(): String? =  userPreferenceStateFlow.value.token.ifBlank { null }
}