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
import kotlinx.coroutines.flow.*
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.Preloader
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
    dataBase: JoozdlogDatabase,
    private val flightRepository: FlightRepository = FlightRepository.instance
): AircraftRepository, CoroutineScope by dispatchersProviderMainScope() {
    private val aircraftTypeDao = dataBase.aircraftTypeDao()
    private val preloadedRegistrationsDao = dataBase.preloadedRegistrationsDao()

    override val dataLoaded: StateFlow<Boolean> = MutableStateFlow(false).apply{
        MainScope().launch {
            if (aircraftTypeDao.aircraftTypesFlow().firstOrNull().isNullOrEmpty())
                updateAircraftTypes(Preloader().getPreloadedAircraftTypes())
            value = true
        }
    }

    override fun aircraftTypesFlow() = aircraftTypeDao.aircraftTypesFlow().map {
        it.toAircraftTypes()
    }


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
        )

    private suspend fun getAircraftTypes() = withContext(Dispatchers.IO) {
        aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }
    }

    private suspend fun getPreloadedRegistrations() = withContext(Dispatchers.IO) {
        preloadedRegistrationsDao.requestAllRegistrations()
    }

    override suspend fun getAircraftTypeByShortName(typeShortName: String): AircraftType? =
        aircraftTypeDao.getAircraftTypeFromShortName(typeShortName)?.toAircraftType()


    override suspend fun getAircraftFromRegistration(registration: String): Aircraft? =
        registrationToAircraftMap()[formatRegistration(registration)]

    private fun findKeyIgnoreCase(
        map: Map<String, Aircraft>,
        registration: String
    ) = map.keys.firstOrNull { it.equals(registration, ignoreCase = true) }

    private fun findKeyLettersOnly(
        map: Map<String, Aircraft>,
        registration: String
    ) = map.keys.firstOrNull {
        it.lettersOnly().equals(registration.lettersOnly(), ignoreCase = true)
    }

    private fun String.lettersOnly() = filter { it.isLetter() }

    override suspend fun updateAircraftTypes(newTypes: List<AircraftType>) =
        withContext(DispatcherProvider.io()+ NonCancellable){
            aircraftTypeDao.clearDb()
            saveAircraftTypes(newTypes)
        }


    override suspend fun updateForcedTypes(newForcedTypes: List<ForcedTypeData>) =
        withContext(DispatcherProvider.io()+ NonCancellable){
            preloadedRegistrationsDao.clearDb()
            savePreloadedRegs(newForcedTypes.map { PreloadedRegistration(it) })
        }


    private suspend fun saveAircraftTypes(types: List<AircraftType>) {
        val typeData = types.map { it.toData() }
        aircraftTypeDao.save(*typeData.toTypedArray())
    }

    private suspend fun savePreloadedRegs(preloaded: List<PreloadedRegistration>) {
        preloadedRegistrationsDao.save(preloaded)
    }

    private fun makeAircraftMapFlow(): Flow<Map<String, Aircraft>> =
        combine(
            aircraftTypesFlow(),
            preloadedRegistrationsFlow(),
            flightRepository.allFlightsFlow()
        ) { aircraftTypes, preloaded, allFlights ->
            makeAircraftMap(aircraftTypes,
                preloaded,
                allFlights
            )
        }


    /*
     * First load Preloaded, then regWithTypes. This way, regWithTypes overrules preloaded.
     */
    private suspend fun makeAircraftMap(
        aircraftTypes: List<AircraftType>,
        preloaded: List<PreloadedRegistration>,
        allFlights: List<Flight>
    ): Map<String, Aircraft> {
        val map = LinkedHashMap<String, Aircraft>()
        val aircraftFromFlightsMapAsync = buildAircraftMapFromFlightsAsync(allFlights, aircraftTypes)
        aircraftFromFlightsMapAsync.await().forEach{
            if (it.value.type != null || map[it.key] == null)
                map[formatRegistration(it.key)] = it.value
        }
        preloaded.forEach {
            map[formatRegistration(it.registration)] = it.toAircraft(aircraftTypes)
        }
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
                map[reg] = Aircraft(registration = reg, type = getAircraftTypeFromMapAndFlight(typesMap, f), source = Aircraft.FLIGHT)
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

