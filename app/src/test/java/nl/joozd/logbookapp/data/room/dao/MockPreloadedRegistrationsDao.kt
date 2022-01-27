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
import kotlinx.coroutines.flow.MutableStateFlow
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData
import nl.joozd.logbookapp.data.room.model.PreloadedRegistration

class MockPreloadedRegistrationsDao: PreloadedRegistrationsDao {
    private val simulatedDatabase = ArrayList<PreloadedRegistration>()
    private val simulatedFlow = MutableStateFlow(simulatedDatabase)

    override suspend fun requestAllRegistrations(): List<PreloadedRegistration> = simulatedDatabase

    override fun requestLiveRegistrations(): LiveData<List<PreloadedRegistration>> {
        TODO("Not yet implemented")
    }

    override fun registrationsFlow(): Flow<List<PreloadedRegistration>> = simulatedFlow

    override suspend fun save(vararg regs: PreloadedRegistration) {
        TODO("Not yet implemented")
    }

    override suspend fun save(regs: List<PreloadedRegistration>) {
        TODO("Not yet implemented")
    }

    override suspend fun clearDb() {
        TODO("Not yet implemented")
    }
}