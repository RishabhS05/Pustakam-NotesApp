package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.localdb.preferences.IosAppPreferences
import com.app.pustakam.domain.repositories.BaseRepository
import org.koin.dsl.module

object Modules {
    val appModule = module(true) {
        single <IAppPreferences>{ IosAppPreferences() }
    }
    val repositoriesModule = module(true ) {
        single<BaseRepository> { BaseRepository(userPrefs = get()) }
    }
}