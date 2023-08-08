package nl.joozd.logbookapp.data.sharedPrefs.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

interface DataStoreProvider{
    val dataStore: DataStore<Preferences>
}