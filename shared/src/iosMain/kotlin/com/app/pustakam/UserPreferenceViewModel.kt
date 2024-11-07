package com.app.pustakam

import com.app.pustakam.data.localdb.preferences.UserPreference
import com.app.pustakam.koinDI.KoinHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserPreferenceViewModel {
    private val userPreference  = KoinHelper.getPreference()
    private val userPrefStateFlow  = userPreference.userPreferencesFlow

    fun observeUserPreference(callback: (UserPreference) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            userPrefStateFlow.collect { preference ->
                callback(preference) // Call the Swift callback with each new value
            }
        }
    }
}