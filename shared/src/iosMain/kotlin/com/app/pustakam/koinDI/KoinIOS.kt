package com.app.pustakam.koinDI

import com.app.pustakam.koinDI.Modules.appModule
import com.app.pustakam.koinDI.Modules.repositoriesModule
import org.koin.core.context.startKoin

object KoinIOS {
    fun startApp() = startKoin{
        modules(
            appModule,
            repositoriesModule,
        )
    }
}