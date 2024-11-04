package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import org.koin.dsl.module

actual fun getDataSourceFromPlatForm()= module {
    single<DataStore<Preferences>> { createDataStore() }
    single<IAppPreferences> { BasePreferences(get()) }
}