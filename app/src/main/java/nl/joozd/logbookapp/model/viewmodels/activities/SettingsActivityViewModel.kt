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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncTypes
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.extensions.toDateStringLocalized
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.ui.utils.DarkModeHub
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SettingsActivityViewModel: JoozdlogActivityViewModel(){

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/



    private val _consensusOptIn = MutableLiveData(Preferences.consensusOptIn)
    private val _picNameNeedsToBeSet = MutableLiveData(Preferences.picNameNeedsToBeSet)
    private val _useIataAirports = MutableLiveData(Preferences.useIataAirports)
    private val _getFlightsFromCalendar = MutableLiveData(Preferences.useCalendarSync)
    private val _alwaysPostponeCalendarSync = MutableLiveData(Preferences.alwaysPostponeCalendarSync)
    private val _useCloudSync = MutableLiveData(Preferences.useCloud)
    private val _getNamesFromRosters = MutableLiveData(Preferences.getNamesFromRosters)

    // Not implemented
    // private val _showOldTimesOnChronoUpdate = MutableLiveData(Preferences.showOldTimesOnChronoUpdate)

    private val _standardTakeoffLandingTimes = MutableLiveData(Preferences.standardTakeoffLandingTimes)
    private val _lastUpdateTime = MutableLiveData(makeTimeString(Preferences.lastUpdateTime))
    private val _backupInterval = MutableLiveData(Preferences.backupInterval)
    private val _backupFromCloud = MutableLiveData(Preferences.backupFromCloud)
    private val _updateLargerFilesOverWifiOnly = MutableLiveData(Preferences.updateLargerFilesOverWifiOnly)

    private val _username = MutableLiveData(Preferences.username) // <String?>
    private val _emailAddress = MutableLiveData(Preferences.emailAddress)
    private val _emailVerified = MutableLiveData(Preferences.emailVerified)

    private val _calendarSyncType = MutableLiveData(Preferences.calendarSyncType)
    private val _calendarDisabledUntil = MutableLiveData(Preferences.calendarDisabledUntil) //  <Long>

    private val _settingsUseIataSelectorTextResource = MutableLiveData(if (Preferences.useIataAirports) R.string.useIataAirports else R.string.useIcaoAirports)

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

            Preferences::useCloud.name ->
                _useCloudSync.value = Preferences.useCloud

            Preferences::username.name ->
                _username.value = Preferences.username

            Preferences::calendarDisabledUntil.name ->
                _calendarDisabledUntil.value = Preferences.calendarDisabledUntil

            Preferences::getNamesFromRosters.name ->
                _getNamesFromRosters.value = Preferences.getNamesFromRosters

            /*
            // Not implemented
            Preferences::showOldTimesOnChronoUpdate.name ->
                _showOldTimesOnChronoUpdate.value = Preferences.showOldTimesOnChronoUpdate
            */

            Preferences::standardTakeoffLandingTimes.name ->
                _standardTakeoffLandingTimes.value = Preferences.standardTakeoffLandingTimes

            Preferences::lastUpdateTime.name -> _lastUpdateTime.value = makeTimeString(Preferences.lastUpdateTime)

            Preferences::backupInterval.name -> _backupInterval.value = Preferences.backupInterval

            Preferences::backupFromCloud.name -> _backupFromCloud.value = Preferences.backupFromCloud

            Preferences::calendarSyncType.name -> _calendarSyncType.value = Preferences.calendarSyncType

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
    }


    /*********************************************************************************************
     * Observables
     *********************************************************************************************/

    val useIataAirports get() = _useIataAirports

    val consensusOptIn: LiveData<Boolean>
        get() = _consensusOptIn

    val picNameNeedsToBeSet: LiveData<Boolean>
        get() = _picNameNeedsToBeSet

    val getFlightsFromCalendar: LiveData<Boolean> get() = _getFlightsFromCalendar

    val alwaysPostponeCalendarSync get() = _alwaysPostponeCalendarSync

    val useCloudSync: LiveData<Boolean>
        get() = _useCloudSync

    val getNamesFromRosters: LiveData<Boolean>
        get() = _getNamesFromRosters

    /*
    // Not implemented
    val showOldTimesOnChronoUpdate: LiveData<Boolean>
        get() = _showOldTimesOnChronoUpdate
    */

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


    // This is not used at the moment but it's there when we need it. Just set [settingsCalendarTypeSpinner] visibility to VISIBLE in SettingsActivity
    // val pickedCalendarType: MutableLiveData<Int>
    //     get() = _calendarType

    val csvUriToShare: LiveData<Uri>
        get() = _uriToShare

    val username: LiveData<String?>
        get() = _username

    val emailAddress = Transformations.map(_emailAddress) { it.takeIf{it.isNotBlank()}}

    val emailVerified: LiveData<Boolean>
        get() = _emailVerified

    val emailData: LiveData<Pair<String?, Boolean>>
        get() = _emailData

    val calendarSyncType: LiveData<Int> = _calendarSyncType.map{
        when{
            !Preferences.useCalendarSync -> 0 // button should be hidden when this is 0
            it == CalendarSyncTypes.CALENDAR_SYNC_NONE -> 0.also { Preferences.useCalendarSync = false } // this should not happen but if it does its okay now
            it == CalendarSyncTypes.CALENDAR_SYNC_DEVICE -> R.string.calendar_this_device
            it == CalendarSyncTypes.CALENDAR_SYNC_ICAL -> R.string.ical_link
            else -> R.string.error // this should not happen aub grgr
        }
    }

    /*
     * If calendar Sync is disabled, this should always be false because it should always be hidden then
     */
    val calendarDisabled: LiveData<Boolean> = Transformations.map(_calendarDisabledUntil) {
        Preferences.useCalendarSync && it > Instant.now().epochSecond
    }

    val calendarDisabledUntilString: String
        get(){
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(Preferences.calendarDisabledUntil), ZoneOffset.UTC)
            return "${time.toDateStringLocalized()} ${time.toTimeStringLocalized()} Z"
        }



    val defaultNightMode
        get() = AppCompatDelegate.getDefaultNightMode().takeIf{ it in (1..2)} ?: 0


    val emailGoodAndVerified
        get() = Preferences.emailAddress.isNotBlank() && Preferences.emailVerified


    /*********************************************************************************************
     * Callable functions
     *********************************************************************************************/

    fun backUpNow() = viewModelScope.launch {
        val dateString = LocalDate.now().toDateStringForFiles()
        //TODO make some kind of "working" animation on button
        _uriToShare.value = JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
    }

    fun darkmodePicked(darkMode: Int){
        DarkModeHub.setDarkMode(darkMode)
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

    /**
     * If [Preferences.useCalendarSync] is true, (switch is on) set it to off
     * else, if a type of sync is selected, switch it to on; if it isn't show dialog (which will switch it on on success)
     */
    fun setGetFlightsFromCalendarClicked() {
        if (!Preferences.useCalendarSync) { // if it is switched on from being off
            Preferences.calendarDisabledUntil = 0

            // If no calendar sync type chosen, show dialog, else just set it to on
            if (Preferences.calendarSyncType == CalendarSyncTypes.CALENDAR_SYNC_NONE)
                feedback(SettingsActivityEvents.CALENDAR_DIALOG_NEEDED)
            else
                Preferences.useCalendarSync = true
        }
        else
            Preferences.useCalendarSync = false
    }

    fun setAutoPostponeCalendarSync(it: Boolean){
        Preferences.alwaysPostponeCalendarSync = it
    }

    fun setAddNamesFromRoster(it: Boolean){
        Preferences.getNamesFromRosters = it
    }

    /*
    // Not implemented
    fun setShowOldTimesOnChronoUpdate(it: Boolean) {
        Preferences.showOldTimesOnChronoUpdate = it
    }
    */

    fun useCloudSyncToggled(){
        when{
            //toggle off if on
            Preferences.useCloud -> Preferences.useCloud = false

            //toggle on if toggled off but user was logged in before
            //this will force a full resync. Device data will overwrite server data with same FlightID
            UserManagement.signedIn -> {
                Preferences.lastUpdateTime = 0
                Preferences.useCloud = true
                JoozdlogWorkersHub.syncTimeAndFlightsIfEnoughTimePassed()
            }

            // Activity will take care of showing &Cs if needed
            else -> {
                Preferences.lastUpdateTime = 0
                feedback(SettingsActivityEvents.WANT_TO_CREATE_NEW_ACCOUNT_QMK)
            }
        }
    }

    /**
     * toggles useCloud to on
     */
    fun forceUseCloud(){
        Preferences.useCloud = true
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

    fun dontPostponeCalendarSync(){
        Preferences.calendarDisabledUntil = 0
        // TODO actually sync calendar
        JoozdlogWorkersHub.syncTimeAndFlightsIfFlightsUpdated()
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