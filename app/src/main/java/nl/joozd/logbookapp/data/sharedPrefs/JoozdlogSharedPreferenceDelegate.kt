package nl.joozd.logbookapp.data.sharedPrefs

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/*
 * NOTE setting is async, so if you set it and immediately read it, it might not have changed.
 * Example: {
 *  Prefs.someValue(false)
 *  f(Prefs.someValue())
 *  } // will probably not give the updated value.
 *
 * If you want that, use setValue(x), valueBlocking = x, or give the data you are setting to the function that needs it.
 *  eg. x = false; Prefs.someValue(x), f(x)
 */
class JoozdlogSharedPreferenceDelegate<T : Any>(
    private val key: String,
    private val defaultValue: T
) : ReadOnlyProperty<JoozdLogPreferences, JoozdlogSharedPreferenceDelegate.ReadOnlyPref<T>> {
    private var _instance : Pref<T>? = null

    /**
     * This is the standard Pref yo get. I can be read (see [ReadOnlyPref]) and written to (see [WriteablePref])
     * It can also be mapped, see [DualMappedPref]. A mapped pref will still be a [Pref] for the outside world.
     */
    interface Pref<T: Any>: ReadOnlyPref<T>, WriteablePref<T>{
        fun <R: Any> mapBothWays(transformer: PrefTransformer<T, R>) =
            DualMappedPref(this, transformer)
    }

    interface ReadOnlyPref<T>{
        val flow: Flow<T>
        suspend operator fun invoke(): T
        fun <R> map(transform: (T) -> R): ReadOnlyPref<R> =
            MappedPref(this, transform)
    }

    interface WriteablePref<T: Any>{
        var valueBlocking: T
        suspend fun setValue(value: T) // difference with invoke is that caller can make this blocking or choose context
        operator fun invoke(newValue: T)
    }

    class PrefImpl<T: Any>(thisRef: JoozdLogPreferences, key: String, private val defaultValue: T): Pref<T>{
        @Suppress("UNCHECKED_CAST")
        private val prefsKey = generatePreferencesKey(key, defaultValue) as Preferences.Key<T>
        private val dataStore = thisRef.dataStore

        override var valueBlocking: T
            get() = readBlocking()
            set(value) = writeBlocking(value)

        override val flow get() = dataStore.data.map { p ->
            p[prefsKey] ?: defaultValue
        }.distinctUntilChanged()

        private suspend fun value() = flow.first()

        override suspend fun setValue(value: T) { // doing this on MainScope is OK as Datastore will give it Dispatchers.IO
            dataStore.edit { p ->
                p[prefsKey] = value
            }
        }

        override suspend operator fun invoke() = value()
        override operator fun invoke(newValue: T) {
            MainScope().launch { setValue(newValue) }
        }

        private fun readBlocking(): T =
            runBlocking {
                (dataStore.data.first()[prefsKey] ?: defaultValue)
            }

        private fun writeBlocking(value: T): Unit = runBlocking {
                setValue(value)
            }
    }

    class MappedPref<T, R>(private val source: ReadOnlyPref<T>, private val transform: (T) -> R): ReadOnlyPref<R>{
        override val flow: Flow<R>
            get() = source.flow.map{ transform(it) }

        override suspend fun invoke(): R =
            transform(source())
    }

    /**
     * A Pref can be mapped to automatically provide the data in the form needed, such as transforming an Int to an enum class.
     * Do do this, provide a [PrefTransformer] to [Pref.map].
     * @see PrefTransformer
     */
    class DualMappedPref<T: Any, R: Any>(
        private val source: Pref<T>,
        private val transformer: PrefTransformer<T, R>
    ): Pref<R>{
        override val flow: Flow<R>
            get() = source.flow.map{ transformer.map(it) }



        override var valueBlocking: R
            get() = transformer.map(source.valueBlocking)
            set(value) { source.valueBlocking = transformer.mapBack(value) }

        override suspend fun setValue(value: R) {
            source.setValue(transformer.mapBack(value))
        }

        override suspend fun invoke(): R =
            transformer.map(source())

        override fun invoke(newValue: R) {
            source(transformer.mapBack(newValue))
        }
    }


    /**
     * [map]: The function that maps the stored data to the wanted output
     * [mapBack]: The function that changes the given input to the type stored by this [DualMappedPref]
     */
    interface PrefTransformer<T: Any, R: Any>{
        fun map(source: T): R
        fun mapBack(transformedValue: R): T
    }

    override fun getValue(thisRef: JoozdLogPreferences, property: KProperty<*>): Pref<T> = getInstance(thisRef)

    private fun getInstance(thisRef: JoozdLogPreferences) = _instance ?: PrefImpl(thisRef, key, defaultValue)
}