package com.app.pustakam.android

import android.app.Application
import com.app.pustakam.android.koinDI.Modules.appModule
import com.app.pustakam.android.koinDI.Modules.coreModule
import com.app.pustakam.android.koinDI.Modules.repositoriesModule
import com.app.pustakam.android.koinDI.Modules.viewModelsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

import org.koin.core.context.startKoin

class PustakmApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        startKoin{
            androidLogger()
            androidContext(this@PustakmApplication)
            modules(
                appModule,
                repositoriesModule,
                coreModule,
                viewModelsModule
            )
        }
    }
}