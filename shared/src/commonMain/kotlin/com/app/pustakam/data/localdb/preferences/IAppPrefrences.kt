package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

 interface IAppPreferences {
    val dataStore : DataStore<Preferences>
    suspend fun setToken(token : String)
    suspend fun getAuthToken() : String?
    suspend fun getUserId() : String?
    suspend  fun setUserId(userId : String)
    suspend fun  clear()
}