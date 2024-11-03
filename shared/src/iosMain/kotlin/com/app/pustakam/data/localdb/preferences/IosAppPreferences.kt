package com.app.pustakam.data.localdb.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class IosAppPreferences(override val dataStore: DataStore<Preferences> = createDataStore())
    : BasePreferences(dataStore)