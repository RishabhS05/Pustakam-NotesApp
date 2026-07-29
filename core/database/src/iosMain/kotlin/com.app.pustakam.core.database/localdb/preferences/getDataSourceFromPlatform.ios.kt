package com.app.pustakam.core.database.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pustakam.koinDI.Dispatcher
import com.app.pustakam.koinDI.provideDispatcher

import org.koin.dsl.module

actual fun getDataSourceFromPlatForm()= module {
    single<DataStore<Preferences>> { createDataStore() }
    factory <Dispatcher> { provideDispatcher() }
}
