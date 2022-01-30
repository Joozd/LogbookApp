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
import nl.joozd.logbookapp.data.room.model.PreloadedRegistration
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class MockPreloadedRegistrationsDao: PreloadedRegistrationsDao {
    private val simulatedDatabase = LinkedHashMap<String, PreloadedRegistration>()
    private val simulatedFlow = MutableStateFlow<List<PreloadedRegistration>>(emptyList())
    private var _flow by CastFlowToMutableFlowShortcut(simulatedFlow)

    override suspend fun requestAllRegistrations(): List<PreloadedRegistration> =
        simulatedDatabase.values.toList()


    override fun registrationsFlow(): Flow<List<PreloadedRegistration>> =
        simulatedFlow

    override suspend fun save(regs: List<PreloadedRegistration>) {
        regs.forEach {
            simulatedDatabase[it.registration] = it
        }
        emit()
    }

    override suspend fun getAircraftFromRegistration(reg: String): PreloadedRegistration? =
        simulatedDatabase[reg] ?: simulatedDatabase.values.firstOrNull { it.registration.equals(reg, ignoreCase = true) }

    override suspend fun clearDb() {
        simulatedDatabase.clear()
        emit()
    }

    private fun emit(){
        _flow = makeList()
    }

    private fun makeList() = simulatedDatabase.values.toList()
}