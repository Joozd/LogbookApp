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
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class MockRegstrationDao: RegistrationDao {
    private val simulatedDatabase = LinkedHashMap<String, AircraftRegistrationWithTypeData>()
    private val simulatedFlow = MutableStateFlow<List<AircraftRegistrationWithTypeData>>(emptyList())
    private var _flow by CastFlowToMutableFlowShortcut(simulatedFlow)

    override suspend fun requestAllRegistrations(): List<AircraftRegistrationWithTypeData> =
        simulatedDatabase.values.toList()

    override fun allRegistrationsFlow(): Flow<List<AircraftRegistrationWithTypeData>> = simulatedFlow

    override suspend fun getAircraftFromRegistration(reg: String): AircraftRegistrationWithTypeData? =
        simulatedDatabase[reg] ?: simulatedDatabase.values.firstOrNull { it.registration.equals(reg, ignoreCase = true) }


    override suspend fun save(vararg regs: AircraftRegistrationWithTypeData) {
        regs.forEach {
            simulatedDatabase[it.registration] = it
            println("added ${it.registration}: ${simulatedDatabase.keys}")
        }
        emit()
    }

    private fun emit(){
        _flow = makeList()
    }

    private fun makeList() = simulatedDatabase.values.toList()
}