package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.database.NotesDao
import com.app.pustakam.data.localdb.database.getDatabaseModule
import com.app.pustakam.data.localdb.preferences.getDataSourceFromPlatForm
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(getDataSourceFromPlatForm(), repositoriesModules(), getDatabaseModule(), )
}


