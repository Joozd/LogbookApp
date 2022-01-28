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
import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.dataclasses.Airport

class MockAirportDao: AirportDao {
    override suspend fun requestAllAirports(): List<Airport> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAllIdents(): List<String> {
        TODO("Not yet implemented")
    }

    override fun requestLiveAirports(): LiveData<List<Airport>> {
        TODO("Not yet implemented")
    }

    override fun airportsFlow(): Flow<List<Airport>> {
        TODO("Not yet implemented")
    }

    override suspend fun insertAirports(vararg airportData: Airport) {
        TODO("Not yet implemented")
    }

    override suspend fun insertAirports(airportData: Collection<Airport>) {
        TODO("Not yet implemented")
    }

    override suspend fun clearDb() {
        TODO("Not yet implemented")
    }

    override suspend fun searchAirportByIdent(query: String): Airport? {
        TODO("Not yet implemented")
    }
}