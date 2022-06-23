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

package nl.joozd.logbookapp.data.room.dao


import androidx.room.*
import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.dataclasses.FlightData

/**
 * Saves flights as ModelFlight, takes care of changing into Flight
 */

@Dao
interface FlightDao {
    @Query("SELECT * FROM FlightData WHERE DELETEFLAG == 0 ORDER BY timeOut DESC")
    fun validFlightsFlow(): Flow<List<FlightData>>

    @Query("SELECT * FROM FlightData WHERE DELETEFLAG == 0 AND flightID = :id LIMIT 1")
    suspend fun getFlightById(id: Int): FlightData?

    @Query("SELECT * FROM FlightData WHERE DELETEFLAG == 0 AND flightID IN (:ids)")
    suspend fun getFlightsByID(ids: Collection<Int>): List<FlightData>

    @Query("SELECT * FROM FlightData WHERE DELETEFLAG == 0 AND timeOut >= :startEpochSecond AND timeOut <= :endEpochSecond")
    suspend fun getFlightsStartingBetween(startEpochSecond: Long, endEpochSecond: Long): List<FlightData>

    @Query("SELECT * FROM FlightData ORDER BY timeOut DESC")
    suspend fun getAllFlights(): List<FlightData>

    @Query("SELECT * FROM FlightData WHERE DELETEFLAG == 0 ORDER BY timeOut DESC")
    suspend fun getValidFlights(): List<FlightData>

    @Query("SELECT MAX(flightID) FROM FlightData")
    suspend fun highestUsedID(): Int?

    @Query("SELECT * FROM FlightData WHERE isPlanned = 0 AND DELETEFLAG = 0 ORDER BY timeIn DESC LIMIT 1")
    suspend fun getMostRecentCompleted(): FlightData?

    @Query("SELECT MAX(timeStamp) FROM FlightData WHERE isPlanned = 0")
    suspend fun getMostRecentTimestampOfACompletedFlight(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(flightData: Collection<FlightData>)

    @Delete
    suspend fun delete(flightData: Collection<FlightData>)
}
