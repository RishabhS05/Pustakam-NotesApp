package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.preferences.getDataSourceFromPlatForm
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(getDataSourceFromPlatForm(), repositoriesModules())
}


