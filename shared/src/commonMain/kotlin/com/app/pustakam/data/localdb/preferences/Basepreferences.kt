package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

open class BasePreferences(override val dataStore: DataStore<Preferences>) : IAppPreferences{
    protected val USER_ID = stringPreferencesKey("userId")
    protected val TOKEN = stringPreferencesKey("token")
    override suspend  fun setToken(token: String) {
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



}