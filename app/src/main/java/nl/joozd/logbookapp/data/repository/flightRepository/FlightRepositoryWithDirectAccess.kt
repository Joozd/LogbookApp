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

/*
 * FlightRepositoryWithDirectAccess should be the only [FlightRepository] with direct access
 * to database, and should be initialized as a singleton.
 */
interface FlightRepositoryWithDirectAccess: FlightRepository {
    /**
     * lock the repository (with a Mutex) while performing [block]
     * The mutex is the same as [save] and [delete] use, so those functions will be delayed as well.
     */
    suspend fun <T> doLocked(block: suspend FlightRepositoryWithDirectAccess.() -> T): T

    /**
     * Save a Flight bypassing all updating that is usually done before saving
     * (e.g. updating timestamp)
     */
    suspend fun saveDirectToDB(flight: Flight)

    /**
     * Save a collection of Flights bypassing all updating that is usually done before saving
     * (e.g. updating timestamp)
     */
    suspend fun saveDirectToDB(flights: Collection<Flight>)

    /**
     * Delete a flight hard from database
     */
    suspend fun deleteHard(flight: Flight)

    /**
     * Delete a collection of Flights hard from database
     */
    suspend fun deleteHard(flights: Collection<Flight>)

    /**
     * Get all flights (including deleted ones)
     */
    suspend fun getAllFlightsInDB(): List<Flight>

    /**
     * Delete all flights in DB
     */
    suspend fun clear()

    companion object{
        val instance: FlightRepositoryWithDirectAccess get() = FlightRepositoryImpl.instance

        fun mock(mockDataBase: JoozdlogDatabase): FlightRepositoryWithDirectAccess =
            FlightRepositoryImpl(mockDataBase)
    }
}