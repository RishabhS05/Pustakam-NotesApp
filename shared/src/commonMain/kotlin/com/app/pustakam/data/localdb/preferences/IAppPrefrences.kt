package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.plugins.auth.Auth

interface IAppPreferences {
    suspend fun setToken(token : String)
    suspend fun getAuthToken() : String?
    suspend  fun setUserId(userId : String)
    suspend  fun setAuth(isAuth : Boolean)
    suspend fun  clear()
}