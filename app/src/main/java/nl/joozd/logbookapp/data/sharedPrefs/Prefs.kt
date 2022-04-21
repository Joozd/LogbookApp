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

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.utils.Encryption


object Prefs {
    private const val USER_PREFERENCES_FILE_KEY = "nl.joozd.logbookapp.PREFERENCE_FILE_KEY"
    const val USERNAME_NOT_SET = "USERNAME_NOT_SET"
    private const val NO_CALENDAR_SELECTED = ""
    private const val PASSWORD_SHAREDPREF_KEY = "passwordSharedPrefKey"

    private val dataStore by lazy {
        App.instance.ctx.dataStore
    }

    private val Context.dataStore by preferencesDataStore(
        name = USER_PREFERENCES_FILE_KEY
    )


    /**
     * username is the users' username.
     * cannot delegate as that doesn't support null
     */


    private var usernameResource: String by JoozdLogSharedPrefs(dataStore, USERNAME_NOT_SET)
    var username: String?
        get() = usernameIfSet(usernameResource)
        set(it) {
            usernameResource = it ?: USERNAME_NOT_SET
        }
    private val usernameResourceFlow get() = getStringFlowForItem(this::usernameResource.name, USERNAME_NOT_SET)
    val usernameFlow = usernameResourceFlow.map {
        usernameIfSet(it!!)
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

    var emailAddress: String by JoozdLogSharedPrefs(dataStore, "")
    val emailAddressFlow get() = getStringFlowForItem(this::emailAddress.name, "")

    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    var emailVerified: Boolean by JoozdLogSharedPrefs(dataStore, false)
    val emailVerifiedFlow get() = getBooleanFlowForItem(this::emailVerified.name, false)

    /**
     * a list of jobs waiting for email confirmation
     * Parse this into an [EmailJobsWaiting] object
     */
    var emailJobsWaitingInt: Int by JoozdLogSharedPrefs(dataStore, 0)

    val emailJobsWaiting: EmailJobsWaiting = EmailJobsWaiting(emailJobsWaitingInt)

    //Placeholder for new password when changing pass. If app gets killed during password change, this will remain set.
    var newPassword: String by JoozdLogSharedPrefs(dataStore, "")

    var lastUpdateTime: Long by JoozdLogSharedPrefs(dataStore, -1)
    val lastUpdateTimeFlow get() = getLongFlowForItem(this::lastUpdateTime.name, -1)

    var serverTimeOffset: Long by JoozdLogSharedPrefs(dataStore, 0)

    var airportDbVersion: Int by JoozdLogSharedPrefs(dataStore, 0)

    var aircraftTypesVersion: Int by JoozdLogSharedPrefs(dataStore, 0)

    var aircraftForcedVersion: Int by JoozdLogSharedPrefs(dataStore, 0)

    /**
     * Amount of days that need to have passed for a notice to be shown
     */
    var backupInterval: Int by JoozdLogSharedPrefs(dataStore, 0)
    val backupIntervalFlow get() = getIntFlowForItem(this::backupInterval.name, 0)

    var backupFromCloud: Boolean by JoozdLogSharedPrefs(dataStore, false)
    val backupFromCloudFlow get() = getBooleanFlowForItem(this::backupFromCloud.name, false)

    //Instant epochSeconds of most recent backup
    var mostRecentBackup: Long by JoozdLogSharedPrefs(dataStore, 0L)

    var newUserActivityFinished: Boolean by JoozdLogSharedPrefs(dataStore, false)

    var editFlightFragmentWelcomeMessageShouldBeDisplayed: Boolean by JoozdLogSharedPrefs(
        dataStore,
        true
    )

    /**
     * Errors to be shown, bit flags. @see scheduleErrorNotification
     */
    var errorsToBeShown: Long by JoozdLogSharedPrefs(dataStore, 0L)


    /***********************
     *   UI preferences:   *
     **********************/

    var darkMode: Int by JoozdLogSharedPrefs(dataStore, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /**
     * Use ICAO or Iata? True = IATA, false ICAO
     */
    var useIataAirports: Boolean by JoozdLogSharedPrefs(dataStore, false)
    val useIataAirportsFlow get() = getBooleanFlowForItem(this::useIataAirports.name, false)

    /**
     * If true, if PIC name is not set, flight will be marked incomplete (red)
     */
    var picNameNeedsToBeSet: Boolean by JoozdLogSharedPrefs(dataStore, true)
    val picNameNeedsToBeSetFlow get() = getBooleanFlowForItem(this::picNameNeedsToBeSet.name, true)

    /**
     * Get planned flights from calendar?
     */
    var useCalendarSync: Boolean by JoozdLogSharedPrefs(dataStore, false)
    val useCalendarSyncFlow get() = getBooleanFlowForItem(this::useCalendarSync.name, false)

    private var _calendarSyncType: Int by JoozdLogSharedPrefs(
        dataStore,
        CalendarSyncType.CALENDAR_SYNC_NONE.value
    )
    val _calendarSyncTypeFlow get() = getIntFlowForItem(this::_calendarSyncType.name, CalendarSyncType.CALENDAR_SYNC_NONE.value)

    var calendarSyncType: CalendarSyncType
        get() = makeCalendarSyncType(_calendarSyncType)
        set(it) {
            _calendarSyncType = it.value
        }
    val calendarSyncTypeFlow = _calendarSyncTypeFlow.map { makeCalendarSyncType(it!!)}

    var calendarSyncIcalAddress: String by JoozdLogSharedPrefs(dataStore, "")
    val calendarSyncIcalAddressFlow get() = getStringFlowForItem(this::calendarSyncIcalAddress.name, "")

    var nextCalendarCheckTime: Long by JoozdLogSharedPrefs(dataStore, -1)

    // in epochSeconds
    var calendarDisabledUntil: Long by JoozdLogSharedPrefs(dataStore, 0)
    val calendarDisabledUntilFlow get() = getLongFlowForItem(this::calendarDisabledUntil.name, 0)

    /**
     * CalendarSync days into the future:
     */
    var calendarSyncAmountOfDays: Long by JoozdLogSharedPrefs(dataStore, 30L)

    /**
     * Postpone calendar sync without asking
     */
    var alwaysPostponeCalendarSync: Boolean by JoozdLogSharedPrefs(dataStore, false)
    val alwaysPostponeCalendarSyncFlow get() = getBooleanFlowForItem(this::alwaysPostponeCalendarSync.name, false)


    /**
     * Accept aircraft change from Monthly Overview without confirmation?
     */
    var updateAircraftWithoutAsking: Boolean by JoozdLogSharedPrefs(dataStore, true)

    /**
     * Max time difference before montly/actual becomes a conflict (in minutes)
     */
    var maxChronoAdjustment: Int by JoozdLogSharedPrefs(dataStore, 180)

    /**
     * Add names from rosters?
     */
    var getNamesFromRosters: Boolean by JoozdLogSharedPrefs(dataStore, defaultValue = true)
    val getNamesFromRostersFlow get() = getBooleanFlowForItem(this::getNamesFromRosters.name, true)

    /*************************
     * Other settings
     *************************/

    //time to allocate to pilot if flying heavy crew and did takeoff or landing
    var standardTakeoffLandingTimes: Int by JoozdLogSharedPrefs(dataStore, 30)
    val standardTakeoffLandingTimesFlow get() = getIntFlowForItem(this::standardTakeoffLandingTimes.name, 30)

    //Calendar on device that is used to import flights
    var selectedCalendar: String by JoozdLogSharedPrefs(dataStore, NO_CALENDAR_SELECTED)
    val selectedCalendarFlow get() = getStringFlowForItem(this::selectedCalendar.name, NO_CALENDAR_SELECTED)

    // true if user wants new flights to be marked as IFR. Not sure if I want to use this.
    // var normallyFliesIFR: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    // true if user wants to use cloud -
    var useCloud: Boolean by JoozdLogSharedPrefs(dataStore, false)
    val useCloudFlow get() = getBooleanFlowForItem(this::useCloud.name, false)

    var acceptedCloudSyncTerms: Boolean by JoozdLogSharedPrefs(dataStore, false)

    /**
     * Small things being saved:
     */

    // If feedback could not be sent to server, save it for the next time
    // TODO handle this with a worker
    var feedbackWaiting: String by JoozdLogSharedPrefs(dataStore, "")

    /**
     * Email confirmation string waiting for a network connection. Handled by [nl.joozd.logbookapp.workmanager.ConfirmEmailWorker]
     */
    var emailConfirmationStringWaiting: String by JoozdLogSharedPrefs(dataStore, "")

    /**
     * Login link waiting for server to be available
     */
    var loginLinkStringWaiting: String by JoozdLogSharedPrefs(dataStore, "")


    private fun usernameIfSet(name: String) = name.takeIf { usernameResource == USERNAME_NOT_SET }

    private fun makeCalendarSyncType(type: Int): CalendarSyncType =
        CalendarSyncType.fromInt(type) ?: CalendarSyncType.CALENDAR_SYNC_NONE

    private fun getBooleanFlowForItem(itemName: String, defaultValue: Boolean? = null): Flow<Boolean?> = dataStore.data.map { p ->
        println("Get boolean flow for item $itemName")
        p[booleanPreferencesKey(itemName)] ?: defaultValue
    }

    private fun getIntFlowForItem(itemName: String, defaultValue: Int? = null): Flow<Int?> = dataStore.data.map { p ->
        println("Get Int flow for item $itemName")
        p[intPreferencesKey(itemName)] ?: defaultValue
    }

    private fun getLongFlowForItem(itemName: String, defaultValue: Long? = null): Flow<Long?> = dataStore.data.map { p ->
        println("Get Long flow for item $itemName")
        p[longPreferencesKey(itemName)] ?: defaultValue
    }

    private fun getFloatFlowForItem(itemName: String, defaultValue: Float? = null): Flow<Float?> = dataStore.data.map { p ->
        println("Get Float flow for item $itemName")
        p[floatPreferencesKey(itemName)] ?: defaultValue
    }

    private fun getStringFlowForItem(itemName: String, defaultValue: String? = null): Flow<String?> = dataStore.data.map { p ->
        println("Get String flow for item $itemName")
        p[stringPreferencesKey(itemName)] ?: defaultValue
    }


}


