package nl.joozd.logbookapp.data.sharedPrefs.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    protected val context: Context get () = App.instance

    //Initialized lazy because initializing it immediately would use uninitialized FILE_KEY.
    val dataStore by lazy {
        DataStoreProviderNoMigration(context,preferencesFileKey).dataStore
    }

    protected fun <T: Any> post(key: String, newValue: T){
        @Suppress("UNCHECKED_CAST")
        val prefsKey = generatePreferencesKey(key, newValue) as Preferences.Key<T>
        MainScope().launch{ // doing this on MainScope is OK as Datastore will give it Dispatchers.IO
            dataStore.edit { p: MutablePreferences ->
                p[prefsKey] = newValue
            }
        }
    }

    protected fun getBooleanFlowForItem(itemName: String, defaultValue: Boolean? = null): Flow<Boolean?> = dataStore.data.map { p ->
        p[booleanPreferencesKey(itemName)] ?: defaultValue
    }



    //[preference] is just in here for type checking, the actual linking is through the properties name!
    protected inner class PrefsFlow<T: Any>(private val name: String, private val defaultValue: T) {
        operator fun getValue(thisRef: JoozdLogPreferences, property: KProperty<*>): Flow<T> {
            @Suppress("UNCHECKED_CAST")
            return when (defaultValue) {
                is Boolean -> getBooleanFlowForItem(name,defaultValue as Boolean?) as Flow<T>
                is Int -> getIntFlowForItem(name, defaultValue as Int?) as Flow<T>
                is Long -> getLongFlowForItem(name, defaultValue as Long?) as Flow<T>
                is Float -> getFloatFlowForItem(name, defaultValue as Float?) as Flow<T>
                is String -> getStringFlowForItem(name, defaultValue as String?) as Flow<T>
                else -> throw IllegalArgumentException("prefsFlow Only accepts Boolean/Int/Long/Float/String, got ${defaultValue::class.simpleName}")
            }
        }

        private fun getIntFlowForItem(itemName: String, defaultValue: Int? = null): Flow<Int?> = dataStore.data.map { p ->
            p[intPreferencesKey(itemName)] ?: defaultValue
        }

        private fun getLongFlowForItem(itemName: String, defaultValue: Long? = null): Flow<Long?> = dataStore.data.map { p ->
            p[longPreferencesKey(itemName)] ?: defaultValue
        }

        private fun getFloatFlowForItem(itemName: String, defaultValue: Float? = null): Flow<Float?> = dataStore.data.map { p ->
            p[floatPreferencesKey(itemName)] ?: defaultValue
        }

        private fun getStringFlowForItem(itemName: String, defaultValue: String? = null): Flow<String?> = dataStore.data.map { p ->
            p[stringPreferencesKey(itemName)] ?: defaultValue
        }
    }

    /**
     * Use a var as a SharedPreference.
     * @param defaultValue: Default value to return. Needs to be used to set type of variable to set
     * @Note reading this value is a blocking IO operation.
     */
    protected inner class JoozdLogSharedPreferenceNotNull<T : Any>(key: String, private val defaultValue: T){
        private var _delegate: T by JoozdLogSharedPreference(this@JoozdLogPreferences, key, defaultValue)

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = _delegate

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            _delegate = value
        }
    }
}