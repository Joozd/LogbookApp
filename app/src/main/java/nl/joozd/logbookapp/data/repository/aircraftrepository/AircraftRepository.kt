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
import kotlinx.coroutines.flow.map
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.AircraftRegistrationWithType
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.model.PreloadedRegistration
import nl.joozd.logbookapp.data.room.model.toAircraftTypes
import nl.joozd.logbookapp.data.room.model.toData

/**
 * Repository for everything aircraft.
 * This takes care of storing/retrieving data from local DBs
 */
object AircraftRepository: CoroutineScope by MainScope() {
    private val dataBase = JoozdlogDatabase.getDatabase(App.instance)
    private val aircraftTypeDao = dataBase.aircraftTypeDao()
    private val registrationDao = dataBase.registrationDao()
    private val preloadedRegistrationsDao = dataBase.preloadedRegistrationsDao()

    private val cache = AircraftDataCache(
        aircraftTypeDao,
        registrationDao,
        preloadedRegistrationsDao,
        Dispatchers.IO
    )

    val aircraftMapFlow = cache.registrationToAircraftMapFlow

    val aircraftFlow = cache.registrationToAircraftMapFlow.map { it.values }

    val aircraftTypesFlow = aircraftTypeDao.aircraftTypesFlow().map {
        it.toAircraftTypes()
    }

    //null if not cached yet
    val aircraftTypes: List<AircraftType>? get() = cache.cachedAircraftTypeData

    /*
    TODO this uses cachedSortedRegistrationsList which did not belong in here.
        If it is used in more places than just EditFLightFragment I might make a
        RegistrationsWorker or something class to deal with this


    /**
     * Searches for a match in order of priority in aircraftMap
     * If nothing cached yet, it will return an empty list
     */
    fun getBestHitForPartialRegistration(r: String): Aircraft? =
        cache.getRegistrationToAircraftMapOrEmptyMapIfNotLoadedYet()[findBestHitForRegistration(r, cachedSortedRegistrationsList)]

     */

    suspend fun requireMap() = cache.getRegistrationToAircraftMap()

    //This gets whatever we have currently loaded. Might be nothing if its called really fast after starting app.
    //Its not suspended though.
    fun getMapWithCurrentCachedValues() =
        cache.getRegistrationToAircraftMapOrEmptyMapIfNotLoadedYet()

    suspend fun getAircraftFromRegistration(registration: String?): Aircraft? =
        registration?.let {
            cache.getRegistrationToAircraftMap()[formatRegistration(registration)]
        }

    fun getAircraftFromRegistrationCachedOnly(registration: String?): Aircraft? =
        registration?.let {
            cache.getRegistrationToAircraftMapOrEmptyMapIfNotLoadedYet()[formatRegistration(registration)]
        }


    suspend fun getAircraftTypeByShortName(shortName: String): AircraftType? =
        cache.getAircraftTypes().firstOrNull { it.shortName == shortName }

    fun saveAircraft(aircraft: Aircraft) = launch(Dispatchers.IO) {
        if (aircraft.type?.name == null) return@launch // Don't save aircraft without type.
        val newAcrwt = AircraftRegistrationWithType(aircraft.registration, aircraft.type)
        saveAircraftRegistrationWithType(newAcrwt)
    }

    fun replaceAllTypesWith(newTypes: List<AircraftType>) =
        launch(Dispatchers.IO + NonCancellable) {
            aircraftTypeDao.clearDb()
            saveAircraftTypes(newTypes)
        }

    fun replaceAllPreloadedWith(newPreloaded: List<PreloadedRegistration>) =
        launch(Dispatchers.IO) {
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
}

