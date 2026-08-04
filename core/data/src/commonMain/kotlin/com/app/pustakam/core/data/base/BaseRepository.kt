package com.app.pustakam.core.data.base

import com.app.pustakam.core.database.localdb.database.NotesDao
import com.app.pustakam.core.database.localdb.preferences.BasePreferences
import com.app.pustakam.core.network.ApiCallClient
import com.app.pustakam.core.user.UserSession
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

abstract class BaseRepository : KoinComponent {
    protected val apiClient by inject<ApiCallClient>()
    protected val notesDao by inject<NotesDao>()
    protected val userPrefs = get<BasePreferences>()
    protected val session by inject<UserSession>()
}
