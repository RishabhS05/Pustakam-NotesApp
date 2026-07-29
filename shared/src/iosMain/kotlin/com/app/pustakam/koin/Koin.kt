package com.app.pustakam.koin

import com.app.pustakam.data.localdb.database.getDatabaseModule
import com.app.pustakam.data.localdb.preferences.getDataSourceFromPlatForm
import org.koin.core.context.startKoin

fun initKoin() = startKoin {
     modules(getDataSourceFromPlatForm(), getDatabaseModule())
 }