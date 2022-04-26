package nl.joozd.logbookapp.data.sharedPrefs

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.joozd.logbookapp.core.App
import kotlin.reflect.KProperty

/**
 * define preferences as follows:
 * var somePreference: Boolean by JoozdLogSharedPreference(dataStore, true)
 * val somePreferenceFlow by PrefsFlow(somePreference)
 * @NOTE The Flow MUST have the same name as the property plus the word "Flow" or it won't work.
 */
abstract class JoozdLogPreferences {
    protected abstract val preferencesFileKey: String

    protected open val needsMigration = false

    protected val context: Context get () = App.instance

    //Initialized in a helper class because initializing it in this class would use uninitialized FILE_KEY.
    protected val dataStore by lazy {
        if (needsMigration)
            DataStoreProviderWithMigration(context, preferencesFileKey).dataStore
        else DataStoreProviderNoMigration(context,preferencesFileKey).dataStore
    }

    protected fun getBooleanFlowForItem(itemName: String, defaultValue: Boolean? = null): Flow<Boolean?> = dataStore.data.map { p ->
        println("Getting value for $itemName: ${p[booleanPreferencesKey(itemName)]}")
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

    //[preference] is just in here for type checking, the actual linking is through the properties name!
    protected inner class PrefsFlow<T: Any>(private val preference: T, private val defaultValue: T? = null) {
        operator fun getValue(thisRef: JoozdLogPreferences, property: KProperty<*>): Flow<T> {
            val name = property.name.dropLast(4)
            println("getting flow for $name}")

            @Suppress("UNCHECKED_CAST")
            return when (preference) {
                is Boolean -> getBooleanFlowForItem(name,defaultValue as Boolean?) as Flow<T>
                is Int -> getIntFlowForItem(name, defaultValue as Int?) as Flow<T>
                is Long -> getLongFlowForItem(name, defaultValue as Long?) as Flow<T>
                is Float -> getFloatFlowForItem(name, defaultValue as Float?) as Flow<T>
                is String -> getStringFlowForItem(name, defaultValue as String?) as Flow<T>
                else -> throw IllegalArgumentException("prefsFlow Only accepts Boolean/Int/Long/Float/String, got ${preference::class.simpleName}")
            }
        }
    }

    /**
     * Use a var as a SharedPreference.
     * @param defaultValue: Default value to return. Needs to be used to set type of variable to set
     * @Note reading this value is a blocking IO operation.
     */
    protected inner class JoozdLogSharedPreference<T : Any>(private val defaultValue: T){
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return getPreference(property.name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            setPreference(property.name, value)
        }

        private fun getPreference(key: String, defaultValue: T): T {
            @Suppress("UNCHECKED_CAST")
            val prefsKey = generatePreferencesKey(key, defaultValue) as Preferences.Key<T>
            return readBlocking(prefsKey, defaultValue)
        }

        private fun setPreference(key: String, value: T) {
            @Suppress("UNCHECKED_CAST")
            val prefsKey = generatePreferencesKey(key, defaultValue) as Preferences.Key<T>
            MainScope().launch { // doing this on MainScope is OK as Datastore will give it Dispatchers.IO
                dataStore.edit { p ->
                    p[prefsKey] = value
                }
            }
        }


        private fun generatePreferencesKey(key: String, defaultValue: T) =
            when(defaultValue){
                is Boolean -> booleanPreferencesKey(key)
                is Int -> intPreferencesKey(key)
                is Long -> longPreferencesKey(key)
                is Float -> floatPreferencesKey(key)
                is String -> stringPreferencesKey(key)
                else -> throw IllegalArgumentException()
            }

        private fun readBlocking(key: Preferences.Key<T>, defaultValue: T): T =
            runBlocking {
                @Suppress("UNCHECKED_CAST")
                (dataStore.data.first()[key] ?: defaultValue)
            }

    }
}