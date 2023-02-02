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
     * Get all flights starting in [range]
     */
    suspend fun getFlightsStartingInEpochSecondRange(range: ClosedRange<Long>)

    /**
     * Get a Flow of all valid (DELETEFLAG = false) Flights
     */
    fun allFlightsFlow(): Flow<List<Flight>>

    /**
     * Get all valid (DELETEFLAG = false) Flights
     */
    suspend fun getAllFlights(): List<Flight>

    /**
     * Get a flow of updated FlightDataCaches
     */
    fun flightDataCacheFlow(): Flow<FlightDataCache>

    /**
     * Save a flight to DB.
     * Saving a flight updates its timestamp
     * and assigns an ID if it is set to [Flight.FLIGHT_ID_NOT_INITIALIZED]
     */
    suspend fun save(flight: Flight)

    /**
     * Save a collection of Flights to DB.
     * Saving a flight updates its timestamp.
     * and assigns an ID if it is set to [Flight.FLIGHT_ID_NOT_INITIALIZED]
     */
    suspend fun save (flights: Collection<Flight>)

    /**
     * Delete a flight. This will remove it from DB.
     */
    suspend fun delete(flight: Flight)

    /**
     * Delete a collection of flights. This will remove them from DB.
     */
    suspend fun delete(flights: Collection<Flight>)

    /**
     * Delete a collection of flights. This will remove them from DB.
     */
    suspend fun deleteById(ids: Collection<Int>)

    /**
     * Delete a collection of flights. This will remove them from DB.
     */
    suspend fun deleteById(id: Int)

    /**
     * Delete entire Flights DB
     */
    suspend fun clear()

    /**
     * Generate a flight ID that can safely be used to save a new flight to DB at a later point.
     * @param highestTakenID: Lowest ID that can be accepted for any new flight.
     *      - Any generated new ID can be equal or higher than this, but not lower.
     *      - Any generated ID will also be higher than the previously highest ID in db,
     *        so you can use this a number of times in a row without having to update the parameter.
     */
    suspend fun generateAndReserveNewFlightID(highestTakenID: Int = 1): Int

    fun interface OnDataChangedListener{
        fun onFlightRepositoryChanged(changedFlightIDs: List<Int>)
    }


    companion object{
        //If changing concrete class, change it in mock() as well!
        val instance: FlightRepository get() = FlightRepositoryImpl.instance

        fun mock(mockDataBase: JoozdlogDatabase): FlightRepository =
            FlightRepositoryImpl(mockDataBase)
    }
}