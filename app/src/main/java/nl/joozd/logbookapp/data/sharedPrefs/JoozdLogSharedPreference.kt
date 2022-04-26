/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.data.sharedPrefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

/**
 * Use a var as a SharedPreference.
 * @param dataStore: SharedPreferences to use
 * @param defaultValue: Default value to return. Needs to be used to set type of variable to set
 * @Note reading this value is a blocking IO operation.
 */
class JoozdLogSharedPreference<T : Any>(private val dataStore: DataStore<Preferences>, private val defaultValue: T){
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
        MainScope().launch {
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

