package com.app.pustakam.data.localdb.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class AndroidAppPreferences(context: Context,
    override val dataStore: DataStore<Preferences>
    = createDataStore(context)) : BasePreferences(dataStore)