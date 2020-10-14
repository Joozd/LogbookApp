/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import kotlin.reflect.KProperty

/**
 * Use a var as a SharedPreference.
 * @param sharedPrefs: SharedPreferences to use
 * @param defaultValue: Default value to return. Needs to be used to set type of variable to set
 * @param liveData: LiveData to update with this
 */
class JoozdLogSharedPrefs<T>(private val sharedPrefs: SharedPreferences, private val defaultValue: T, private val liveData: MutableLiveData<T>? = null){


    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getPreference(property.name, defaultValue)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setPreference(property.name, value)
        liveData?.postValue(value)
    }

    private fun getPreference(key: String, defaultValue: T): T {
        with(sharedPrefs) {
            @Suppress("UNCHECKED_CAST")
            return when (defaultValue) {
                is Boolean -> getBoolean(key, defaultValue)
                is Int -> getInt(key, defaultValue)
                is Long -> getLong(key, defaultValue)
                is Float -> getFloat(key, defaultValue)
                is String -> getString(key, defaultValue)
                else -> throw IllegalArgumentException()
            } as T
        }
    }

    private fun setPreference(key: String, value: T) {
        sharedPrefs.edit {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                else -> throw IllegalArgumentException()
            }
        }
    }
}

