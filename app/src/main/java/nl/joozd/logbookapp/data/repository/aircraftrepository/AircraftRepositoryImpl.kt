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
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.model.*
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope

/**
 * Repository for everything aircraft.
 * This takes care of storing/retrieving data from local DBs
 * Singleton instead of object so we can inject a mock database
 */
// TODO finish getting aircraft from flights
class AircraftRepositoryImpl(
    dataBase: JoozdlogDatabase
): AircraftRepository, CoroutineScope by dispatchersProviderMainScope() {
    private val aircraftTypeDao = dataBase.aircraftTypeDao()
    //private val registrationDao = dataBase.registrationDao()
    private val preloadedRegistrationsDao = dataBase.preloadedRegistrationsDao()
    private val flightRepository = FlightRepository.instance

    override fun aircraftTypesFlow() = aircraftTypeDao.aircraftTypesFlow().map {
        it.toAircraftTypes()
    }

    /*
    private fun aircraftRegistrationsFlow() = registrationDao.allRegistrationsFlow().map {
        it.toAircraftRegistrationWithTypes()
    }
    */

    private  fun preloadedRegistrationsFlow() = preloadedRegistrationsDao.registrationsFlow()

    override fun aircraftMapFlow() = makeAircraftMapFlow()

    override fun aircraftDataCacheFlow(): Flow<AircraftDataCache> =
        combine(aircraftTypesFlow(), aircraftMapFlow()){ types, map ->
            AircraftDataCache.make(types, map)
        }

    override suspend fun getAircraftDataCache(): AircraftDataCache = AircraftDataCache.make(
        getAircraftTypes(),
        registrationToAircraftMap()
    )

    override suspend fun registrationToAircraftMap(): Map<String, Aircraft> =
        makeAircraftMap(
            getAircraftTypes(),
            getPreloadedRegistrations(),
            flightRepository.getAllFlights()
            //getRegistrationWithTypes()
        )

    suspend fun getAircraftTypes() =
        aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }

    suspend fun getPreloadedRegistrations() =
        preloadedRegistrationsDao.requestAllRegistrations()

    /*
    suspend fun getRegistrationWithTypes() =
        registrationDao.requestAllRegistrations().map { it.toAircraftRegistrationWithType() }
     */

    override suspend fun getAircraftTypeByShortName(typeShortName: String): AircraftType? =
        aircraftTypeDao.getAircraftTypeFromShortName(typeShortName)?.toAircraftType()

    /*
    override suspend fun getAircraftFromRegistration(registration: String): Aircraft? =
        registrationDao.getAircraftFromRegistration(registration)
                ?.toAircraftRegistrationWithType()
                ?.toAircraft()
            ?: preloadedRegistrationsDao.getAircraftFromRegistration(registration)
                ?.let{
                    val type = aircraftTypeDao.getAircraftType(it.type)?.toAircraftType()
                    Aircraft(it.registration, type, Aircraft.PRELOADED)
                }

     */

    /*
    override suspend fun saveAircraft(aircraft: Aircraft) = withContext(DispatcherProvider.io()) {
        if (aircraft.type?.name == null) return@withContext // Don't save aircraft without type.
        val newAcrwt = AircraftRegistrationWithType(aircraft.registration, aircraft.type)
        println("NEWAIRCRAFT: $newAcrwt")
        saveAircraftRegistrationWithType(newAcrwt)
    }
    */

    override suspend fun replaceAllTypesWith(newTypes: List<AircraftType>) =
        withContext(DispatcherProvider.io()+ NonCancellable){
            aircraftTypeDao.clearDb()
            saveAircraftTypes(newTypes)
        }


    override suspend fun replaceAllPreloadedWith(newPreloaded: List<PreloadedRegistration>) =
        withContext(DispatcherProvider.io()+ NonCancellable){
            preloadedRegistrationsDao.clearDb()
            savePreloadedRegs(newPreloaded)
        }


    private suspend fun saveAircraftTypes(types: List<AircraftType>) {
        val typeData = types.map { it.toData() }
        aircraftTypeDao.save(*typeData.toTypedArray())
    }

    /*
    private suspend fun saveAircraftRegistrationWithType(arwt: AircraftRegistrationWithType) {
        registrationDao.save(arwt.toData())
    }
    */

    private suspend fun savePreloadedRegs(preloaded: List<PreloadedRegistration>) {
        preloadedRegistrationsDao.save(preloaded)
    }

    private fun makeAircraftMapFlow(): Flow<Map<String, Aircraft>> =
        combine(
            aircraftTypesFlow(),
            //aircraftRegistrationsFlow(),
            preloadedRegistrationsFlow(),
            flightRepository.allFlightsFlow()
        ) { aircraftTypes /*, registrationsWithTypes */, preloaded, allFlights ->
            makeAircraftMap(aircraftTypes,
                preloaded,
                allFlights
                //registrationsWithTypes
            )
        }


    /*
     * First load Preloaded, then regWithTypes. This way, regWithTypes overrules preloaded.
     */
    private suspend fun makeAircraftMap(
        aircraftTypes: List<AircraftType>,
        preloaded: List<PreloadedRegistration>,
        allFlights: List<Flight>
        //registrationsWithTypes: List<AircraftRegistrationWithType>
    ): HashMap<String, Aircraft> {
        val map = HashMap<String, Aircraft>()
        val aircraftFromFlightsMapAsync = buildAircraftMapFromFlightsAsync(allFlights, aircraftTypes)
        preloaded.forEach {
            map[formatRegistration(it.registration)] = it.toAircraft(aircraftTypes)
        }
        aircraftFromFlightsMapAsync.await().forEach{
            if (it.value.type != null || map[it.key] == null)
                map[it.key] = it.value
        }

        /*
        registrationsWithTypes.forEach {
            map[formatRegistration(it.registration)] = it.toAircraft()
        }
        */
        return map
    }


    private fun buildAircraftMapFromFlightsAsync(flights: List<Flight>, aircraftTypes: List<AircraftType>): Deferred<Map<String, Aircraft>> =
        async(DispatcherProvider.default()){
            val sortedFlights = flights.sortedByDescending { it.timeOut }
            val typesMap = aircraftTypes.associateBy { it.shortName.uppercase() }
            val registrations = flights.map { it.registration }.toSet()
            val map = LinkedHashMap<String, Aircraft>(registrations.size)
            registrations.forEach {  reg ->
                val f = sortedFlights.first { it.registration == reg }
                map[reg] = Aircraft(registration = reg, type = getAircraftTypeFromMapAndFlight(typesMap, f))
            }
            return@async map // explicit return only for readability
        }

    private fun getAircraftTypeFromMapAndFlight(
        map: Map<String, AircraftType>,
        flight: Flight
    ): AircraftType? = with(flight) {
        if (aircraftType.isBlank()) null
        else map[aircraftType] ?: makeAircraftTypeFromFlight()
    }

    private fun Flight.makeAircraftTypeFromFlight() =
        AircraftType(
            shortName = aircraftType,
            multiPilot = multiPilotTime > 0
        )
}

