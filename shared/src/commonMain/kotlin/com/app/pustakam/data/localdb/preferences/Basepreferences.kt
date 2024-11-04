package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.app.pustakam.data.localdb.preferences.BasePreferences.PreferencesKeys.TOKEN
import com.app.pustakam.data.localdb.preferences.BasePreferences.PreferencesKeys.USER_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

data class UserPreference(val token: String? = null, val userId: String? = null)
open class BasePreferences(override val dataStore: DataStore<Preferences>) : IAppPreferences {
    object PreferencesKeys {
        val USER_ID = stringPreferencesKey("userId")
        val TOKEN = stringPreferencesKey("token")
    }

    suspend fun fetchInitialPreferences() =
        mapAppPreferences(dataStore.data.first().toPreferences())

    val userPreferencesFlow: Flow<UserPreference> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
//                 { "Error reading preferences: $exception" }
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapAppPreferences(preferences)
        }

    override suspend fun setToken(token: String) {
        dataStore.edit {
            it[TOKEN] = token
        }
    }

    override suspend fun setUserId(userId: String) {
        dataStore.edit {
            it[USER_ID] = userId
        }
    }

    override suspend fun getAuthToken(): String? {
        return dataStore.data.map { it[TOKEN] }.firstOrNull()
    }

    override suspend fun getUserId(): String? {
        return dataStore.data.map { it[USER_ID] }.firstOrNull()
    }

    override suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }

    private fun mapAppPreferences(preferences: Preferences): UserPreference {
//        val lastScreen = preferences[PreferencesKeys.LAST_ONBOARDING_SCREEN] ?: 0
//        val isOnBoardingComplete: Boolean = (lastScreen >= 1)
//        val isSignedOut = preferences[PreferencesKeys.IS_SIGNED_OUT] ?: false
//        val showNotifications = preferences[PreferencesKeys.NOTIFICATIONS_DENIED] ?: false
        val userId = preferences[USER_ID] ?: ""
        val token = preferences[TOKEN] ?: ""
        return UserPreference(userId = userId, token = token)
    }
}