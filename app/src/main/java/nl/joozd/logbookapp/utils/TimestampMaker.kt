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

import androidx.work.ListenableWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.maxOfThisAnd
import java.time.Instant

class TimestampMaker(private val mock: Boolean = false) {
    /**
     * Now, in seconds from Epoch, for sync purposes
     * - corrected for differences with server time
     * - always 1 second later than the last sync
     */
    val nowForSycPurposes: Long
        get() = if (mock) Instant.now().epochSecond
                else maxOf(Instant.now().epochSecond + Preferences.serverTimeOffset, Preferences.lastUpdateTime+1)

    suspend fun getAndSaveTimeOffset(): Long? {
        val serverTime = Cloud.getTime() ?: return null
        val now = Instant.now().epochSecond
        Preferences.serverTimeOffset = serverTime - now
        return Preferences.serverTimeOffset
    }
}