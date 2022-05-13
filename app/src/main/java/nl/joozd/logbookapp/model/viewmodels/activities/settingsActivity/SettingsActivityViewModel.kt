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
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.*
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.notNullFlow
import nl.joozd.logbookapp.extensions.toDateStringLocalized
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.status.SettingsActivityStatus
import nl.joozd.logbookapp.core.DarkModeCenter
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.core.JoozdlogWorkersHubOld
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SettingsActivityViewModel: JoozdlogActivityViewModel(){

    val statusFlow: StateFlow<SettingsActivityStatus?> = MutableStateFlow(null)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    val useIataFlow get() = Prefs.useIataAirportsFlow.notNullFlow()
    val picNameRequiredFlow get() = Prefs.picNameNeedsToBeSetFlow.notNullFlow()
    val standardTakeoffLandingTimesFlow get() = Prefs.standardTakeoffLandingTimesFlow.notNullFlow()

    val backupIntervalFlow = Prefs.backupIntervalFlow.notNullFlow()
    val backupFromCloudFlow = Prefs.backupFromCloudFlow.notNullFlow()

    val useCalendarSyncFlow = Prefs.useCalendarSyncFlow.notNullFlow()
    val calendarSyncTypeFlow = Prefs.calendarSyncTypeFlow.notNullFlow()
    val alwaysPostponeCalendarSyncFlow = Prefs.alwaysPostponeCalendarSyncFlow.notNullFlow()
    private val calendarDisabledUntilFlow = Prefs.calendarDisabledUntilFlow.notNullFlow()

    val calendarDisabledFlow = calendarDisabledUntilFlow.map { calendarIsDisabledNow(it) }
    val calendarDisabled: Boolean
        get() = calendarIsDisabledNow(Prefs.calendarDisabledUntil)

    val useCloudFlow = Prefs.useCloudFlow.notNullFlow()

    val lastUpdateTimeFlow = Prefs.lastUpdateTimeFlow.notNullFlow()

    val usernameFlow = Prefs.usernameFlow

    private val emailAddressFlow = EmailPrefs.emailAddressFlow.notNullFlow()

    private val emailVerifiedFlow = EmailPrefs.emailVerifiedFlow.notNullFlow()
    val emailDataFlow = buildEmailDataFlow()

    val getNamesFromRostersFlow = Prefs.getNamesFromRostersFlow.notNullFlow()


    val calendarDisabledUntilString: String
        get(){
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(Prefs.calendarDisabledUntil), ZoneOffset.UTC)
            return "${time.toDateStringLocalized()} ${time.toTimeStringLocalized()} Z"
        }



    val defaultNightMode
        get() = AppCompatDelegate.getDefaultNightMode().takeIf{ it in (1..2)} ?: 0


    val emailGoodAndVerified
        get() = EmailPrefs.emailAddress.isNotBlank() && EmailPrefs.emailVerified


    /*********************************************************************************************
     * Callable functions
     *********************************************************************************************/

    fun resetStatus(){
        status = null
    }

    fun darkmodePicked(darkMode: Int){
        DarkModeCenter.setDarkMode(darkMode)
    }

    fun toggleUseIataAirports() {
        Prefs.useIataAirports = !Prefs.useIataAirports
    }

    fun toggleRequirePicName(){
        Prefs.picNameNeedsToBeSet = !Prefs.picNameNeedsToBeSet
    }

    /**
     * If [Prefs.useCalendarSync] is true, (switch is on) set it to off
     * else, if a type of sync is selected, switch it to on; if it isn't show dialog (which will switch it on on success)
     */
    fun setGetFlightsFromCalendarClicked() {
        if (!Prefs.useCalendarSync) { // if it is switched on from being off
            Prefs.calendarDisabledUntil = 0

            // If no calendar sync type chosen, show dialog, else just set it to on
            if (Prefs.calendarSyncType == CalendarSyncType.CALENDAR_SYNC_NONE)
                status = SettingsActivityStatus.CalendarDialogNeeded
            else
                Prefs.useCalendarSync = true
        }
        else
            Prefs.useCalendarSync = false
    }

    fun toggleAutoPostponeCalendarSync(){
        Prefs.alwaysPostponeCalendarSync = !Prefs.alwaysPostponeCalendarSync
    }

    fun toggleAddNamesFromRoster(){
        Prefs.getNamesFromRosters = !Prefs.getNamesFromRosters
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
            Prefs.useCloud -> Prefs.useCloud = false

            //toggle on if toggled off but user was logged in before
            //this will force a full resync. Device data will overwrite server data with same FlightID
            UserManagement.signedIn -> {
                Prefs.lastUpdateTime = 0
                Prefs.useCloud = true
                JoozdlogWorkersHubOld.syncTimeAndFlightsIfEnoughTimePassed()
            }

            // Activity will take care of showing &Cs if needed
            else -> {
                Prefs.lastUpdateTime = 0
                status = SettingsActivityStatus.AskIfNewAccountNeeded
            }
        }
    }

    fun forceUseCloud(){
        Prefs.useCloud = true
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
        Prefs.calendarDisabledUntil = 0
    }

    private fun calendarIsDisabledNow(disabledUntil: Long) =
        Prefs.useCalendarSync && disabledUntil > Instant.now().epochSecond

    private fun buildEmailDataFlow() =
        combine(emailAddressFlow, emailVerifiedFlow){ address, verified ->
            address.takeIf{ it.isNotBlank() } to verified
        }
}