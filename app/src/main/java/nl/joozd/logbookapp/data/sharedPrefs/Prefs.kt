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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.utils.Encryption


object Prefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.PREFERENCE_FILE_KEY"
    override val needsMigration = true

    const val USERNAME_NOT_SET = "USERNAME_NOT_SET"
    private const val NO_CALENDAR_SELECTED = ""
    private const val PASSWORD_SHAREDPREF_KEY = "passwordSharedPrefKey"

    /**
     * username is the users' username.
     * cannot delegate as that doesn't support null
     */
    private var usernameResource: String by JoozdLogSharedPreference(USERNAME_NOT_SET)
    var username: String?
        get() = usernameIfSet(usernameResource)
        set(it) {
            usernameResource = it ?: USERNAME_NOT_SET
        }
    private val usernameResourceFlow by PrefsFlow(usernameResource, USERNAME_NOT_SET)// get() = getStringFlowForItem(this::usernameResource.name, USERNAME_NOT_SET)
    val usernameFlow = usernameResourceFlow.map {
        usernameIfSet(it)
    }

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

    private fun hashAndEncodeToBase64(v: String?): String? =
        v?.let {
            Base64.encodeToString(Encryption.md5Hash(it), Base64.DEFAULT)
        }


    //Base64 encoded password
    fun forcePassword(encodedPassword: String) {
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

    var emailAddress: String by JoozdLogSharedPreference("")
    val emailAddressFlow by PrefsFlow(emailAddress, "")

    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    var emailVerified: Boolean by JoozdLogSharedPreference(false)
    val emailVerifiedFlow by PrefsFlow(emailVerified, false)

    /**
     * a list of jobs waiting for email confirmation
     * Parse this into an [EmailJobsWaiting] object
     */
    var emailJobsWaitingInt: Int by JoozdLogSharedPreference(0)

    val emailJobsWaiting: EmailJobsWaiting = EmailJobsWaiting(emailJobsWaitingInt)

    //Placeholder for new password when changing pass. If app gets killed during password change, this will remain set.
    var newPassword: String by JoozdLogSharedPreference("")

    var lastUpdateTime: Long by JoozdLogSharedPreference(-1)
    val lastUpdateTimeFlow by PrefsFlow(lastUpdateTime-1)

    var serverTimeOffset: Long by JoozdLogSharedPreference(0)

    var airportDbVersion: Int by JoozdLogSharedPreference(0)

    var aircraftTypesVersion: Int by JoozdLogSharedPreference(0)

    var aircraftForcedVersion: Int by JoozdLogSharedPreference(0)

    /**
     * Amount of days that need to have passed for a notice to be shown
     */
    var backupInterval: Int by JoozdLogSharedPreference(0)
    val backupIntervalFlow by PrefsFlow(backupInterval, 0)

    var backupFromCloud: Boolean by JoozdLogSharedPreference(false)
    val backupFromCloudFlow by PrefsFlow(backupFromCloud, false)

    //Instant epochSeconds of most recent backup
    var mostRecentBackup: Long by JoozdLogSharedPreference(0L)

    var newUserActivityFinished: Boolean by JoozdLogSharedPreference(false)

    var editFlightFragmentWelcomeMessageShouldBeDisplayed: Boolean by JoozdLogSharedPreference(true)

    /**
     * Errors to be shown, bit flags. @see scheduleErrorNotification
     */
    var errorsToBeShown: Long by JoozdLogSharedPreference(0L)


    /***********************
     *   UI preferences:   *
     **********************/

    var darkMode: Int by JoozdLogSharedPreference(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /**
     * Use ICAO or Iata? True = IATA, false ICAO
     */
    var useIataAirports: Boolean by JoozdLogSharedPreference(false)
    val useIataAirportsFlow by PrefsFlow(useIataAirports, false)

    /**
     * If true, if PIC name is not set, flight will be marked incomplete (red)
     */
    var picNameNeedsToBeSet: Boolean by JoozdLogSharedPreference(true)
    val picNameNeedsToBeSetFlow by PrefsFlow(picNameNeedsToBeSet, true)

    /**
     * Get planned flights from calendar?
     */
    var useCalendarSync: Boolean by JoozdLogSharedPreference(false)
    val useCalendarSyncFlow by PrefsFlow(useCalendarSync, false)

    private var _calendarSyncType: Int by JoozdLogSharedPreference(
        CalendarSyncType.CALENDAR_SYNC_NONE.value
    )
    val _calendarSyncTypeFlow  by PrefsFlow(_calendarSyncType, CalendarSyncType.CALENDAR_SYNC_NONE.value)//  get() = getIntFlowForItem(this::_calendarSyncType.name, CalendarSyncType.CALENDAR_SYNC_NONE.value)

    var calendarSyncType: CalendarSyncType
        get() = makeCalendarSyncType(_calendarSyncType)
        set(it) {
            _calendarSyncType = it.value
        }
    val calendarSyncTypeFlow = _calendarSyncTypeFlow.map { makeCalendarSyncType(it)}

    var calendarSyncIcalAddress: String by JoozdLogSharedPreference("")
    val calendarSyncIcalAddressFlow by PrefsFlow(calendarSyncIcalAddress, "")

    var nextCalendarCheckTime: Long by JoozdLogSharedPreference(-1)

    // in epochSeconds
    var calendarDisabledUntil: Long by JoozdLogSharedPreference(0)
    val calendarDisabledUntilFlow by PrefsFlow(calendarDisabledUntil, 0)

    /**
     * CalendarSync days into the future:
     */
    var calendarSyncAmountOfDays: Long by JoozdLogSharedPreference(30L)

    /**
     * Postpone calendar sync without asking
     */
    var alwaysPostponeCalendarSync: Boolean by JoozdLogSharedPreference(false)
    val alwaysPostponeCalendarSyncFlow by PrefsFlow(alwaysPostponeCalendarSync, false)


    /**
     * Accept aircraft change from Monthly Overview without confirmation?
     */
    var updateAircraftWithoutAsking: Boolean by JoozdLogSharedPreference(true)

    /**
     * Max time difference before montly/actual becomes a conflict (in minutes)
     */
    var maxChronoAdjustment: Int by JoozdLogSharedPreference(180)

    /**
     * Add names from rosters?
     */
    var getNamesFromRosters: Boolean by JoozdLogSharedPreference(defaultValue = true)
    val getNamesFromRostersFlow by PrefsFlow(getNamesFromRosters, true)

    /*************************
     * Other settings
     *************************/

    //time to allocate to pilot if flying heavy crew and did takeoff or landing
    var standardTakeoffLandingTimes: Int by JoozdLogSharedPreference(30)
    val standardTakeoffLandingTimesFlow by PrefsFlow(standardTakeoffLandingTimes, 30)

    //Calendar on device that is used to import flights
    var selectedCalendar: String by JoozdLogSharedPreference(NO_CALENDAR_SELECTED)
    val selectedCalendarFlow by PrefsFlow(selectedCalendar, NO_CALENDAR_SELECTED)

    // true if user wants new flights to be marked as IFR. Not sure if I want to use this.
    // var normallyFliesIFR: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    // true if user wants to use cloud -
    var useCloud: Boolean by JoozdLogSharedPreference(false)
    val useCloudFlow by PrefsFlow(useCloud, false)

    var acceptedCloudSyncTerms: Boolean by JoozdLogSharedPreference(false)

    /**
     * Small things being saved:
     */

    // If feedback could not be sent to server, save it for the next time
    // TODO handle this with a worker
    var feedbackWaiting: String by JoozdLogSharedPreference("")

    /**
     * Email confirmation string waiting for a network connection. Handled by [nl.joozd.logbookapp.workmanager.ConfirmEmailWorker]
     */
    var emailConfirmationStringWaiting: String by JoozdLogSharedPreference("")

    /**
     * Login link waiting for server to be available
     */
    var loginLinkStringWaiting: String by JoozdLogSharedPreference("")


    private fun usernameIfSet(name: String) = name.takeIf { usernameResource != USERNAME_NOT_SET }

    private fun makeCalendarSyncType(type: Int): CalendarSyncType =
        CalendarSyncType.fromInt(type) ?: CalendarSyncType.CALENDAR_SYNC_NONE
}


