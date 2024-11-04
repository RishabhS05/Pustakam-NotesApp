package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.preferences.getDataSourceFromPlatForm
import org.koin.core.context.startKoin

fun initKoin() = startKoin {
     modules(getDataSourceFromPlatForm(), repositoriesModules())
 }