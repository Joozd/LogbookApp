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
import kotlinx.coroutines.flow.update
import nl.joozd.logbookapp.data.room.model.AircraftTypeData
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class MockAircraftTypeDao: AircraftTypeDao {
    private val simulatedDatabase = LinkedHashMap<String,AircraftTypeData>()
    private val simulatedFlow = MutableStateFlow<List<AircraftTypeData>>(emptyList())
    private var _flow by CastFlowToMutableFlowShortcut(simulatedFlow)

    override suspend fun requestAllAircraftTypes(): List<AircraftTypeData> =
        makeList()

    override fun aircraftTypesFlow(): Flow<List<AircraftTypeData>> = simulatedFlow

    override suspend fun save(vararg aircraftTypeData: AircraftTypeData) {
        //println("${this::class.simpleName} Saving ${aircraftTypeData.size} type data")
        aircraftTypeData.forEach {
            simulatedDatabase[it.name] = it
        }
        emit()
    }

    override suspend fun clearDb() {
        simulatedDatabase.clear()
        emit()
    }

    override fun getAircraftType(name: String): AircraftTypeData? =
        simulatedDatabase[name]

    override suspend fun getAircraftTypeFromShortName(name: String): AircraftTypeData? =
        simulatedDatabase.values.firstOrNull { it.shortName.equals(name, ignoreCase = true) }

    private fun emit(){
        _flow = makeList()
    }

    private fun makeList() = simulatedDatabase.values.toList()
}