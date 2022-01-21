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
     * Get all flights (including deleted ones)
     * For only usable flights, use [FlightDataCache.flights]
     */
    suspend fun getAllFlightsInDB(): List<Flight>

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
    fun save(flight: Flight)

    /**
     * Save a collection of Flights to DB.
     */
    fun save (flights: Collection<Flight>)



    /**
     * Delete a flight.
     */
    fun delete(flight: Flight)



    companion object{
        val instance: FlightRepository get() = FlightRepositoryWithDirectAccess.instance
    }
}