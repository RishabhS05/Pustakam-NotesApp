package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.preferences.IosAppPreferences
import com.app.pustakam.domain.repositories.BaseRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object BaseRepositoryHelper : KoinComponent{
         val baseRepository : BaseRepository  = BaseRepository(userPrefs =  IosAppPreferences())
}