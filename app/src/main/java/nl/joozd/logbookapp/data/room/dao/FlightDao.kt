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

package nl.joozd.logbookapp.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import nl.joozd.logbookapp.data.dataclasses.FlightData

/**
 * Saves flights as ModelFlight, takes care of changing into Flight
 */

@Dao
interface FlightDao {
    @Query("SELECT * FROM FlightData ORDER BY timeOut DESC")
    suspend fun requestAllFlights(): List<FlightData>

    @Query("SELECT * FROM FlightData WHERE DELETEFLAG == 0 ORDER BY timeOut DESC")
    suspend fun requestValidFlights(): List<FlightData>

    @Query("SELECT * FROM FlightData ORDER BY timeOut DESC")
    fun requestLiveData(): LiveData<List<FlightData>>

    @Query("SELECT * FROM FlightData WHERE DELETEFLAG = 0 ORDER BY timeOut DESC")
    fun requestNonDeletedLiveData(): LiveData<List<FlightData>>

    @Query("SELECT MAX(flightID) from FlightData")
    suspend fun highestId(): Int?

    @Query("SELECT * FROM FlightData WHERE flightID = :id LIMIT 1")
    suspend fun fetchFlightByID(id: Int): FlightData?

    @Query("SELECT * FROM FlightData WHERE isPlanned = 0 AND DELETEFLAG = 0 ORDER BY timeIn LIMIT 1")
    suspend fun getMostRecentCompleted(): FlightData?

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlights(vararg flightData: FlightData)

    @Delete
    suspend fun delete(flightData: FlightData)

    @Query("DELETE FROM FlightData where flightID in (:idsToDelete)")
    suspend fun deleteMultipleByID(idsToDelete: List<Int>)

    @Query ("DELETE FROM FlightData")
    suspend fun clearDb()
}
