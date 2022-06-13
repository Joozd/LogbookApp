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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import nl.joozd.joozdcalendarapi.CalendarDescriptor
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.viewmodels.status.CalendarDialogStatus
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class CalendarSyncDialogViewModel : JoozdlogDialogViewModel() {
    val statusFlow: StateFlow<CalendarDialogStatus?> = MutableStateFlow(null)

    private var status: CalendarDialogStatus? by CastFlowToMutableFlowShortcut(statusFlow)

    val foundCalendarsFlow: StateFlow<List<CalendarDescriptor>?> = MutableStateFlow(null) // null until initialized with [fillCalendarsList]
    val selectedCalendarFlow: StateFlow<CalendarDescriptor?> = MutableStateFlow(null) // null until initialized with [fillCalendarsList]
    val calendarSyncTypeFlow: StateFlow<CalendarSyncType?> = MutableStateFlow(null)
    private val calendarSyncIcalAddressFlow: StateFlow<String?> = MutableStateFlow(null) // will never be null because initialized here

    private var foundCalendars: List<CalendarDescriptor>? by CastFlowToMutableFlowShortcut(foundCalendarsFlow)

    var selectedCalendar: CalendarDescriptor? by CastFlowToMutableFlowShortcut(selectedCalendarFlow)
        private set

    var calendarSyncType: CalendarSyncType? by CastFlowToMutableFlowShortcut(calendarSyncTypeFlow)

    var calendarSyncIcalAddress: String? by CastFlowToMutableFlowShortcut(calendarSyncIcalAddressFlow)

    val okButtonEnabledFlow = makeOkButtonEnabledFlow()

    var foundLink: String? = calendarSyncIcalAddress?.nullIfBlank()
        private set

    /*******************************************
     * Functions to be ran on (re)creation
     *******************************************/

    suspend fun fillCalendarsList(calendars: List<CalendarDescriptor>){
        foundCalendars = calendars
        selectedCalendar = calendars.firstOrNull { c -> c.displayName == Prefs.selectedCalendar()}
            ?: calendars.firstOrNull()
    }

    private fun makeOkButtonEnabledFlow() = combine(
        selectedCalendarFlow,
        calendarSyncTypeFlow,
        calendarSyncIcalAddressFlow
    ) { cal, syncType, address ->
        when (syncType) {
            CalendarSyncType.CALENDAR_SYNC_NONE, null -> false
            CalendarSyncType.CALENDAR_SYNC_ICAL -> address?.isNotBlank() ?: false
            CalendarSyncType.CALENDAR_SYNC_DEVICE -> cal != null
        }
    }

    /*******************************************
     * Functions to be ran on a UI action
     *******************************************/

    /**
     * Calendar Scraper radio button clicked.
     * This should set [Prefs.calendarSyncType] to [CalendarSyncType.CALENDAR_SYNC_DEVICE]
     */
    fun calendarScraperRadioButtonClicked(){
        calendarSyncType = if (foundCalendars != null)
            CalendarSyncType.CALENDAR_SYNC_DEVICE
        else
            CalendarSyncType.CALENDAR_SYNC_NONE
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
            status = CalendarDialogStatus.NO_ICAL_LINK_FOUND
            calendarSyncType=CalendarSyncType.CALENDAR_SYNC_NONE
        }
    }

    fun calendarPicked(index: Int){
        foundCalendars?.get(index)?.let{
            selectedCalendar = it
        } ?: run { calendarSyncType=CalendarSyncType.CALENDAR_SYNC_NONE }
    }

    /**
     * Set the selected calendar type + its associated data, and, if one actually is selected, set [Prefs.useCalendarSync] to true and sync it
     */
    fun okClicked(){
        calendarSyncType?.let { Prefs.calendarSyncType(it) } // OK button should be disabled if calendarSyncType == null
        Prefs.selectedCalendar(selectedCalendar?.displayName ?: "")
        calendarSyncIcalAddress?.let { Prefs.calendarSyncIcalAddress(it) }
        Prefs.useCalendarSync((calendarSyncType != CalendarSyncType.CALENDAR_SYNC_NONE).also{
            if (it) {
                Prefs.nextCalendarCheckTime(0)
            }
        })
        status = CalendarDialogStatus.DONE
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
            calendarSyncType = CalendarSyncType.CALENDAR_SYNC_ICAL
        } ?: run { status = CalendarDialogStatus.Error("No link found to use") }
    }
}