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

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.toDateStringLocalized
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.status.SettingsActivityStatus
import nl.joozd.logbookapp.core.DarkModeCenter
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SettingsActivityViewModel: JoozdlogActivityViewModel(){
    val statusFlow: StateFlow<SettingsActivityStatus?> = MutableStateFlow(null)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    val showHintFlow: StateFlow<Int?> = MutableStateFlow(null) // holds a ResID
    private var showHint by CastFlowToMutableFlowShortcut(showHintFlow)

    val useIataFlow get() = Prefs.useIataAirports.flow
    val picNameRequiredFlow get() = Prefs.picNameNeedsToBeSet.flow
    val augmentedTakeoffLandingTimesFlow get() = Prefs.augmentedTakeoffLandingTimes.flow

    val backupIntervalFlow = Prefs.backupInterval.flow
    val sendBackupEmailsFlow = Prefs.sendBackupEmails.flow

    val useCalendarSyncFlow = Prefs.useCalendarSync.flow
    val calendarSyncTypeFlow = Prefs.calendarSyncType.flow
    val alwaysPostponeCalendarSyncFlow = Prefs.alwaysPostponeCalendarSync.flow
    private val calendarDisabledUntilFlow = Prefs.calendarDisabledUntil.flow

    val calendarDisabledFlow = calendarDisabledUntilFlow.map { calendarIsDisabledNow(it) }
    suspend fun calendarDisabled(): Boolean =
        calendarIsDisabledNow(Prefs.calendarDisabledUntil())

    val getNamesFromRostersFlow = Prefs.getNamesFromRosters.flow


    suspend fun calendarDisabledUntilString(): String {
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(Prefs.calendarDisabledUntil()), ZoneOffset.UTC)
            return "${time.toDateStringLocalized()} ${time.toTimeStringLocalized()} Z"
        }



    val defaultNightMode
        get() = AppCompatDelegate.getDefaultNightMode().takeIf{ it in (1..2)} ?: 0


    suspend fun emailGoodAndVerified() =
        EmailPrefs.emailAddress().isNotBlank() && EmailPrefs.emailVerified()


    /*********************************************************************************************
     * Callable functions
     *********************************************************************************************/

    fun resetStatus(){
        status = null
    }

    fun darkmodePicked(darkMode: Int){
        DarkModeCenter.setDarkMode(darkMode)
    }

    fun toggleUseIataAirports() = viewModelScope.launch {
        Prefs.useIataAirports(!Prefs.useIataAirports())
    }

    fun toggleRequirePicName() = viewModelScope.launch {
        Prefs.picNameNeedsToBeSet(!Prefs.picNameNeedsToBeSet())
    }

    /**
     * If [Prefs.useCalendarSync] is true, (switch is on) set it to off
     * else, if a type of sync is selected, switch it to on; if it isn't show dialog (which will switch it on on success)
     */
    fun setGetFlightsFromCalendarClicked() = viewModelScope.launch {
        if (!Prefs.useCalendarSync()) { // if it is switched on from being off
            Prefs.calendarDisabledUntil(0)

            // If no calendar sync type chosen, show dialog, else just set it to on
            if (Prefs.calendarSyncType() == CalendarSyncType.CALENDAR_SYNC_NONE)
                status = SettingsActivityStatus.CalendarDialogNeeded
            else
                Prefs.useCalendarSync(true)
        }
        else
            Prefs.useCalendarSync(false)
    }

    fun toggleAutoPostponeCalendarSync() = viewModelScope.launch{
        Prefs.alwaysPostponeCalendarSync(!Prefs.alwaysPostponeCalendarSync())
    }

    fun toggleAddNamesFromRoster() = viewModelScope.launch{
        Prefs.getNamesFromRosters(!Prefs.getNamesFromRosters())
    }

    fun dontPostponeCalendarSync(){
        Prefs.calendarDisabledUntil(0)
    }

    // Pass the ResID of a string to be shown as hint to this function so SettingsActivity can display it.
    fun showHint(hint: Int){
        showHint = hint
    }

    fun hintShown(){
        showHint = null
    }

    private suspend fun calendarIsDisabledNow(disabledUntil: Long) =
        Prefs.useCalendarSync() && disabledUntil > Instant.now().epochSecond
}