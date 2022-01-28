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

import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * Flight Repository takes care of undo/redo-able i/o operations to Flight Database
 */
interface FlightRepository {
    /**
     * Get a single flight by it's ID
     */
    suspend fun getFlightByID(flightID: Int): Flight?

    /**
     * Get a list of Flights by it's ID
     */
    suspend fun getFlightsByID(ids: Collection<Int>): List<Flight>

    /**
     * Get all flights (including deleted ones)
     */
    suspend fun getAllFlightsInDB(): List<Flight>

    /**
     * Get a Flow of all valid (DELETEFLAG = false) Flights
     */
    fun getAllFlightsFlow(): Flow<List<Flight>>

    /**
     * Get all valid (DELETEFLAG = false) Flights
     */
    suspend fun getAllFlights(): List<Flight>

    /**
     * make a [FlightDataCache] with snapshot flight data
     */
    suspend fun getFLightDataCache(): FlightDataCache

    /**
     * Get a flow of updated FlightDataCaches
     */
    fun flightDataCacheFlow(): Flow<FlightDataCache>

    /**
     * Save a flight to DB.
     */
    suspend fun save(flight: Flight)

    /**
     * Save a collection of Flights to DB.
     */
    suspend fun save (flights: Collection<Flight>)

    /**
     * Delete a flight.
     */
    suspend fun delete(flight: Flight)

    /**
     * Delete a collection of flights.
     */
    suspend fun delete(flights: Collection<Flight>)

    /**
     * Generate a flight ID that can safely be used to save a new flight to DB at a later point.
     * @param highestTakenID: Lowest ID that can be accepted for any new flight.
     *      Any generated new ID can be equal or higher than this, but not lower.
     */
    suspend fun generateAndReserveNewFlightID(highestTakenID: Int): Int


    companion object{
        val instance: FlightRepository get() = FlightRepositoryWithDirectAccess.instance
    }
}