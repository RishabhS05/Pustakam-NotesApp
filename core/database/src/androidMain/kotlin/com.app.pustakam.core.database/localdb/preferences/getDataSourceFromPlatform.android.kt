package com.app.pustakam.core.database.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pustakam.data.localdb.preferences.createDataStore
import com.app.pustakam.data.localdb.preferences.dataStoreFileName


actual fun getDataSourceFromPlatForm() = module {
    single<DataStore<Preferences>> {
        createDataStore {
            androidContext().filesDir?.resolve(dataStoreFileName)?.absolutePath
                ?: throw Exception("Couldn't get Android Datastore context.")
        }
    }
    factory <Dispatcher> { provideDispatcher() }
}