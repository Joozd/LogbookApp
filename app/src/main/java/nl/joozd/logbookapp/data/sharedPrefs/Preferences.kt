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

package nl.joozd.logbookapp.data.sharedPrefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.calendar.dataclasses.SupportedCalendarTypes
import java.security.MessageDigest

object Preferences {
    private const val USERNAME = "username"
    private const val PASSWORD = "password" // saved as md5 hash, cannot retrieve password!
    private const val NO_CALENDAR_SELECTED = ""

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
    var username: String by JoozdLogSharedPrefs(sharedPref, USERNAME_NOT_SET)

    /**
     * password is the users password, hashed to 128 bits
     * @get will return a base64 encoded 128 bit hash, use _key_ for reading the key
     * @set will save an MD5 hash to sharedPrefs. This hash will be used to encrypt on server
     */
    var password: String?
        get() = sharedPref.getString(PASSWORD,null)
        set(v) = if (v==null) {
            with(sharedPref.edit()) {
                putString(PASSWORD, v)
                apply()
            }
        } else {
            with(sharedPref.edit()) {
                val encodedPassword = with (MessageDigest.getInstance("MD5")){
                    update(v.toByteArray())
                    digest()
                }
                putString(PASSWORD, Base64.encodeToString(encodedPassword, Base64.DEFAULT))
                apply()
            }
        }

    /**
     * will return bytearray from hashed password
     */
    val key: ByteArray?
    get() {
        password?.let {
            return Base64.decode(it, Base64.DEFAULT)
        }
        return null
    }

    var lastUpdateTime: Long by JoozdLogSharedPrefs(sharedPref, -1)

    var lastCalendarCheckTime: Long by JoozdLogSharedPrefs(sharedPref, -1)

    var calendarDisabledUntil: Long by JoozdLogSharedPrefs(sharedPref, 0)

    var serverTimeOffset: Long by JoozdLogSharedPrefs(sharedPref, 0)

    var airportUpdateTimestamp: Long by JoozdLogSharedPrefs(sharedPref, -1)

    var airportDbVersion: Int by JoozdLogSharedPrefs(sharedPref, 0)

    var aircraftTypesVersion: Int by JoozdLogSharedPrefs(sharedPref, 0)

    var aircraftForcedVersion: Int by JoozdLogSharedPrefs(sharedPref, 0)

    var updateLargerFilesOverWifiOnly: Boolean by JoozdLogSharedPrefs(sharedPref, true)


    /***********************
     *   UI preferences:   *
     **********************/

    /**
     * Use ICAO or Iata? True = IATA, false ICAO
     */
    var useIataAirports: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    /**
     * Get planned flights from calendar?
     */
    var getFlightsFromCalendar: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    /**
     * Type of calendar used
     * @see SupportedCalendarTypes for supported types and their codes
     */
    var calendarType: Int by JoozdLogSharedPrefs(sharedPref, SupportedCalendarTypes.INVALID)


    /**
     * Accept aircraft change from Monthly Overview without confirmation?
     */
    var updateAircraftWithoutAsking: Boolean by JoozdLogSharedPrefs(sharedPref, false)

    /*************************
     * Other settings
     *************************/

    //time to allocate to pilot if flying heavy crew and did takeoff or landing
    var standardTakeoffLandingTimes: Int by JoozdLogSharedPrefs(sharedPref, 30)

    //Calendar on device that is used to import flights
    var selectedCalendar: String by JoozdLogSharedPrefs(sharedPref, NO_CALENDAR_SELECTED)

    // true if user wants new flights to be marked as IFR. Not sure if I want to use this.
    // var normallyFliesIFR: Boolean by JoozdLogSharedPrefs(sharedPref, true)

    // true if user doesn't want to use cloud -
    var dontUseCloud: Boolean by JoozdLogSharedPrefs(sharedPref, false)

}