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

package nl.joozd.logbookapp.model.viewmodels.activities.settingsActivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.extensions.toDateStringLocalized
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.status.SettingsActivityStatus
import nl.joozd.logbookapp.ui.utils.DarkModeHub
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SettingsActivityViewModel: JoozdlogActivityViewModel(){

    val statusFlow: StateFlow<SettingsActivityStatus?> = MutableStateFlow(null)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    val useIataFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.useIataAirports)
    private var useIata by CastFlowToMutableFlowShortcut(useIataFlow)

    val picNameRequiredFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.picNameNeedsToBeSet)
    private var picNameRequired by CastFlowToMutableFlowShortcut(picNameRequiredFlow)

    val standardTakeoffLandingTimesFlow: StateFlow<Int> = MutableStateFlow(Preferences.standardTakeoffLandingTimes)
    private var standardTakeoffLandingTimes: Int by CastFlowToMutableFlowShortcut(standardTakeoffLandingTimesFlow)

    val backupIntervalFlow: StateFlow<Int> = MutableStateFlow(Preferences.backupInterval)
    private var backupInterval: Int by CastFlowToMutableFlowShortcut(backupIntervalFlow)

    val backupFromCloudFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.backupFromCloud)
    private var backupFromCloud by CastFlowToMutableFlowShortcut(backupFromCloudFlow)

    val useCalendarSyncFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.useCalendarSync)
    private var useCalendarSync by CastFlowToMutableFlowShortcut(useCalendarSyncFlow)

    val calendarSyncTypeFlow: StateFlow<CalendarSyncType> = MutableStateFlow(Preferences.calendarSyncType)
    private var calendarSyncType: CalendarSyncType by CastFlowToMutableFlowShortcut(calendarSyncTypeFlow)

    val alwaysPostponeCalendarSyncFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.alwaysPostponeCalendarSync)
    private var alwaysPostponeCalendarSync by CastFlowToMutableFlowShortcut(alwaysPostponeCalendarSyncFlow)

    private val calendarDisabledUntilFlow: StateFlow<Long> = MutableStateFlow(Preferences.calendarDisabledUntil)
    private var calendarDisabledUntil: Long by CastFlowToMutableFlowShortcut(calendarDisabledUntilFlow)

    val calendarDisabledFlow = calendarDisabledUntilFlow.map { calendarIsDisabledNow(it) }
    val calendarDisabled: Boolean
        get() = calendarIsDisabledNow(calendarDisabledUntil)

    val useCloudFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.useCloud)
    private var useCloud by CastFlowToMutableFlowShortcut(useCloudFlow)

    val lastUpdateTimeFlow: StateFlow<Long> = MutableStateFlow(Preferences.lastUpdateTime)
    private var lastUpdateTime: Long by CastFlowToMutableFlowShortcut(lastUpdateTimeFlow)

    val usernameFlow: StateFlow<String?> = MutableStateFlow(Preferences.username)
    private var username by CastFlowToMutableFlowShortcut(usernameFlow)

    private val emailAddressFlow: StateFlow<String> = MutableStateFlow(Preferences.emailAddress)
    private var emailAddress by CastFlowToMutableFlowShortcut(emailAddressFlow)

    private val emailVerifiedFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.emailVerified)
    private var emailVerified by CastFlowToMutableFlowShortcut(emailVerifiedFlow)
    val emailDataFlow = buildEmailDataFlow()

    val getNamesFromRostersFlow: StateFlow<Boolean> = MutableStateFlow(Preferences.getNamesFromRosters)
    private var getNamesFromRosters by CastFlowToMutableFlowShortcut(getNamesFromRostersFlow)


    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val onSharedPrefsChangedListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Preferences::useIataAirports.name -> useIata = Preferences.useIataAirports
            Preferences::useCalendarSync.name -> useCalendarSync = Preferences.useCalendarSync
            Preferences::alwaysPostponeCalendarSync.name -> alwaysPostponeCalendarSync = Preferences.alwaysPostponeCalendarSync
            Preferences::useCloud.name -> useCloud = Preferences.useCloud
            Preferences::username.name -> username = Preferences.username
            Preferences::calendarDisabledUntil.name -> calendarDisabledUntil = Preferences.calendarDisabledUntil
            Preferences::getNamesFromRosters.name -> getNamesFromRosters = Preferences.getNamesFromRosters
            Preferences::lastUpdateTime.name -> lastUpdateTime = Preferences.lastUpdateTime
            Preferences::backupInterval.name -> backupInterval = Preferences.backupInterval
            Preferences::backupFromCloud.name -> backupFromCloud = Preferences.backupFromCloud
            Preferences::calendarSyncType.name -> calendarSyncType = Preferences.calendarSyncType
            Preferences::picNameNeedsToBeSet.name -> picNameRequired = Preferences.picNameNeedsToBeSet
            Preferences::standardTakeoffLandingTimes.name -> standardTakeoffLandingTimes = Preferences.standardTakeoffLandingTimes
            Preferences::emailAddress.name -> emailAddress = Preferences.emailAddress
            Preferences::emailVerified.name -> emailVerified = Preferences.emailVerified
        }
    }.also{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (it)
    }

    override fun onCleared() {
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPrefsChangedListener)
    }


    /*********************************************************************************************
     * Observables
     *********************************************************************************************/

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

    fun resetStatus(){
        status = null
    }

    fun backUpNow() = viewModelScope.launch {
        val dateString = LocalDate.now().toDateStringForFiles()
        status = SettingsActivityStatus.BuildingCsv
        val uri = JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
        status = SettingsActivityStatus.SharedUri(uri)
    }

    fun darkmodePicked(darkMode: Int){
        DarkModeHub.setDarkMode(darkMode)
    }

    fun toggleUseIataAirports() {
        Preferences.useIataAirports = !Preferences.useIataAirports
    }

    fun toggleRequirePicName(){
        Preferences.picNameNeedsToBeSet = !Preferences.picNameNeedsToBeSet
    }

    /**
     * If [Preferences.useCalendarSync] is true, (switch is on) set it to off
     * else, if a type of sync is selected, switch it to on; if it isn't show dialog (which will switch it on on success)
     */
    fun setGetFlightsFromCalendarClicked() {
        if (!Preferences.useCalendarSync) { // if it is switched on from being off
            Preferences.calendarDisabledUntil = 0

            // If no calendar sync type chosen, show dialog, else just set it to on
            if (Preferences.calendarSyncType == CalendarSyncType.CALENDAR_SYNC_NONE)
                status = SettingsActivityStatus.CalendarDialogNeeded
            else
                Preferences.useCalendarSync = true
        }
        else
            Preferences.useCalendarSync = false
    }

    fun toggleAutoPostponeCalendarSync(){
        Preferences.alwaysPostponeCalendarSync = !Preferences.alwaysPostponeCalendarSync
    }

    fun toggleAddNamesFromRoster(){
        Preferences.getNamesFromRosters = !Preferences.getNamesFromRosters
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
                status = SettingsActivityStatus.AskIfNewAccountNeeded
            }
        }
    }

    fun forceUseCloud(){
        Preferences.useCloud = true
    }

    fun copyLoginLinkToClipboard(){
        UserManagement.generateLoginLink()?.let { loginLink ->
            status =
            with (App.instance) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.login_link), loginLink)
                )
                SettingsActivityStatus.LoginLinkCopied
            }

        } ?: SettingsActivityStatus.Error(R.string.not_signed_in)
    }

    fun dontPostponeCalendarSync(){
        Preferences.calendarDisabledUntil = 0
    }

    private fun calendarIsDisabledNow(disabledUntil: Long) =
        Preferences.useCalendarSync && disabledUntil > Instant.now().epochSecond

    private fun buildEmailDataFlow() =
        combine(emailAddressFlow, emailVerifiedFlow){ address, verified ->
            address.takeIf{ it.isNotBlank() } to verified
        }
}