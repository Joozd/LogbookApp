/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.data.sharedPrefs

import android.content.Context
import android.util.Base64
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.App
import java.security.MessageDigest

object Preferences {
    //key names:
    const val CURRENTVERSION = 1 // version of this Preferences instance, only update if changing something in this companion object
    /*
    Version history: 1  -> First implementation, had USERNAME, PASSWORD and STOREDVERSION
    */

    private const val STOREDVERSION = "storedVersion"
    private const val USERNAME = "username"
    private const val PASSWORD = "password" // saved in plaintext, warn users about this
    private const val LAST_UPDATE_TIMESTAMP = "lastUpdateTime"
    private const val SERVER_TIME_OFFSET = "serverTimeOffset"
    private const val AIRPORT_DB_VERSION = "airportDbVersion"

    // private const val USE_IATA_AIRPORTS = "useIataAirports"
    private const val STANDARD_TAKEOFF_LANDING_TIMES = "standardTakeoffLandingTimes"


    private val sharedPref by lazy{
        val context = App.instance.ctx
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }


    var version: Int
        get() = sharedPref.getInt(STOREDVERSION, 0)
        set(v) = with(sharedPref.edit()) {
            putInt(STOREDVERSION, v)
            apply()
        }

    /**
     * username is the users' username.
     */
    var username: String?
        get() = sharedPref.getString(USERNAME,null)
        set(v) = with(sharedPref.edit()) {
            putString(USERNAME, v)
            apply()
        }

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

    var lastUpdateTime: Long
        get() = sharedPref.getLong(LAST_UPDATE_TIMESTAMP,-1)
        set(v) = with(sharedPref.edit()) {
            putLong(LAST_UPDATE_TIMESTAMP, v)
            apply()
        }

    var serverTimeOffset: Long
        get() = sharedPref.getLong(SERVER_TIME_OFFSET,0)
        set(v) = with(sharedPref.edit()) {
            putLong(SERVER_TIME_OFFSET, v)
            apply()
        }

    var airportDbVersion: Int
        get() = sharedPref.getInt(AIRPORT_DB_VERSION,0)
        set(v) = with(sharedPref.edit()) {
            putInt(AIRPORT_DB_VERSION, v)
            apply()
        }

    /***********************
     *   UI preferences:   *
     **********************/

    /**
     * Use ICAO or Iata? True = IATA, false ICAO
     */

    var useIataAirports: Boolean by JoozdLogSharedPrefs(sharedPref, false)
//        get() = sharedPref.getBoolean(USE_IATA_AIRPORTS,false)
//        set(v) = with(sharedPref.edit()) {
//            putBoolean(USE_IATA_AIRPORTS, v)
//            apply()
//        }

    /*************************
     * Other settings
     *************************/

    var standardTakeoffLandingTimes: Int
        get() = sharedPref.getInt(STANDARD_TAKEOFF_LANDING_TIMES,30)
        set(v) = with(sharedPref.edit()) {
            putInt(STANDARD_TAKEOFF_LANDING_TIMES, v)
            apply()
        }
}