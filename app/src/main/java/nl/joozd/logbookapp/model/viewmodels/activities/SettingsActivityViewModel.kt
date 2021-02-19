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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.calendar.CalendarScraper
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.extensions.toDateStringLocalized
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SettingsActivityViewModel: JoozdlogActivityViewModel(){

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val calendarScraper = CalendarScraper(context)

    private val _consensusOptIn = MutableLiveData(Preferences.consensusOptIn)
    private val _picNameNeedsToBeSet = MutableLiveData(Preferences.picNameNeedsToBeSet)
    private val _useIataAirports = MutableLiveData(Preferences.useIataAirports)
    private val _getFlightsFromCalendar = MutableLiveData(Preferences.useCalendarSync)
    private val _alwaysPostponeCalendarSync = MutableLiveData(Preferences.alwaysPostponeCalendarSync)
    private val _useCloudSync = MutableLiveData(Preferences.useCloud)
    private val _getNamesFromRosters = MutableLiveData(Preferences.getNamesFromRosters)
    private val _showOldTimesOnChronoUpdate = MutableLiveData(Preferences.showOldTimesOnChronoUpdate)
    private val _standardTakeoffLandingTimes = MutableLiveData(Preferences.standardTakeoffLandingTimes)
    private val _lastUpdateTime = MutableLiveData(makeTimeString(Preferences.lastUpdateTime))
    private val _backupInterval = MutableLiveData(Preferences.backupInterval)
    private val _backupFromCloud = MutableLiveData(Preferences.backupFromCloud)
    private val _updateLargerFilesOverWifiOnly = MutableLiveData(Preferences.updateLargerFilesOverWifiOnly)

    private val _username = MutableLiveData(Preferences.username) // <String?>
    private val _emailAddress = MutableLiveData(Preferences.emailAddress)
    private val _emailVerified = MutableLiveData(Preferences.emailVerified)

    private val _calendarDisabledUntil = MutableLiveData(Preferences.calendarDisabledUntil) //  <Long>

    private val _settingsUseIataSelectorTextResource = MutableLiveData<Int>()
    private val _foundCalendars = MutableLiveData<List<JoozdCalendar>>()
    private val _pickedCalendar = MutableLiveData<JoozdCalendar>()

    private val _uriToShare = MutableLiveData<Uri>()
    private val _emailData = MediatorLiveData<Pair<String?, Boolean>>().apply{
        addSource(_emailAddress) { value = it.takeIf{it.isNotBlank()} to Preferences.emailVerified}
        addSource(_emailVerified) { value = Preferences.emailAddress.takeIf{it.isNotBlank()} to Preferences.emailVerified}
    }


    // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
    // search for tag #SETTHISIFNEEDED1
    // private val _calendarType = MutableLiveData<Int>()


    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Preferences::useIataAirports.name ->
                _useIataAirports.value = Preferences.useIataAirports.also {
                    _settingsUseIataSelectorTextResource.value =
                        if (it) R.string.useIataAirports else R.string.useIcaoAirports
                }

            Preferences::useCalendarSync.name ->
                _getFlightsFromCalendar.value = Preferences.useCalendarSync

            Preferences::alwaysPostponeCalendarSync.name ->
                _alwaysPostponeCalendarSync.value = Preferences.alwaysPostponeCalendarSync

            Preferences::selectedCalendar.name ->
                _pickedCalendar.value = _foundCalendars.value?.firstOrNull {c -> c.name == Preferences.selectedCalendar}

            Preferences::useCloud.name ->
                _useCloudSync.value = Preferences.useCloud

            Preferences::username.name ->
                _username.value = Preferences.username

            Preferences::calendarDisabledUntil.name ->
                _calendarDisabledUntil.value = Preferences.calendarDisabledUntil

            Preferences::getNamesFromRosters.name ->
                _getNamesFromRosters.value = Preferences.getNamesFromRosters

            Preferences::showOldTimesOnChronoUpdate.name ->
                _showOldTimesOnChronoUpdate.value = Preferences.showOldTimesOnChronoUpdate

            Preferences::standardTakeoffLandingTimes.name ->
                _standardTakeoffLandingTimes.value = Preferences.standardTakeoffLandingTimes

            Preferences::lastUpdateTime.name -> _lastUpdateTime.value = makeTimeString(Preferences.lastUpdateTime)

            Preferences::backupInterval.name -> _backupInterval.value = Preferences.backupInterval

            Preferences::backupFromCloud.name -> _backupFromCloud.value = Preferences.backupFromCloud

            Preferences::consensusOptIn.name -> _consensusOptIn.value = Preferences.consensusOptIn

            Preferences::picNameNeedsToBeSet.name -> _picNameNeedsToBeSet.value = Preferences.picNameNeedsToBeSet

            Preferences::updateLargerFilesOverWifiOnly.name -> _updateLargerFilesOverWifiOnly.value = Preferences.updateLargerFilesOverWifiOnly

            Preferences::emailAddress.name -> _emailAddress.value = Preferences.emailAddress

            Preferences::emailVerified.name -> _emailVerified.value = Preferences.emailVerified




            // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
            // Preferences::calendarType.name ->
            //    _calendarType.value = Preferences.calendarType
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

    val consensusOptIn: LiveData<Boolean>
        get() = _consensusOptIn

    val picNameNeedsToBeSet: LiveData<Boolean>
        get() = _picNameNeedsToBeSet

    val getFlightsFromCalendar = distinctUntilChanged(_getFlightsFromCalendar)

    val alwaysPostponeCalendarSync = distinctUntilChanged(_alwaysPostponeCalendarSync)

    val useCloudSync: LiveData<Boolean>
        get() = _useCloudSync

    val getNamesFromRosters: LiveData<Boolean>
        get() = _getNamesFromRosters

    val showOldTimesOnChronoUpdate: LiveData<Boolean>
        get() = _showOldTimesOnChronoUpdate

    val standardTakeoffLandingTimes: LiveData<Int>
        get() = _standardTakeoffLandingTimes

    val lastUpdateTime: LiveData<String>
        get() = _lastUpdateTime

    val backupInterval: LiveData<Int>
        get() = _backupInterval

    val backupFromCloud: LiveData<Boolean>
        get() = _backupFromCloud

    val updateLargerFilesOverWifiOnly: LiveData<Boolean>
        get() = _updateLargerFilesOverWifiOnly

    val settingsUseIataSelectorTextResource: LiveData<Int>
        get() = _settingsUseIataSelectorTextResource

    val foundCalendars = Transformations.map(_foundCalendars) {it.map{c -> c.name} }

    // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
    // val pickedCalendarType: MutableLiveData<Int>
    //     get() = _calendarType

    val selectedCalendar: LiveData<JoozdCalendar>
        get() = _pickedCalendar

    val csvUriToShare: LiveData<Uri>
        get() = _uriToShare

    val username: LiveData<String?>
        get() = _username

    val emailAddress = Transformations.map(_emailAddress) { it.takeIf{it.isNotBlank()}}

    val emailVerified: LiveData<Boolean>
        get() = _emailVerified

    val emailData: LiveData<Pair<String?, Boolean>>
        get() = _emailData

    val calendarDisabled: LiveData<Boolean> = Transformations.map(_calendarDisabledUntil) {
        it > Instant.now().epochSecond
    }

    val calendarDisabledUntilString: String
        get(){
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(Preferences.calendarDisabledUntil), ZoneOffset.UTC)
            return "${time.toDateStringLocalized()} ${time.toTimeStringLocalized()} Z"
        }



    /*********************************************************************************************
     * Callable functions
     *********************************************************************************************/

    fun backUpNow() = viewModelScope.launch {
        val dateString = LocalDate.now().toDateStringForFiles()
        //TODO make some kind of "working" animation on button
        _uriToShare.value = JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
    }

    fun setUseIataAirports(useIata: Boolean) {
        Preferences.useIataAirports = useIata
    }

    fun setConsensusOptIn(optIn: Boolean){
        Preferences.consensusOptIn = optIn
    }

    fun setMarkIncompleteWithoutPIC(it: Boolean){
        Preferences.picNameNeedsToBeSet = it
    }

    fun setGetFlightsFromCalendar(it: Boolean) {
        if (it && !Preferences.useCalendarSync) // if it is switched on from being off
            Preferences.calendarDisabledUntil = 0
        Preferences.useCalendarSync = it
    }

    fun setAutoPostponeCalendarSync(it: Boolean){
        Preferences.alwaysPostponeCalendarSync = it
    }

    fun setAddNamesFromRoster(it: Boolean){
        Preferences.getNamesFromRosters = it
    }

    fun setShowOldTimesOnChronoUpdate(it: Boolean) {
        Preferences.showOldTimesOnChronoUpdate = it
    }

    fun useCloudSyncToggled(){
        Preferences.useCloud = !Preferences.useCloud
    }

    /**
     * Toggles Preferences.updateLargerFilesOverWifiOnly and reschedules workers with that preference
     */
    fun useWifiForLargeFilesToggled(){
        Preferences.updateLargerFilesOverWifiOnly = !Preferences.updateLargerFilesOverWifiOnly
        JoozdlogWorkersHub.rescheduleAircraftAndAirports(Preferences.updateLargerFilesOverWifiOnly)
    }

    fun copyLoginLinkToClipboard(){
        UserManagement.generateLoginLink()?.let { loginLink ->
            with (App.instance) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.login_link), loginLink)
                )
            }
            feedback(SettingsActivityEvents.LOGIN_LINK_COPIED)
        } ?: feedback(SettingsActivityEvents.NOT_LOGGED_IN)
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

    fun dontPostponeCalendarSync(){
        Preferences.calendarDisabledUntil = 0
        flightRepository.syncIfNeeded()
    }

    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    fun fillCalendarsList() {
        viewModelScope.launch {
            _foundCalendars.value =
                withContext(Dispatchers.IO) { calendarScraper.getCalendarsList() }
            _pickedCalendar.value = _foundCalendars.value?.firstOrNull {c -> c.name == Preferences.selectedCalendar}
        }
    }

    private fun makeTimeString(instant: Long): String =
        if (instant < 0) "Never"
        else  LocalDateTime.ofInstant(Instant.ofEpochSecond(instant), ZoneOffset.UTC).let{
            "${it.toDateStringLocalized()} ${it.toTimeStringLocalized()}Z"
        }




    /*********************************************************************************************
     * Companion object
     *********************************************************************************************/


    companion object{
        // empty
    }
}