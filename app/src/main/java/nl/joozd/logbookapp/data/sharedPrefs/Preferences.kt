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
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.utils.Encryption

@Suppress("ObjectPropertyName")
object Preferences {
    private val sharedPref by lazy{
        with (App.instance.ctx) {
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        }
    }

    fun getSharedPreferences(): SharedPreferences = with (App.instance.ctx) {
        getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }


    /**
     * username is the users' username.
     * cannot delegate as that doesn't support null
     */

    const val USERNAME_NOT_SET = "USERNAME_NOT_SET"
    var usernameResource: String by JoozdLogSharedPrefs(sharedPref, USERNAME_NOT_SET)
    var username: String?
        get() = if (usernameResource == USERNAME_NOT_SET) null else usernameResource
        set(it) {
            usernameResource = it ?: USERNAME_NOT_SET
            Log.d("Preferences", "Name set to $it")
        }

    /**
     * password is the users password, hashed to 128 bits
     * @get will return a base64 encoded 128 bit hash, use _key_ for reading the key as bytes
     * @set will save a (base64 encoded) 128 bit MD5 hash to sharedPrefs. This hash will be used to encrypt on server
     */
    private const val PASSWORD_SHAREDPREF_KEY = "passwordSharedPrefKey" // saved as md5 hash, cannot retrieve password!
    var password: String?
        get() = sharedPref.getString(PASSWORD_SHAREDPREF_KEY,null)
        set(v) = if (v==null) {
            with(sharedPref.edit()) {
                putString(PASSWORD_SHAREDPREF_KEY, null)
                apply()
            }
        } else {
            with(sharedPref.edit()) {
                val encodedPassword = Encryption.md5Hash(v)
                putString(PASSWORD_SHAREDPREF_KEY, Base64.encodeToString(encodedPassword, Base64.DEFAULT))
                apply()
            }
        }

    //TODO for etsting and debugging
    //Base64 encoded pass
    fun forcePassword(encodedPassword: String){
        with(sharedPref.edit()) {
            putString(PASSWORD_SHAREDPREF_KEY, encodedPassword)
            apply()
        }
    }

    /**
     * will return bytearray from hashed password
     */
    val key: ByteArray?
    get() =
        password?.let {
            Base64.decode(it, Base64.DEFAULT)
        }

    var emailAddress: String by JoozdLogSharedPrefs(sharedPref, "")

    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    var emailVerified: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    /**
     * a list of jobs waiting for email confirmation
     * Parse this into an [EmailJobsWaiting] object
     */
    var _emailJobsWaiting: Int by JoozdLogSharedPrefs(sharedPref, 0)

    val emailJobsWaiting: EmailJobsWaiting = EmailJobsWaiting(_emailJobsWaiting)

    //Placeholder for new password when changing pass. If app gets killed during password change, this will remain set.
    var newPassword: String by JoozdLogSharedPrefs(sharedPref, "")

    var lastUpdateTime: Long by JoozdLogSharedPrefs(sharedPref, -1)

    var serverTimeOffset: Long by JoozdLogSharedPrefs(sharedPref, 0)

    var airportUpdateTimestamp: Long by JoozdLogSharedPrefs(sharedPref, -1)

    var airportDbVersion: Int by JoozdLogSharedPrefs(sharedPref, 0)

    var aircraftTypesVersion: Int by JoozdLogSharedPrefs(sharedPref, 0)

    var aircraftForcedVersion: Int by JoozdLogSharedPrefs(sharedPref, 0)

    /**
     * Amount of days that need to have passed for a notice to be shown
     */
    private val _backupInterval = MutableLiveData<Int>()
    val backupIntervalLiveData: LiveData<Int>
        get() = _backupInterval
    var backupInterval: Int by JoozdLogSharedPrefs(sharedPref, 0, _backupInterval)

    var backupFromCloud: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    //Instant epochseconds of most recent backup
    private val _mostRecentBackup = MutableLiveData<Long>()
    val mostRecentBackupLiveData: LiveData<Long>
        get() = _mostRecentBackup
    var mostRecentBackup: Long by JoozdLogSharedPrefs(sharedPref, 0L, _mostRecentBackup)

    var updateLargerFilesOverWifiOnly: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    var newUserActivityFinished: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    var editFlightFragmentWelcomeMessageShouldBeDisplayed: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    /**
     * Errors to be shown, bit flags. @see scheduleErrorNotification
     */
    var errorsToBeShown: Long by JoozdLogSharedPrefs(sharedPref, 0L)


    /***********************
     *   UI preferences:   *
     **********************/

    var darkMode: Int by JoozdLogSharedPrefs(sharedPref, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /**
     * Use ICAO or Iata? True = IATA, false ICAO
     */
    var useIataAirports: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    /**
     * Opt-in for Aircraft Type Consensus
     */
    @Suppress("ObjectPropertyName")
    private val _consensusOptIn = MutableLiveData<Boolean>()
    val consensusOptInLiveData: LiveData<Boolean>
        get() = _consensusOptIn
    var consensusOptIn: Boolean by JoozdLogSharedPrefs(sharedPref, true, _consensusOptIn)

    /**
     * If true, if PIC name is not set, flight will be marked incomplete (red)
     */
    @Suppress("ObjectPropertyName")
    private val _picNameNeedsToBeSet = MutableLiveData<Boolean>()
    val picNameNeedsToBeSetLiveData: LiveData<Boolean>
        get() = _picNameNeedsToBeSet
    var picNameNeedsToBeSet: Boolean by JoozdLogSharedPrefs(sharedPref, true, _picNameNeedsToBeSet)

    /**
     * Get planned flights from calendar?
     */
    var useCalendarSync: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    var calendarSyncType: Int by JoozdLogSharedPrefs(sharedPref, CalendarSyncTypes.CALENDAR_SYNC_NONE)

    var calendarSyncIcalAddress: String by JoozdLogSharedPrefs(sharedPref, "")

    var nextCalendarCheckTime: Long by JoozdLogSharedPrefs(sharedPref, -1)

    var calendarDisabledUntil: Long by JoozdLogSharedPrefs(sharedPref, 0)

    /**
     * CalendarSync days into the future:
     */
    var calendarSyncAmountOfDays: Long by JoozdLogSharedPrefs(sharedPref, 30L)

    /**
     * Postpone calendar sync without asking
     */
    var alwaysPostponeCalendarSync: Boolean by JoozdLogSharedPrefs(sharedPref, false)


    /**
     * Accept aircraft change from Monthly Overview without confirmation?
     */
    var updateAircraftWithoutAsking: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    /**
     * Max time difference before montly/actual becomes a conflict (in minutes)
     */
    var maxChronoAdjustment: Int by JoozdLogSharedPrefs(sharedPref, 180)

    var showOldTimesOnChronoUpdate: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    /**
     * Add names from rosters?
     */
    var getNamesFromRosters: Boolean by JoozdLogSharedPrefs(sharedPref, defaultValue = true)

    /*************************
     * Other settings
     *************************/

    //time to allocate to pilot if flying heavy crew and did takeoff or landing
    var standardTakeoffLandingTimes: Int by JoozdLogSharedPrefs(sharedPref, 30)

    //Standard amount of time entered when a user makes a flight sim
    var standardSimDuration: Int by JoozdLogSharedPrefs(sharedPref, 210)

    //Calendar on device that is used to import flights
    const val NO_CALENDAR_SELECTED = ""
    var selectedCalendar: String by JoozdLogSharedPrefs(sharedPref, NO_CALENDAR_SELECTED)

    // true if user wants new flights to be marked as IFR. Not sure if I want to use this.
    // var normallyFliesIFR: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    // true if user wants to use cloud -
    var useCloud: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    var acceptedCloudSyncTerms: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    /**
     * Small things being saved:
     */

    // If feedback could not be sent to server, save it for the next time
    // TODO handle this with a worker
    var feedbackWaiting: String by JoozdLogSharedPrefs(sharedPref, "")

    /**
     * Email confirmation string waiting for a network connection. Handled by [nl.joozd.logbookapp.workmanager.ConfirmEmailWorker]
     */
    var emailConfirmationStringWaiting: String by JoozdLogSharedPrefs(sharedPref, "")

    /**
     * Login link waiting for server to be available
     */
    var loginLinkStringWaiting: String by JoozdLogSharedPrefs(sharedPref, "")

    /**
     * Hardcoded global const values
     */
    const val MIN_CALENDAR_CHECK_INTERVAL = 30L // seconds
}