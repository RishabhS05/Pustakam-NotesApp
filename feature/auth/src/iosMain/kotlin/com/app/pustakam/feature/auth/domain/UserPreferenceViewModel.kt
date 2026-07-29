package com.app.pustakam.feature.auth.domain

import com.app.pustakam.core.database.localdb.preferences.UserPreference
import com.app.pustakam.core.database.localdb.preferences.BasePreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 🔧 30-Jul-2026 02:10 was KoinHelper.getPreference() (:shared) — resolved from Koin directly to keep :feature:auth below :shared
class UserPreferenceViewModel : KoinComponent {
    private val userPreference  = get<BasePreferences>()
    private val userPrefStateFlow  = userPreference.userPreferencesFlow

    fun observeUserPreference(callback: (UserPreference) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            userPrefStateFlow.collect { preference ->
                callback(preference) // Call the Swift callback with each new value
            }
        }
    }
}
