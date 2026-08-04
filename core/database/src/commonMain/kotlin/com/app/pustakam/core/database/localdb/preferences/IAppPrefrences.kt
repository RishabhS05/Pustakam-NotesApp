package com.app.pustakam.core.database.localdb.preferences

import kotlinx.coroutines.flow.Flow

interface IAppPreferences {
    val userPreferencesFlow: Flow<UserPreference>
    val readingModeFlow: Flow<String>

    suspend fun setToken(token : String)
    suspend fun getAuthToken() : String?
    suspend  fun setUserId(userId : String)
    suspend  fun setAuth(isAuth : Boolean)
    // 🎨 22-Jul-2026 — Granth spec §6 Appearance: persisted theme mode (system/light/dark/amoled)
    suspend fun setThemeMode(mode : String)
    // 📖 23-Jul-2026 — reader: "page" (curl) vs "scroll" (continuous).
    //   Per-book resume is NOT here — it lives on MediaContent (progressPage/totalPages).
    suspend fun setReadingMode(mode : String)
    suspend fun  clear()
    fun currentTokenOrNull(): String?
}
