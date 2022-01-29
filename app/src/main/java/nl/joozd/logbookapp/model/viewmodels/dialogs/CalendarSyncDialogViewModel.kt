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

import android.Manifest
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RequiresPermission
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.calendar.CalendarScraper
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncTypes
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.CalendarSyncDialogEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

class CalendarSyncDialogViewModel : JoozdlogDialogViewModel() {
    // Private flags
    private var calendarScrapeWaitingForCalendars: Boolean = false

    /**
     * Temporary values for settings
     */
    private var mCalendarSyncType: Int
        get() = _calendarSyncType.value!!
        set(it) { _calendarSyncType.value = it.also{
            println("CalnedarSyncType now $it")
        } }

    private var mCalendarSyncIcalAddress: String
        get() = _calendarSyncIcalAddress.value!!
        set(it) { _calendarSyncIcalAddress.value = it }


    private var mSelectedCalendar: JoozdCalendar?
        get() = _selectedCalendar.value
        set(it) { _selectedCalendar.value = it }

    private val calendarScraper = CalendarScraper(context)

    private val _foundCalendars = MutableLiveData<List<JoozdCalendar>?>() // null until initialized with [fillCalendarsList]
    private val _selectedCalendar = MutableLiveData<JoozdCalendar?>() // null until initialized with [fillCalendarsList]
    private val _calendarSyncType = MutableLiveData(Preferences.calendarSyncType) // will never be null because initialized here
    private val _calendarSyncIcalAddress = MutableLiveData(Preferences.calendarSyncIcalAddress) // will never be null because initialized here

    private val _okButtonEnabled = MediatorLiveData<Boolean>().apply{
        addSource(_selectedCalendar){
            value = checkOkButtonActive()
        }
        addSource(_calendarSyncType){
            value = checkOkButtonActive()
        }
        addSource(_calendarSyncIcalAddress){
            value = checkOkButtonActive()
        }
    }

    private val clipboard: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /**
     * To support other companies etc add extra regexes here and use them in [getLinkFromClipboard]
     */
    private val klmIcalRegex = "https://calendar.klm.com/crew/.+".toRegex()

    var foundLink: String? = Preferences.calendarSyncIcalAddress.nullIfBlank()
        private set



    val foundCalendars = Transformations.map(_foundCalendars) {it?.map{ c -> c.name} ?: emptyList() }
    val selectedCalendar: LiveData<JoozdCalendar?> get() = _selectedCalendar
    val calendarSyncType: LiveData<Int> get() = _calendarSyncType
    val calendarSyncIcalAddress: LiveData<String> get() = _calendarSyncIcalAddress
    val okButtonEnabled: LiveData<Boolean> get() = _okButtonEnabled

    /**
     * This can be set to on if a sync shouldn't happen right after clicking OK
     */
    var sync: Boolean = true

    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Preferences::selectedCalendar.name ->
                _selectedCalendar.value = _foundCalendars.value?.firstOrNull { c -> c.name == Preferences.selectedCalendar }

            Preferences::calendarSyncType.name -> {
                mCalendarSyncType = Preferences.calendarSyncType
            }

        }
    }

    /*******************************************
     * Functions to be ran on (re)creation
     *******************************************/

    /**
     * Fill calendars list
     * call [calendarScraperRadioButtonClicked] if that was waiting for calendar list to be filled
     */
    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    fun fillCalendarsList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { calendarScraper.getCalendarsList() }.let { _foundCalendars.value = it }
            _selectedCalendar.value = _foundCalendars.value?.firstOrNull { c -> c.name == Preferences.selectedCalendar} ?: run{
                //if no calendar matches, make first calendar in list the picked calendar
                _foundCalendars.value?.firstOrNull()?.let{ firstCalendarInList ->
                    mSelectedCalendar = firstCalendarInList // this will also set _selectedCalendar through Preferences listener
                    firstCalendarInList
                }
            }
            if (calendarScrapeWaitingForCalendars){
                calendarScraperRadioButtonClicked()
            }
        }
    }

    /**
     * This checks clipboard for an iCalendar link, and if found, places it in foundLink and returns true
     * @return true if an iCal link was found on clipboard, false if not
     */
    fun checkClipboardForIcalLink(): Boolean =
        getLinkFromClipboard().let{
            it != null && it != mCalendarSyncIcalAddress
        }


    /*******************************************
     * Functions to be ran on a UI action
     *******************************************/

    /**
     * Calendar Scraper radio button clicked.
     * This should set [Preferences.calendarSyncType] to [CalendarSyncTypes.CALENDAR_SYNC_DEVICE]
     * which in turn should trigger a watcher that updates a livedata that updates the UI
     * If no calendars found (ie. no permission (yet)), it sets [calendarScrapeWaitingForCalendars] to true
     */
    fun calendarScraperRadioButtonClicked(){
        if (_foundCalendars.value != null) {
            mCalendarSyncType = CalendarSyncTypes.CALENDAR_SYNC_DEVICE
            calendarScrapeWaitingForCalendars = false
        }
        else {
            mCalendarSyncType = CalendarSyncTypes.CALENDAR_SYNC_NONE
            calendarScrapeWaitingForCalendars = true
        }

    }

    /**
     * If a new link is on clipboard, use that
     * If there isn't one but an old one was stored, use that
     * If there isn't one, feedback [CalendarSyncDialogEvents.NO_ICAL_LINK_FOUND] and set type to NONE
     */
    fun icalSubscriptionRadioButtonClicked(){
        getLinkFromClipboard()?.let { foundLink = it }
        if (foundLink != null) {
            useFoundLink()
        }
        else {
            feedback(CalendarSyncDialogEvents.NO_ICAL_LINK_FOUND)
            mCalendarSyncType=CalendarSyncTypes.CALENDAR_SYNC_NONE
        }
    }

    fun calendarPicked(index: Int){
        _foundCalendars.value?.get(index)?.let{
            mSelectedCalendar = it
        } ?: run { Preferences.calendarSyncType=CalendarSyncTypes.CALENDAR_SYNC_NONE }
    }

    /**
     * Set the selected calendar type + its associated data, and, if one actually is selected, set [Preferences.useCalendarSync] to true and sync it
     */
    fun okClicked(){
        Preferences.calendarSyncType = mCalendarSyncType
        Preferences.selectedCalendar = mSelectedCalendar?.name ?: ""
        Preferences.calendarSyncIcalAddress = mCalendarSyncIcalAddress
        Preferences.useCalendarSync = (mCalendarSyncType != CalendarSyncTypes.CALENDAR_SYNC_NONE).also{
            if (it) {
                Preferences.nextCalendarCheckTime = 0
            }
        }
        feedback(CalendarSyncDialogEvents.DONE)
    }

    private fun checkOkButtonActive(): Boolean = when (mCalendarSyncType){
        CalendarSyncTypes.CALENDAR_SYNC_NONE -> false
        CalendarSyncTypes.CALENDAR_SYNC_ICAL -> mCalendarSyncIcalAddress.isNotBlank()
        CalendarSyncTypes.CALENDAR_SYNC_DEVICE -> mSelectedCalendar != null
        else -> false // It will never be this but compiler doesn't know that
    }

    /**
     * Use [foundLink] to fill appropriate fields, will feedback an ERROR if null
     */
    private fun useFoundLink() {
        foundLink?.let {
            println("found link $foundLink")
            mCalendarSyncIcalAddress = it
            mCalendarSyncType = CalendarSyncTypes.CALENDAR_SYNC_ICAL

        } ?: feedback(CalendarSyncDialogEvents.ERROR)
    }

    /**
     * Grab iCalendar link from cliboard
     * @return link as String if found, null if not.
     */
    private fun getLinkFromClipboard(): String?{
        with (clipboard){
            if (!hasPrimaryClip()) return null.also{ println("no primary clip")}
            println(clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN))
            println(clipboard.primaryClip?.getItemAt(0)?.text)
            return clipboard.primaryClip?.getItemAt(0)?.text?.let {
                klmIcalRegex.find(it)?.value
            }

        }
    }




    init {
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPrefsChangedListener)
    }

    override fun onCleared() {
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPrefsChangedListener)
        super.onCleared()
    }
}