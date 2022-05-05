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

import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.utils.Encryption


object Prefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.PREFERENCE_FILE_KEY"
    override val needsMigration = true

    const val USERNAME_NOT_SET = "USERNAME_NOT_SET"
    private const val NO_CALENDAR_SELECTED = ""
    private const val PASSWORD_SHAREDPREF_KEY = "passwordSharedPrefKey"


    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    /**
     * username is the users' username.
     * cannot delegate as that doesn't support null
     */
    private const val USERNAME_RESOURCE = "USERNAME_RESOURCE"
    private var usernameResource: String by JoozdLogSharedPreferenceNotNull(USERNAME_RESOURCE, USERNAME_NOT_SET)
    var username: String?
        get() = usernameIfSet(usernameResource)
        set(it) {
            usernameResource = it ?: USERNAME_NOT_SET
        }
    private val usernameResourceFlow by PrefsFlow(USERNAME_RESOURCE, USERNAME_NOT_SET)
    val usernameFlow = usernameResourceFlow.map {
        usernameIfSet(it)
    }
    suspend fun username() = usernameFlow.first()

    /**
     * password is the users password, hashed to 128 bits
     * NOTE: saved as an md5 hash, cannot retrieve password!
     * @get will return a base64 encoded 128 bit hash, use _key_ for reading the key as bytes
     * @set will save a (base64 encoded) 128 bit MD5 hash to sharedPrefs. This hash will be used to encrypt on server
     */
    var password: String?
        get() = dataStore.getString(PASSWORD_SHAREDPREF_KEY, null)
        set(v) {
            MainScope().launch {
                val encodedPassword = hashAndEncodeToBase64(v)
                dataStore.putString(PASSWORD_SHAREDPREF_KEY, encodedPassword)
                }
            }
    private val passwordFlow = dataStore.data.map { p ->
        p[stringPreferencesKey(PASSWORD_SHAREDPREF_KEY)]}

    private fun hashAndEncodeToBase64(v: String?): String? =
        v?.let {
            Base64.encodeToString(Encryption.md5Hash(it), Base64.DEFAULT)
        }

    //Base64 encoded password
    fun setEncodedPassword(encodedPassword: String) {
        dataStore.putString(PASSWORD_SHAREDPREF_KEY, encodedPassword)
    }

    /**
     * will return bytearray from hashed password
     */
    val key: ByteArray?
        get() =
            password?.let {
                Base64.decode(it, Base64.DEFAULT)
            }
    val keyFlow = passwordFlow.map { p -> p?.let {Base64.decode(it, Base64.DEFAULT) }}
    suspend fun key() = keyFlow.first()

    //Placeholder for new password when changing pass. If app gets killed during password change, this will remain set.
    private const val NEW_PASSWORD = "NEW_PASSWORD"
    var newPassword: String by JoozdLogSharedPreferenceNotNull(NEW_PASSWORD, "")

    private const val LAST_UPDATE_TIME = "LAST_UPDATE_TIME"
    var lastUpdateTime: Long by JoozdLogSharedPreferenceNotNull(LAST_UPDATE_TIME,-1)
    val lastUpdateTimeFlow by PrefsFlow(LAST_UPDATE_TIME,-1)

    private const val SERVER_TIME_OFFSET = "SERVER_TIME_OFFSET"
    var serverTimeOffset: Long by JoozdLogSharedPreferenceNotNull(SERVER_TIME_OFFSET,0)

    private const val AIRPORT_DB_VERSION = "AIRPORT_DB_VERSION"
    var airportDbVersion: Int by JoozdLogSharedPreferenceNotNull(AIRPORT_DB_VERSION,0)

    private const val AIRCRAFT_TYPES_VERSION = "AIRCRAFT_TYPES_VERSION"
    var aircraftTypesVersion: Int by JoozdLogSharedPreferenceNotNull(AIRCRAFT_TYPES_VERSION,0)

    private const val AIRCRAFT_FORCED_TYPES_VERSION = "AIRCRAFT_FORCED_TYPES_VERSION"
    var aircraftForcedVersion: Int by JoozdLogSharedPreferenceNotNull(AIRCRAFT_FORCED_TYPES_VERSION,0)

    /**
     * Amount of days that need to have passed for a notice to be shown
     */
    private const val BACKUP_INTERVAL = "BACKUP_INTERVAL"
    var backupInterval: Int by JoozdLogSharedPreferenceNotNull(BACKUP_INTERVAL,0)
    val backupIntervalFlow by PrefsFlow(BACKUP_INTERVAL, 0)

    private const val BACKUP_FROM_CLOUD = "BACKUP_FROM_CLOUD"
    var backupFromCloud: Boolean by JoozdLogSharedPreferenceNotNull(BACKUP_FROM_CLOUD,false)
    val backupFromCloudFlow by PrefsFlow(BACKUP_FROM_CLOUD, false)
    fun postBackupFromCloud(value: Boolean) = post(BACKUP_FROM_CLOUD, value)

    //Instant epochSeconds of most recent backup
    private const val MOST_RECENT_BACKUP = "MOST_RECENT_BACKUP"
    var mostRecentBackup: Long by JoozdLogSharedPreferenceNotNull(MOST_RECENT_BACKUP,0L)

    private const val NEW_USER_ACT_FINISHED = "NEW_USER_ACT_FINISHED"
    var newUserActivityFinished: Boolean by JoozdLogSharedPreferenceNotNull(NEW_USER_ACT_FINISHED,false)

    private const val EFF_FIRST_USE = "EFF_FIRST_USE"
    var editFlightFragmentWelcomeMessageShouldBeDisplayed: Boolean by JoozdLogSharedPreferenceNotNull(EFF_FIRST_USE,true)

    /***********************
     *   UI preferences:   *
     **********************/

    private const val DARK_MODE = "DARK_MODE"
    var darkMode: Int by JoozdLogSharedPreferenceNotNull(DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    val darkModeFlow by PrefsFlow(DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /**
     * Use ICAO or Iata? True = IATA, false ICAO
     */
    private const val USE_IATA = "USE_IATA"
    var useIataAirports: Boolean by JoozdLogSharedPreferenceNotNull(USE_IATA, false)
    val useIataAirportsFlow by PrefsFlow(USE_IATA, false)

    /**
     * If true, if PIC name is not set, flight will be marked incomplete (red)
     */
    private const val PIC_NAME_NEEDED = "PIC_NAME_NEEDED"
    var picNameNeedsToBeSet: Boolean by JoozdLogSharedPreferenceNotNull(PIC_NAME_NEEDED,true)
    val picNameNeedsToBeSetFlow by PrefsFlow(PIC_NAME_NEEDED, true)

    /**
     * Get planned flights from calendar?
     */
    private const val USE_CAL_SYNC = "USE_CAL_SYNC"
    var useCalendarSync: Boolean by JoozdLogSharedPreferenceNotNull(USE_CAL_SYNC, false)
    val useCalendarSyncFlow by PrefsFlow(USE_CAL_SYNC, false)

    private const val _CALENDAR_SYNC_TYPE = "_CALENDAR_SYNC_TYPE"
    private var calendarSyncTypeValue: Int by JoozdLogSharedPreferenceNotNull(_CALENDAR_SYNC_TYPE,
        CalendarSyncType.CALENDAR_SYNC_NONE.value
    )
    val calendarSyncTypeValueFlow  by PrefsFlow(_CALENDAR_SYNC_TYPE, CalendarSyncType.CALENDAR_SYNC_NONE.value)//  get() = getIntFlowForItem(this::_calendarSyncType.name, CalendarSyncType.CALENDAR_SYNC_NONE.value)

    var calendarSyncType: CalendarSyncType
        get() = makeCalendarSyncType(calendarSyncTypeValue)
        set(it) {
            calendarSyncTypeValue = it.value
        }
    val calendarSyncTypeFlow = calendarSyncTypeValueFlow.map { makeCalendarSyncType(it)}

    private const val CAL_SYNC_ICAL_ADDR = "CAL_SYNC_ICAL_ADDR"
    var calendarSyncIcalAddress: String by JoozdLogSharedPreferenceNotNull(CAL_SYNC_ICAL_ADDR,"")
    val calendarSyncIcalAddressFlow by PrefsFlow(CAL_SYNC_ICAL_ADDR, "")

    private const val NEXT_CAL_CHECK_TIME = "NEXT_CAL_CHECK_TIME"
    var nextCalendarCheckTime: Long by JoozdLogSharedPreferenceNotNull(NEXT_CAL_CHECK_TIME,-1)

    // in epochSeconds
    private const val CAL_DISABLED_UNTIL = "CAL_DISABLED_UNTIL"
    var calendarDisabledUntil: Long by JoozdLogSharedPreferenceNotNull(CAL_DISABLED_UNTIL,0L)
    val calendarDisabledUntilFlow by PrefsFlow(CAL_DISABLED_UNTIL, 0L)

    /**
     * CalendarSync days into the future:
     */
    private const val CAL_SYNC_DAYS = "CAL_SYNC_DAYS"
    var calendarSyncAmountOfDays: Long by JoozdLogSharedPreferenceNotNull(CAL_SYNC_DAYS,30L)

    /**
     * Postpone calendar sync without asking
     */
    private const val CAL_ALWAYS_POSTPONE = "CAL_ALWAYS_POSTPONE"
    var alwaysPostponeCalendarSync: Boolean by JoozdLogSharedPreferenceNotNull(CAL_ALWAYS_POSTPONE,false)
    val alwaysPostponeCalendarSyncFlow by PrefsFlow(CAL_ALWAYS_POSTPONE, false)


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

    /**
     * Add names from rosters?
     */
    private const val GET_NAMES_FROM_ROSTERS = "GET_NAMES_FROM_ROSTERS"
    var getNamesFromRosters: Boolean by JoozdLogSharedPreferenceNotNull(GET_NAMES_FROM_ROSTERS, defaultValue = true)
    val getNamesFromRostersFlow by PrefsFlow(GET_NAMES_FROM_ROSTERS, true)

    /*************************
     * Other settings
     *************************/

    //time to allocate to pilot if flying heavy crew and did takeoff or landing
    private const val STANDARD_TAKEOFF_LANDING_TIMES = "STANDARD_TAKEOFF_LANDING_TIMES"
    var standardTakeoffLandingTimes: Int by JoozdLogSharedPreferenceNotNull(STANDARD_TAKEOFF_LANDING_TIMES,30)
    val standardTakeoffLandingTimesFlow by PrefsFlow(STANDARD_TAKEOFF_LANDING_TIMES, 30)

    //Calendar on device that is used to import flights
    private const val SELECTED_CALENDAR = "SELECTED_CALENDAR"
    var selectedCalendar: String by JoozdLogSharedPreferenceNotNull(SELECTED_CALENDAR, NO_CALENDAR_SELECTED)
    val selectedCalendarFlow by PrefsFlow(SELECTED_CALENDAR, NO_CALENDAR_SELECTED)

    // true if user wants new flights to be marked as IFR. Not sure if I want to use this.
    // var normallyFliesIFR: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    // true if user wants to use cloud -
    private const val USE_CLOUD = "USE_CLOUD"
    var useCloud: Boolean by JoozdLogSharedPreferenceNotNull(USE_CLOUD,false)
    val useCloudFlow by PrefsFlow(USE_CLOUD, false)

    private const val ACCEPTED_CLOUD_TERMS = "ACCEPTED_CLOUD_TERMS"
    var acceptedCloudSyncTerms: Boolean by JoozdLogSharedPreferenceNotNull(ACCEPTED_CLOUD_TERMS, false)

    /**
     * Small things being saved:
     */

    // If feedback could not be sent to server, save it for the next time
    // TODO handle this with a worker
    private const val FEEDBACK_WAITING = "FEEDBACK_WAITING"
    var feedbackWaiting: String by JoozdLogSharedPreferenceNotNull(FEEDBACK_WAITING, "")


    /**
     * Login link waiting for server to be available
     */
    private const val LOGIN_LINK_STRING_WAITING = "LOGIN_LINK_STRING_WAITING"
    var loginLinkStringWaiting: String by JoozdLogSharedPreferenceNotNull(LOGIN_LINK_STRING_WAITING, "")
    val loginLinkStringWaitingFlow by PrefsFlow(LOGIN_LINK_STRING_WAITING, "")
    fun pushLoginLinkStringWaiting(value: String) = post(LOGIN_LINK_STRING_WAITING, value)


    private fun usernameIfSet(name: String) = name.takeIf { usernameResource != USERNAME_NOT_SET }

    private fun makeCalendarSyncType(type: Int): CalendarSyncType =
        CalendarSyncType.fromInt(type) ?: CalendarSyncType.CALENDAR_SYNC_NONE
}


