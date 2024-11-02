package com.app.pustakam.android.koinDI

import com.app.pustakam.android.screen.signup.SignUseCase
import com.app.pustakam.data.localdb.preferences.AndroidAppPreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.domain.repositories.BaseRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
object Modules {
    val appModule = module {
        single <IAppPreferences>{ AndroidAppPreferences(androidApplication()) }
    }
    val repositoriesModule = module {
        single<BaseRepository> { BaseRepository(userPrefs = get()) }
    }
    val coreModule = module {
        factory <SignUseCase>{ SignUseCase() }
    }

    val viewModelsModule = module {

    }
}