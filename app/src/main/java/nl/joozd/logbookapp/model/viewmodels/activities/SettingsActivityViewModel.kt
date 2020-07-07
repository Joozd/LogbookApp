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

import android.Manifest
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.calendar.CalendarFlightUpdater
import nl.joozd.logbookapp.data.calendar.CalendarScraper
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import java.time.Instant

class SettingsActivityViewModel: JoozdlogActivityViewModel(){

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val calendarScraper = CalendarScraper(context)

    private val _useIataAirports = MutableLiveData<Boolean>(Preferences.useIataAirports)
    private val _getFlightsFromCalendar = MutableLiveData<Boolean>(Preferences.getFlightsFromCalendar)
    private val _settingsUseIataSelectorTextResource = MutableLiveData<Int>()
    private val _foundCalendars = MutableLiveData<List<JoozdCalendar>>()
    private val _pickedCalendar = MutableLiveData<JoozdCalendar>()
    private val pickedCalendar: JoozdCalendar?
        get() = _pickedCalendar.value

    // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
    // search for tag #SETTHISIFNEEDED1
    private val _calendarType = MutableLiveData<Int>()


    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Preferences::useIataAirports.name ->
                _useIataAirports.value = Preferences.useIataAirports.also {
                    _settingsUseIataSelectorTextResource.value =
                        if (it) R.string.useIataAirports else R.string.useIcaoAirports
                }
            Preferences::getFlightsFromCalendar.name ->
                _getFlightsFromCalendar.value = Preferences.getFlightsFromCalendar
            Preferences::selectedCalendar.name ->
                _pickedCalendar.value = _foundCalendars.value?.firstOrNull {c -> c.name == Preferences.selectedCalendar}

            // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
            Preferences::calendarType.name ->
                _calendarType.value = Preferences.calendarType
        }
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
        _settingsUseIataSelectorTextResource.value = if (Preferences.useIataAirports) R.string.useIataAirports else R.string.useIcaoAirports
    }


    /*********************************************************************************************
     * Observables
     *********************************************************************************************/

    val useIataAirports = distinctUntilChanged(_useIataAirports)

    val getFlightsFromCalendar = distinctUntilChanged(_getFlightsFromCalendar)

    val settingsUseIataSelectorTextResource: LiveData<Int>
        get() = _settingsUseIataSelectorTextResource

    val foundCalendars = Transformations.map(_foundCalendars) {it.map{c -> c.name} }

    // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
    val pickedCalendarType: MutableLiveData<Int>
        get() = _calendarType

    val selectedCalendar: LiveData<JoozdCalendar>
        get() = _pickedCalendar



    /*********************************************************************************************
     * Callable functions
     *********************************************************************************************/

    fun setUseIataAirports(useIata: Boolean) {
        Preferences.useIataAirports = useIata
    }

    fun setGetFlightsFromCalendar(it: Boolean) {
        if (it && !Preferences.getFlightsFromCalendar) // if it is switched on from being off
            Preferences.calendarDisabledUntil = 0
        Preferences.getFlightsFromCalendar = it

    }

    fun calendarPicked(index: Int){
        _foundCalendars.value?.get(index)?.let{
            Preferences.selectedCalendar = it.name
            flightRepository.syncIfNeeded()
        }
    }

    // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
    fun calendarTypePicked(index: Int){
        Preferences.calendarType = index
    }

    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    fun fillCalendarsList() {
        viewModelScope.launch {
            _foundCalendars.value =
                withContext(Dispatchers.IO) { calendarScraper.getCalendarsList() }
            _pickedCalendar.value = _foundCalendars.value?.firstOrNull {c -> c.name == Preferences.selectedCalendar}
        }
    }

    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    fun tempButtonClicked(){
        Log.d(this::class.simpleName, "selectedCalendar is ${selectedCalendar.value}")
        pickedCalendar?.let {
            viewModelScope.launch {
                val foundFlights = CalendarFlightUpdater().getFlights()
                Log.d(this::class.simpleName, "Found flights: $foundFlights")
            }
        } ?: Log.d(this::class.simpleName, "pickedCalendar is null: $pickedCalendar")
    }


    /*********************************************************************************************
     * Companion object
     *********************************************************************************************/


    companion object{
        // empty
    }
}