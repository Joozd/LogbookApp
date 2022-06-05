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

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

/**
 * Use a var as a SharedPreference.
 * @param defaultValue: Default value to return. Needs to be used to set type of variable to set
 * @Note reading this value is a blocking IO operation.
 */
class JoozdLogSharedPreference<T : Any>(private val joozdlogSharedPreferences: JoozdLogPreferences, key: String, private val defaultValue: T){
    @Suppress("UNCHECKED_CAST")
    private val prefsKey = generatePreferencesKey(key, defaultValue) as Preferences.Key<T>

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getPreference(defaultValue)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setPreference(value)
    }

    private fun getPreference(defaultValue: T): T {
        return readBlocking(prefsKey, defaultValue)
    }

    private fun setPreference(value: T) {
        writeBlocking(prefsKey, value)
    }

    private fun readBlocking(key: Preferences.Key<T>, defaultValue: T): T =
        runBlocking {
            (joozdlogSharedPreferences.dataStore.data.first()[key] ?: defaultValue)
        }

    private fun writeBlocking(prefsKey: Preferences.Key<T>, value: T) =
        runBlocking {
            joozdlogSharedPreferences.dataStore.edit { p ->
                p[prefsKey] = value
            }
        }
}



