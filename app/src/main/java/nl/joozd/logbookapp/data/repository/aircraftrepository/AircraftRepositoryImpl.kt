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

package nl.joozd.logbookapp.data.repository.aircraftrepository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.AircraftRegistrationWithType
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.model.*
import nl.joozd.logbookapp.utils.DispatcherProvider

/**
 * Repository for everything aircraft.
 * This takes care of storing/retrieving data from local DBs
 * Singleton instead of object so we can inject a mock database
 */
class AircraftRepositoryImpl(
    private val dataBase: JoozdlogDatabase
): AircraftRepository, CoroutineScope by MainScope() {
    private val aircraftTypeDao get() = dataBase.aircraftTypeDao()
    private val registrationDao get() = dataBase.registrationDao()
    private val preloadedRegistrationsDao get() = dataBase.preloadedRegistrationsDao()

    override fun aircraftTypesFlow() = aircraftTypeDao.aircraftTypesFlow().map {
        it.toAircraftTypes()
    }

    override fun aircraftRegistrationsFlow() = registrationDao.allRegistrationsFlow().map {
        it.toAircraftRegistrationWithTypes()
    }

    override fun preloadedRegistrationsFlow() = preloadedRegistrationsDao.registrationsFlow()

    override fun aircraftMapFlow() = makeAircraftMapFlow()

    override fun aircraftFlow() = aircraftMapFlow().map {
        it.values.toList()
    }

    override fun registrationsFlow() = aircraftMapFlow().map { it.keys.toList() }

    override fun aircraftDataCacheFlow(): Flow<AircraftDataCache> =
        combine(aircraftTypesFlow(), aircraftMapFlow()){ types, map ->
            AircraftDataCache.make(types, map)
        }

    suspend fun registrationToAircraftMap(): Map<String, Aircraft> =
        makeAircraftMap(
            getAircraftTypes(),
            getPreloadedRegistrations(),
            getRegistrationWithTypes()
        )

    suspend fun getAircraftDataCache(): AircraftDataCache = AircraftDataCache.make(
        getAircraftTypes(),
        registrationToAircraftMap()
    )


    suspend fun getAircraftTypes() =
        aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }

    suspend fun getPreloadedRegistrations() =
        preloadedRegistrationsDao.requestAllRegistrations()

    suspend fun getRegistrationWithTypes() =
        registrationDao.requestAllRegistrations().map { it.toAircraftRegistrationWithType() }

    suspend fun getAircraftTypeByShortName(shortName: String): AircraftType? =
        aircraftTypeDao.getAircraftTypeFromShortName(shortName)?.toAircraftType()

    suspend fun getAircraftFromRegistration(registration: String) =
        registrationDao.getAircraftFromRegistration(registration)?.toAircraftRegistrationWithType()

    fun saveAircraft(aircraft: Aircraft) = launch(DispatcherProvider.io()) {
        if (aircraft.type?.name == null) return@launch // Don't save aircraft without type.
        val newAcrwt = AircraftRegistrationWithType(aircraft.registration, aircraft.type)
        saveAircraftRegistrationWithType(newAcrwt)
    }

    fun replaceAllTypesWith(newTypes: List<AircraftType>) =
        launch(DispatcherProvider.io() + NonCancellable) {
            aircraftTypeDao.clearDb()
            saveAircraftTypes(newTypes)
        }

    fun replaceAllPreloadedWith(newPreloaded: List<PreloadedRegistration>) =
        launch(DispatcherProvider.io()) {
            preloadedRegistrationsDao.clearDb()
            savePreloadedRegs(newPreloaded)
        }

    private suspend fun saveAircraftTypes(types: List<AircraftType>) {
        val typeData = types.map { it.toData() }
        aircraftTypeDao.save(*typeData.toTypedArray())
    }

    private suspend fun saveAircraftRegistrationWithType(arwt: AircraftRegistrationWithType) {
        registrationDao.save(arwt.toData())
    }

    private suspend fun savePreloadedRegs(preloaded: List<PreloadedRegistration>) {
        preloadedRegistrationsDao.save(preloaded)
    }

    private fun makeAircraftMapFlow(): Flow<Map<String, Aircraft>> =
        combine(
            aircraftTypesFlow(),
            aircraftRegistrationsFlow(),
            preloadedRegistrationsFlow()
        ) { aircraftTypes, registrationsWithTypes, preloaded ->
            makeAircraftMap(aircraftTypes, preloaded, registrationsWithTypes)
        }


    private fun makeAircraftMap(
        aircraftTypes: List<AircraftType>,
        preloaded: List<PreloadedRegistration>,
        registrationsWithTypes: List<AircraftRegistrationWithType>
    ): HashMap<String, Aircraft> {
        val map = HashMap<String, Aircraft>()
        preloaded.forEach {
            map[formatRegistration(it.registration)] = it.toAircraft(aircraftTypes)
        }
        registrationsWithTypes.forEach {
            map[formatRegistration(it.registration)] = it.toAircraft()
        }
        return map
    }
}
