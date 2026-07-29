package com.app.pustakam.core.database.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pustakam.core.common.coroutines.Dispatcher
import com.app.pustakam.core.common.coroutines.provideDispatcher
// 🔧 30-Jul-2026 02:10 — these two were lost in the module move; module{} and androidContext() are unresolved without them
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


actual fun getDataSourceFromPlatForm() = module {
    single<DataStore<Preferences>> {
        createDataStore {
            androidContext().filesDir?.resolve(dataStoreFileName)?.absolutePath
                ?: throw Exception("Couldn't get Android Datastore context.")
        }
    }
    factory <Dispatcher> { provideDispatcher() }
}