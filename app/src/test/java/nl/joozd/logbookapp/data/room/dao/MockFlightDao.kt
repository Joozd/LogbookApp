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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class MockFlightDao: FlightDao {
    private val simulatedDatabase = LinkedHashMap<Int, FlightData>()
    private val simulatedFlow = MutableStateFlow<List<FlightData>>(emptyList())
    private var _flow by CastFlowToMutableFlowShortcut(simulatedFlow)

    override fun validFlightsFlow(): Flow<List<FlightData>> =
        simulatedFlow.map { it.filter { f -> !f.DELETEFLAG }}

    override suspend fun getFlightById(id: Int): FlightData? =
        simulatedDatabase[id]

    override suspend fun getFlightsByID(ids: Collection<Int>): List<FlightData> =
        ids.mapNotNull { simulatedDatabase[it] }

    override suspend fun getAllFlights(): List<FlightData> =
        simulatedDatabase.values.toList()

    override suspend fun getValidFlights(): List<FlightData> =
        getAllFlights().filter { !it.DELETEFLAG }

    override suspend fun highestUsedID(): Int? =
        simulatedDatabase.keys.maxOrNull()

    override suspend fun getMostRecentCompleted(): FlightData? =
        getCompletedFlights()
            .maxByOrNull { it.timeIn }

    override suspend fun getMostRecentTimestampOfACompletedFlight(): Long? =
        getCompletedFlights()
            .maxOfOrNull { it.timeStamp }


    override suspend fun save(flightData: Collection<FlightData>) {
        flightData.forEach {
            simulatedDatabase[it.flightID] = it
        }
        emit()
    }

    override suspend fun delete(flightData: Collection<FlightData>) {
        TODO("Not yet implemented")
    }

    private fun emit(){
        _flow = makeList()
    }

    private suspend fun getCompletedFlights() =
        getValidFlights()
            .filter { !it.isPlanned }

    private fun makeList() = simulatedDatabase.values.toList()
}