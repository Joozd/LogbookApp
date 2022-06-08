package nl.joozd.logbookapp.data.sharedPrefs

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class JoozdlogSharedPreferenceDelegate<T : Any>(private val key: String, private val defaultValue: T): ReadOnlyProperty<JoozdLogPreferences, JoozdlogSharedPreferenceDelegate.Pref<T>> {
    private var _instance : Pref<T>? = null


    class Pref<T: Any>(thisRef: JoozdLogPreferences, key: String, private val defaultValue: T){
        @Suppress("UNCHECKED_CAST")
        private val prefsKey = generatePreferencesKey(key, defaultValue) as Preferences.Key<T>
        private val dataStore = thisRef.dataStore

        var valueBlocking: T
            get() = readBlocking()
            set(value) = writeBlocking(value)

        val flow: Flow<T> get() = dataStore.data.map { p ->
            p[prefsKey] ?: defaultValue
        }

        suspend fun value(): T = flow.first()

        fun postValue(value: T) = MainScope().launch {
            setValue(value)
        }

        suspend fun setValue(value: T) { // doing this on MainScope is OK as Datastore will give it Dispatchers.IO
            dataStore.edit { p ->
                p[prefsKey] = value
            }
        }


        suspend operator fun invoke() = value()
        suspend operator fun invoke(newValue: T) = setValue(newValue)

        private fun readBlocking(): T =
            runBlocking {
                (dataStore.data.first()[prefsKey] ?: defaultValue)
            }

        private fun writeBlocking(value: T): Unit = runBlocking {
                setValue(value)
            }
    }

    override fun getValue(thisRef: JoozdLogPreferences, property: KProperty<*>): Pref<T> = getInstance(thisRef)

    private fun getInstance(thisRef: JoozdLogPreferences) = _instance ?: Pref(thisRef, key, defaultValue)
}