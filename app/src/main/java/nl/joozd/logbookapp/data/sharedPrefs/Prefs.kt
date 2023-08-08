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

package nl.joozd.logbookapp.data.sharedPrefs

import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.first
import nl.joozd.logbookapp.core.metadata.Version
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate


object Prefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.PREFERENCE_FILE_KEY"

    private const val USERNAME_NOT_SET = "USERNAME_NOT_SET"
    private const val NO_CALENDAR_SELECTED = ""

    private const val GET_NAMES_FROM_ROSTERS = "GET_NAMES_FROM_ROSTERS"
    private const val STANDARD_TAKEOFF_LANDING_TIMES = "STANDARD_TAKEOFF_LANDING_TIMES"
    private const val SELECTED_CALENDAR = "SELECTED_CALENDAR"


    //TODO remove this after 01-05-2023
    private const val USERNAME_RESOURCE = "USERNAME_RESOURCE"
    private var usernameResource: String by JoozdLogSharedPreferenceNotNull(USERNAME_RESOURCE, USERNAME_NOT_SET)
    private val usernameResourceFlow by PrefsFlow(USERNAME_RESOURCE, USERNAME_NOT_SET)

    suspend fun username() = usernameIfSet(usernameResourceFlow.first())   // usernameFlow gives null if USERNAME_NOT_SET
    fun username(newName: String?) = post(USERNAME_RESOURCE, newName ?: USERNAME_NOT_SET)

    private fun usernameIfSet(name: String) = name.takeIf { usernameResource != USERNAME_NOT_SET }
    //TODO remove until here


    /**
     * Amount of days that need to have passed for a notice to be shown
     */
    private const val BACKUP_INTERVAL = "BACKUP_INTERVAL"
    private const val BACKUP_FROM_CLOUD = "BACKUP_FROM_CLOUD"

    // Number of days after which to send an email or a reminder
    // 0 means don't remind me to make backups, don't send me emails
    val backupInterval by JoozdlogSharedPreferenceDelegate(BACKUP_INTERVAL,14)
    val sendBackupEmails by JoozdlogSharedPreferenceDelegate(BACKUP_FROM_CLOUD,false)

    private const val NEW_USER_ACTIVITY_FINISHED = "NEW_USER_ACT_FINISHED"
    private const val PREVIOUS_VERSION = "PREVIOUS_VERSION"

    //Replaced by
    val newUserActivityFinished by JoozdlogSharedPreferenceDelegate(NEW_USER_ACTIVITY_FINISHED,false)

    val configuredVersion by JoozdlogSharedPreferenceDelegate(PREVIOUS_VERSION, Version.NEW_INSTALL)

    /***********************
     *   UI preferences:   *
     **********************/

    private const val DARK_MODE = "DARK_MODE"
    private const val USE_IATA = "USE_IATA"
    private const val PIC_NAME_NEEDED = "PIC_NAME_NEEDED"

    private const val REPLACE_OWN_NAME_WITH_SELF = "REPLACE_OWN_NAME_WITH_SELF"
    private const val OWN_NAME = "OWN_NAME"

    private const val USE_CAL_SYNC = "USE_CAL_SYNC"
    private const val CALENDAR_SYNC_TYPE = "_CALENDAR_SYNC_TYPE"
    private const val CAL_SYNC_ICAL_ADDR = "CAL_SYNC_ICAL_ADDR"
    private const val NEXT_CAL_CHECK_TIME = "NEXT_CAL_CHECK_TIME"
    private const val CAL_DISABLED_UNTIL = "CAL_DISABLED_UNTIL"
    private const val DEFAULT_MULTICREW_MODE_IS_FIXED_REST = "DEFAULT_MULTICREW_MODE_IS_FIXED_REST"

    val darkMode by JoozdlogSharedPreferenceDelegate(DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    val useIataAirports by JoozdlogSharedPreferenceDelegate(USE_IATA, false)
    val picNameNeedsToBeSet by JoozdlogSharedPreferenceDelegate(PIC_NAME_NEEDED,true)
    val replaceOwnNameWithSelf by JoozdlogSharedPreferenceDelegate(REPLACE_OWN_NAME_WITH_SELF,true)
    val ownName by JoozdlogSharedPreferenceDelegate(OWN_NAME,"")

    val useCalendarSync by JoozdlogSharedPreferenceDelegate(USE_CAL_SYNC, false)

    private val calendarSyncTypeValue by JoozdlogSharedPreferenceDelegate(CALENDAR_SYNC_TYPE, CalendarSyncType.CALENDAR_SYNC_NONE.value)
    var calendarSyncType = calendarSyncTypeValue.mapBothWays(object : JoozdlogSharedPreferenceDelegate.PrefTransformer<Int, CalendarSyncType>{
        override fun map(source: Int): CalendarSyncType = makeCalendarSyncType(source)
        override fun mapBack(transformedValue: CalendarSyncType): Int = transformedValue.value
    })

    private fun makeCalendarSyncType(type: Int): CalendarSyncType =
        CalendarSyncType.fromInt(type) ?: CalendarSyncType.CALENDAR_SYNC_NONE

    val calendarSyncIcalAddress by JoozdlogSharedPreferenceDelegate(CAL_SYNC_ICAL_ADDR,"")
    val nextCalendarCheckTime by JoozdlogSharedPreferenceDelegate(NEXT_CAL_CHECK_TIME,-1)
    val calendarDisabledUntil by JoozdlogSharedPreferenceDelegate(CAL_DISABLED_UNTIL,0L) // in epochSeconds
    val defaultMultiCrewModeIsFixedRest by JoozdlogSharedPreferenceDelegate(DEFAULT_MULTICREW_MODE_IS_FIXED_REST, false)


    /**
     * CalendarSync days into the future:
     */
    private const val CAL_SYNC_DAYS = "CAL_SYNC_DAYS"
    var calendarSyncAmountOfDays: Long by JoozdLogSharedPreferenceNotNull(CAL_SYNC_DAYS,30L)

    /**
     * Postpone calendar sync without asking
     */
    private const val CAL_ALWAYS_POSTPONE = "CAL_ALWAYS_POSTPONE"
    val alwaysPostponeCalendarSync by JoozdlogSharedPreferenceDelegate(CAL_ALWAYS_POSTPONE,false)

    /**
     * Accept aircraft change from Monthly Overview without confirmation?
     */
    private const val UPDATE_AIRCRAFT_WITHOUT_ASKING = "UPDATE_AIRCRAFT_WITHOUT_ASKING"
    var updateAircraftWithoutAsking: Boolean by JoozdLogSharedPreferenceNotNull(UPDATE_AIRCRAFT_WITHOUT_ASKING,true)

    /**
     * Max time difference before montly/actual becomes a conflict (in minutes)
     */
    private const val MAX_CHRONO_DIFF = "MAX_CHRONO_DIFF"
    var maxChronoAdjustment: Int by JoozdLogSharedPreferenceNotNull(MAX_CHRONO_DIFF,180)

    val getNamesFromRosters by JoozdlogSharedPreferenceDelegate(GET_NAMES_FROM_ROSTERS, defaultValue = true)
    val augmentedTakeoffLandingTimes by JoozdlogSharedPreferenceDelegate(STANDARD_TAKEOFF_LANDING_TIMES,30) //time to allocate to pilot if flying heavy crew and did takeoff or landing
    val selectedCalendar by JoozdlogSharedPreferenceDelegate(SELECTED_CALENDAR, NO_CALENDAR_SELECTED) //Calendar on device that is used to import flights



}


