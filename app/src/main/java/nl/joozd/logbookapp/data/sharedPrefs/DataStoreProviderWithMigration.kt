package nl.joozd.logbookapp.data.sharedPrefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

class DataStoreProviderWithMigration(private val context: Context, private val key: String): DataStoreProvider {
    override val dataStore: DataStore<Preferences>
        get() = context.ds
    private val Context.ds by preferencesDataStore(
        name = key,
        produceMigrations = { context ->
            // Since we're migrating from SharedPreferences, add a migration based on the
            // SharedPreferences name
            listOf(SharedPreferencesMigration(context, key))
        }
    )
}