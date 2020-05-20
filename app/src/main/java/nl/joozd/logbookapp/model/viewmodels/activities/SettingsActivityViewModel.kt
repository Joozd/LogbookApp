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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel

class SettingsActivityViewModel: JoozdlogActivityViewModel(){
    private val _useIataAirports = MutableLiveData<Boolean>(Preferences.useIataAirports)
    val useIataAirports = distinctUntilChanged(_useIataAirports)

    fun setUseIataAirports(useIata: Boolean) {
        Preferences.useIataAirports = useIata
    }

    private val _settingsUseIataSelectorTextResource = MutableLiveData<Int>()
    val settingsUseIataSelectorTextResource: LiveData<Int>
        get() = _settingsUseIataSelectorTextResource
    init{
        _settingsUseIataSelectorTextResource.value = if (Preferences.useIataAirports) R.string.useIataAirports else R.string.useIcaoAirports
    }

    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Log.d("AirportRepository", "key = $key")
        if (key == Preferences::useIataAirports.name) {
            _useIataAirports.value = Preferences.useIataAirports.also{
                _settingsUseIataSelectorTextResource.value = if (it) R.string.useIataAirports else R.string.useIcaoAirports
            }

        }
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }
}