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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM AircraftRegistrationWithTypeData")
    suspend fun requestAllRegistrations(): List<AircraftRegistrationWithTypeData>

    @Query("SELECT * FROM AircraftRegistrationWithTypeData")
    fun allRegistrationsFlow(): Flow<List<AircraftRegistrationWithTypeData>>

    @Query("SELECT * FROM AircraftRegistrationWithTypeData where registration = :reg LIMIT 1")
    suspend fun getAircraftFromRegistration(reg: String): AircraftRegistrationWithTypeData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg regs: AircraftRegistrationWithTypeData)
}