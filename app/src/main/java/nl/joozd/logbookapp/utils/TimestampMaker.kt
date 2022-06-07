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

package nl.joozd.logbookapp.utils

import nl.joozd.logbookapp.comm.getTimeFromServer
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import java.time.Instant

class TimestampMaker() {
    /**
     * Now, in seconds from Epoch, for sync purposes
     * - corrected for differences with server time
     * - always 1 second later than the last sync
     */
    val nowForSycPurposes get() = now + Prefs.serverTimeOffset

    val now get() = Instant.now().epochSecond

    suspend fun getAndSaveTimeOffset() {
        getTimeFromServer()?.let { serverTime ->
            val now = Instant.now().epochSecond
            Prefs.postServerTimeOffset(serverTime - now)
        }
    }
}