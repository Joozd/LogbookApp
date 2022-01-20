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


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.dataclasses.Airport


@Dao
interface AirportDao {
    @Query("SELECT * FROM Airport")
    suspend fun requestAllAirports(): List<Airport>

    @Query("SELECT ident FROM Airport")
    suspend fun requestAllIdents(): List<String>

    @Query("SELECT * FROM Airport")
    fun requestLiveAirports(): LiveData<List<Airport>>

    @Query("SELECT * FROM Airport")
    fun airportsFlow(): Flow<List<Airport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirports(vararg airportData: Airport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirports(airportData: Collection<Airport>)

    @Query("DELETE FROM Airport")
    suspend fun clearDb()

    @Query("SELECT * FROM Airport WHERE :query LIKE ident LIMIT 1")
    suspend fun searchAirportByIdent(query: String): Airport?

}
