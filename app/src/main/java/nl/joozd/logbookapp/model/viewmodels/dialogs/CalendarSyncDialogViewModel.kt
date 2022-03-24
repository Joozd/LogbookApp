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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import nl.joozd.joozdcalendarapi.CalendarDescriptor
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncTypes
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.viewmodels.status.Status
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class CalendarSyncDialogViewModel : JoozdlogDialogViewModel() {
    val statusFlow: StateFlow<Status?> = MutableStateFlow(null)

    private var status: Status? by CastFlowToMutableFlowShortcut(statusFlow)

    val foundCalendarsFlow: StateFlow<List<CalendarDescriptor>?> = MutableStateFlow(null) // null until initialized with [fillCalendarsList]
    val selectedCalendarFlow: StateFlow<CalendarDescriptor?> = MutableStateFlow(null) // null until initialized with [fillCalendarsList]
    val calendarSyncTypeFlow: StateFlow<Int> = MutableStateFlow(Preferences.calendarSyncType) // will never be null because initialized here
    private val calendarSyncIcalAddressFlow: StateFlow<String> = MutableStateFlow(Preferences.calendarSyncIcalAddress) // will never be null because initialized here

    private var foundCalendars: List<CalendarDescriptor>? by CastFlowToMutableFlowShortcut(foundCalendarsFlow)

    var selectedCalendar: CalendarDescriptor? by CastFlowToMutableFlowShortcut(selectedCalendarFlow)
        private set

    var calendarSyncType: Int by CastFlowToMutableFlowShortcut(calendarSyncTypeFlow)
        private set

    var calendarSyncIcalAddress: String by CastFlowToMutableFlowShortcut(calendarSyncIcalAddressFlow)
        private set

    val okButtonEnabledFlow = makeOkButtonEnabledFlow()

    var foundLink: String? = Preferences.calendarSyncIcalAddress.nullIfBlank()
        private set

    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Preferences::selectedCalendar.name ->
                selectedCalendar = foundCalendars?.firstOrNull { c -> c.displayName == Preferences.selectedCalendar }

            Preferences::calendarSyncType.name -> {
                calendarSyncType = Preferences.calendarSyncType
            }

        }
    }

    /*******************************************
     * Functions to be ran on (re)creation
     *******************************************/

    fun fillCalendarsList(calendars: List<CalendarDescriptor>){
        foundCalendars = calendars
        selectedCalendar = calendars.firstOrNull { c -> c.displayName == Preferences.selectedCalendar}
            ?: calendars.firstOrNull()
    }

    private fun makeOkButtonEnabledFlow() = combine(
        selectedCalendarFlow,
        calendarSyncTypeFlow,
        calendarSyncIcalAddressFlow
    ) { cal, syncType, address ->
        when (syncType) {
            CalendarSyncTypes.CALENDAR_SYNC_NONE -> false
            CalendarSyncTypes.CALENDAR_SYNC_ICAL -> address.isNotBlank()
            CalendarSyncTypes.CALENDAR_SYNC_DEVICE -> cal != null
            else -> false // It will never be this but compiler doesn't know that
        }
    }

    /*******************************************
     * Functions to be ran on a UI action
     *******************************************/

    /**
     * Calendar Scraper radio button clicked.
     * This should set [Preferences.calendarSyncType] to [CalendarSyncTypes.CALENDAR_SYNC_DEVICE]
     */
    fun calendarScraperRadioButtonClicked(){
        calendarSyncType = if (foundCalendars != null) {
            CalendarSyncTypes.CALENDAR_SYNC_DEVICE
        } else {
            CalendarSyncTypes.CALENDAR_SYNC_NONE
        }

    }

    /**
     * If a new link is on clipboard, use that
     * If there isn't one but an old one was stored, use that
     * If there isn't one, set status to NO_ICAL_LINK_FOUND and set type to NONE
     */
    fun icalSubscriptionRadioButtonClicked(link: String?){
        link?.let { foundLink = it }
        if (foundLink != null) {
            useFoundLink()
        }
        else {
            status = Status.NO_ICAL_LINK_FOUND
            calendarSyncType=CalendarSyncTypes.CALENDAR_SYNC_NONE
        }
    }

    fun calendarPicked(index: Int){
        foundCalendars?.get(index)?.let{
            selectedCalendar = it
        } ?: run { calendarSyncType=CalendarSyncTypes.CALENDAR_SYNC_NONE }
    }

    /**
     * Set the selected calendar type + its associated data, and, if one actually is selected, set [Preferences.useCalendarSync] to true and sync it
     */
    fun okClicked(){
        Preferences.calendarSyncType = calendarSyncType
        Preferences.selectedCalendar = selectedCalendar?.displayName ?: ""
        Preferences.calendarSyncIcalAddress = calendarSyncIcalAddress
        Preferences.useCalendarSync = (calendarSyncType != CalendarSyncTypes.CALENDAR_SYNC_NONE).also{
            if (it) {
                Preferences.nextCalendarCheckTime = 0
            }
        }
        status = Status.DONE
    }

    fun resetStatus(){
        status = null
    }



    /**
     * Use [foundLink] to fill appropriate fields, will feedback an ERROR if null
     */
    private fun useFoundLink() {
        foundLink?.let {
            calendarSyncIcalAddress = it
            calendarSyncType = CalendarSyncTypes.CALENDAR_SYNC_ICAL
        } ?: run { status = Status.Error("No link found to use") }
    }

    init {
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPrefsChangedListener)
    }

    override fun onCleared() {
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPrefsChangedListener)
        super.onCleared()
    }
}