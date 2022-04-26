package nl.joozd.logbookapp.data.sharedPrefs

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.App

abstract class JoozdLogPreferences {
    protected abstract val preferencesFileKey: String

    protected open val needsMigration = false

    protected val context: Context = App.instance

    //Initialized in a helper class because initializing it in this class would use uninitialized FILE_KEY.
    protected val dataStore by lazy {
        if (needsMigration)
            DataStoreProviderWithMigration(context, preferencesFileKey).dataStore
        else DataStoreProviderNoMigration(context,preferencesFileKey).dataStore
    }

    protected fun getBooleanFlowForItem(itemName: String, defaultValue: Boolean? = null): Flow<Boolean?> = dataStore.data.map { p ->
        p[booleanPreferencesKey(itemName)] ?: defaultValue
    }

    protected fun getIntFlowForItem(itemName: String, defaultValue: Int? = null): Flow<Int?> = dataStore.data.map { p ->
        p[intPreferencesKey(itemName)] ?: defaultValue
    }

    protected fun getLongFlowForItem(itemName: String, defaultValue: Long? = null): Flow<Long?> = dataStore.data.map { p ->
        p[longPreferencesKey(itemName)] ?: defaultValue
    }

    protected fun getFloatFlowForItem(itemName: String, defaultValue: Float? = null): Flow<Float?> = dataStore.data.map { p ->
        p[floatPreferencesKey(itemName)] ?: defaultValue
    }

    protected fun getStringFlowForItem(itemName: String, defaultValue: String? = null): Flow<String?> = dataStore.data.map { p ->
        p[stringPreferencesKey(itemName)] ?: defaultValue
    }
}