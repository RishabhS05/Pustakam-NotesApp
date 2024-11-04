package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.android.ext.koin.androidContext

import org.koin.dsl.module

actual fun getDataSourceFromPlatForm() = module {
    single<DataStore<Preferences>>  {
        createDataStore {
            androidContext().filesDir?.resolve(dataStoreFileName)?.absolutePath
                ?: throw Exception("Couldn't get Android Datastore context.")
        }
    }
single<IAppPreferences> { BasePreferences(get()) }
}