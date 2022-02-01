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

package nl.joozd.logbookapp.data.repository.flightRepository

import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * Expanded interface with specific functions to get specific data from DB
 *  e.g. "most recent timestamp of a completed flight"
 */
interface FlightRepositoryWithSpecializedFunctions: FlightRepository {
    /**
     * Get most recent timestamp of a completed flight.
     * If none found, returns null.
     */
    suspend fun getMostRecentTimestampOfACompletedFlight(): Long?

    /**
     * Get most recent timestamp of a completed flight.
     * If none found, returns null.
     */
    suspend fun getMostRecentCompletedFlight(): Flight?

    companion object{
        val instance: FlightRepositoryWithSpecializedFunctions get() = FlightRepositoryImpl.instance
        fun mock(mockDataBase: JoozdlogDatabase): FlightRepositoryWithSpecializedFunctions =
            FlightRepositoryImpl(mockDataBase)
    }
}