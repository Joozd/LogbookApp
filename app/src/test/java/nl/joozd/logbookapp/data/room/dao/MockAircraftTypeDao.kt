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

class MockAircraftTypeDao: AircraftTypeDao {
    private val simulatedDatabase = ArrayList<AircraftTypeData>()
    private val simulatedFlow = MutableStateFlow<List<AircraftTypeData>>(listOf(AircraftTypeData("aap", "noot", false, multiEngine = false)))

    override suspend fun requestAllAircraftTypes(): List<AircraftTypeData> = simulatedDatabase

    override fun aircraftTypesFlow(): Flow<List<AircraftTypeData>> = simulatedFlow

    override suspend fun save(vararg aircraftTypeData: AircraftTypeData) {
        //println("${this::class.simpleName} Saving ${aircraftTypeData.size} type data")
        simulatedDatabase.addAll(aircraftTypeData)
        emit()
    }

    override suspend fun clearDb() {
        println("${this::class.simpleName} Clear DB")
        simulatedDatabase.clear()
        emit()
    }

    private fun emit(){
        println("emit() should emit ${simulatedDatabase.size} items")
        simulatedFlow.update { simulatedDatabase.toList() } // also tried: simulatedFlow.value = simulatedDatabase.toList()
        println("simulatedFlow.value is now ${simulatedFlow.value.size}")
    }

    override fun getAircraftType(name: String): AircraftTypeData? =
        simulatedDatabase.firstOrNull { it.name == name }

    override fun getAircraftTypeFromShortName(name: String): AircraftTypeData? =
        simulatedDatabase.firstOrNull { it.shortName == name }


}