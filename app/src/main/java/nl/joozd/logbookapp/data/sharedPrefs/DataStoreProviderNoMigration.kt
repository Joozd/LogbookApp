package nl.joozd.logbookapp.data.sharedPrefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

class DataStoreProviderNoMigration(private val context: Context, key: String): DataStoreProvider {
    override val dataStore: DataStore<Preferences>
        get() = context.ds
    private val Context.ds by preferencesDataStore(name = key)
}