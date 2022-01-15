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
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration
import nl.joozd.logbookapp.data.room.JoozdlogDatabase

/**
 * Repository for everything aircraft.
 * This takes care of storing/retrieving data from local DBs
 * TODO: Consensus data only to be uploaded to server, not downloaded. Checked consensus data will be added to forced types list on server? Maybe?
 */
object AircraftRepository: CoroutineScope by MainScope() {
    private val dataBase = JoozdlogDatabase.getDatabase(App.instance)
    private val aircraftTypeDao = dataBase.aircraftTypeDao()
    private val registrationDao = dataBase.registrationDao()
    private val aircraftTypeConsensusDao = dataBase.aircraftTypeConsensusDao()
    private val preloadedRegistrationsDao = dataBase.preloadedRegistrationsDao()

    private val cache = AircraftDataCache(aircraftTypeDao, registrationDao, aircraftTypeConsensusDao, preloadedRegistrationsDao, Dispatchers.IO)

    val aircraftMapFlow = cache.registrationToAircraftMapFlow

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
    fun getMapWithCurrentCachedValues() = cache.getRegistrationToAircraftMapOrEmptyMapIfNotLoadedYet()

    suspend fun getAircraftFromRegistration(registration: String?): Aircraft? =
        registration?.let {
            cache.getRegistrationToAircraftMap()[formatRegistration(registration)]
        }

    suspend fun getAircraftTypeByShortName(shortName: String): AircraftType? =
        cache.getAircraftTypes().firstOrNull { it.shortName == shortName }




    /*

    /**
     * Livedata of al AircraftTypes
     */
    val aircraftTypesLiveData: LiveData<List<AircraftType>>
        get() = _aircraftTypesLiveData

    /**
     * LiveData of all Aircraft
     */
    val aircraftListLiveData: LiveData<List<Aircraft>>
        get() = _aircraftListLiveData

    /**
     * Livedata of all aircraft as map
     */
    val aircraftMapLiveData: LiveData<Map<String, Aircraft>>
        get() = _aircraftMapLiveData

    /**
     * Map of all aircraft, with registration as key.
     * Will be kept up-to-date by the same observeForever that makes sure [aircraftTypesLiveData], [aircraftListLiveData] and [aircraftMapLiveData] are always observed
     * (ie. always up-to-date and never null, even when not observed)
     * If you need to make sure it is filled (in case you might use it before data is loaded from DB), use [requireMap]
     */
    var aircraftMap: Map<String, Aircraft> = emptyMap()

    /**
     * Registrations, ordered by priority
     */
    val registrationsLiveData: LiveData<List<String>>
        get() = _sortedRegistrations

    /**
     * List of all known aircraft, null if [aircraftListLiveData] is not observed (it is always observed)
     */
    val aircraftList: List<Aircraft>?
        get() = aircraftListLiveData.value // null if not observed

    /**
     * List of all known aircraft types, null if [aircraftTypesLiveData] is not observed (it is always observed)
     */
    val aircraftTypes: List<AircraftType>?
        get() = aircraftTypesLiveData.value // null if not observed or not initialized yet (it is always observed)


    /**
     * Gets a map of all aircraft even when [aircraftListLiveData] is not ready yet
     */
    suspend fun requireMap(): Map<String, Aircraft> =
        if (aircraftMap.isNotEmpty()) aircraftMap
        else getFullAircraftList().map{it.registration to it}.toMap().also{
            aircraftMap = it
        }

    /**
     * Get ACRWT data from Dao (for SyncAircraftWorker)
     */
    suspend fun getAcrwtData(): List<AircraftRegistrationWithTypeData> = registrationDao.requestAllRegistrations()

    /**
     * SavesACRWT data to disk (for SyncAircraftWorker)
     */
    fun saveAcrwtData(data: List<AircraftRegistrationWithTypeData>) = saveAircraftRegistrationWithTypeData(data)

    /**
     * Replace entire Consensus DB with new data (for SyncAircraftWorker)
     */
    suspend fun replaceConsensusData(newData: Map<String, ByteArray>) {
        aircraftTypeConsensusDao.clearDb()
        newData.map{
            AircraftTypeConsensusData(it.key, it.value)
        }.let {
            aircraftTypeConsensusDao.save(*it.toTypedArray())
        }
    }




    /********************************************************************************************
     * Private parts that make it all work (private)
     ********************************************************************************************/

    /**
     * Sources:
     */
    private val acrwtLiveData = registrationDao.allRegistrationsFlow().map{it.map {acrwtd -> AircraftRegistrationWithType(acrwtd)} }.asLiveData()
    private val aircraftTypeLiveData = aircraftTypeDao.aircraftTypesFlow().map { it.map {atd -> atd.toAircraftType() } }.asLiveData()
    private val allFlights = FlightRepository.getInstance().liveFlights
    private val consensusLiveData = aircraftTypeConsensusDao.consensusDataFlow().map{ it.map{consensusData -> consensusData.toAircraftTypeConsensus().toAircraft()} }.asLiveData()
    private val preloadedRegistrationsLiveData = preloadedRegistrationsDao.registrationsFlow().asLiveData()

    // List of all AircraftTypes known in DB
    private val _aircraftTypesLiveData = MediatorLiveData<List<AircraftType>>().apply{
        addSource(aircraftTypeLiveData){
            value = it
        }
    }

    // List of all Aircraft known in DB
    private val _aircraftListLiveData = object: MediatorLiveData<List<Aircraft>>() {
        val mutex = Mutex()
        fun launchWithLock(function: suspend () -> Unit) = launch { mutex.withLock { function() } }
    }.apply {
        //This one to be observed first, because it is used in others as well
        addSource(aircraftTypesLiveData) { types ->
            launchWithLock {
                value = updateAircraftListWithNewFromFlights(allFlights.value ?: emptyList(), types )
                preloadedRegistrationsLiveData.value?.let {
                    value = updateAircraftListWithNewPreloaded(it, types )
                }
                acrwtLiveData.value?.let{
                    value = updateAircraftListWithNewAcrwt(it)
                }
            }
        }

        addSource(acrwtLiveData) {
            launchWithLock {
                value = aircraftTypeLiveData.value?.let { _ -> updateAircraftListWithNewAcrwt(it) }
            }
        }

        addSource(allFlights) {
            launchWithLock {
                value = aircraftTypeLiveData.value?.let { types -> updateAircraftListWithNewFromFlights(it,  types) }
            }
        }
        addSource(consensusLiveData) {
            launchWithLock {
                value = updateAircraftListWithNewConsensus(it)
            }
        }
        addSource(preloadedRegistrationsLiveData){
            launchWithLock {
                value = aircraftTypeLiveData.value?.let { types -> updateAircraftListWithNewPreloaded(it, types )}
            }
        }
    }

    // Map of [allAircraft] as [registration] to [aircraft]
    private val _aircraftMapLiveData = MediatorLiveData<Map<String, Aircraft>>().apply{
        addSource(aircraftListLiveData) {source ->
            source?.let { acList ->
                value = acList.map { it.registration to it }.toMap()
            }
        }
    }

    /**
     * Sorted registrations:
     * @see getSortedRegistrations
     */
    private val _sortedRegistrations = MediatorLiveData<List<String>>().apply{
        addSource(aircraftListLiveData){ value = getSortedRegistrations() }
        addSource(FlightRepository.getInstance().usedRegistrationsLiveData){
            value = getSortedRegistrations()
        }
    }

    /**
     * Observing aircraftMap forever, to make sure all MediatorLiveData are observed and working
     */
    init{
        launch { // make sure this happens on main thread
            _aircraftMapLiveData.observeForever {
                aircraftMap = it
            }
        }
    }

    /**
     * Observing consensusOptIn to make sure it is used or not
     * If [consensusLiveData] is empty, don't do anything as this will get triggered when it is filled.
     */

    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Preferences::consensusOptIn.name -> launch { _aircraftListLiveData.value = consensusLiveData.value?.let { updateAircraftListWithNewConsensus(it) } }
        }
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }

    /********************************************************************************************
     * Data saving functions to Dao (private)
     ********************************************************************************************/

    private fun saveAircraftRegistrationWithTypeData(dataToSave: AircraftRegistrationWithTypeData) = launch(dispatcher + NonCancellable) {
        registrationDao.save(dataToSave)
    }

    private fun saveAircraftRegistrationWithTypeData(dataToSave: List<AircraftRegistrationWithTypeData>) = launch(dispatcher + NonCancellable) {
        registrationDao.save( *(dataToSave.toTypedArray()))
    }

    private fun savePreloadedRegistrations(dataToSave: PreloadedRegistration) = launch (dispatcher + NonCancellable) {
        preloadedRegistrationsDao.save(dataToSave)
    }
    private fun savePreloadedRegistrations(dataToSave: List<PreloadedRegistration>) = launch (dispatcher + NonCancellable) {
        preloadedRegistrationsDao.save(*(dataToSave.toTypedArray()))
    }

    //This one will probably not be used
    private fun saveConsensus(dataToSave: AircraftTypeConsensus) = launch (dispatcher + NonCancellable) {
        aircraftTypeConsensusDao.save(dataToSave.toModel())
    }

    /**
     * Data will arrive from server as [ConsensusData], caller should take care of converting to model (with [ConsensusData.toModel])
     */
    private fun saveConsensus(dataToSave: List<AircraftTypeConsensus>) = launch (dispatcher + NonCancellable) {
        aircraftTypeConsensusDao.save(*(dataToSave.map{it.toModel()}.toTypedArray()))
    }

    private fun saveAircraftTypes(dataToSave: AircraftType) = launch (dispatcher + NonCancellable) {
        aircraftTypeDao.save(dataToSave.toModel())
    }

    private fun saveAircraftTypes(dataToSave: List<AircraftType>) = launch (dispatcher + NonCancellable) {
        aircraftTypeDao.save(*(dataToSave.map{it.toModel()}.toTypedArray()))
    }

    private fun replaceAircraftTypesDB(newTypes: List<AircraftType>) = launch(dispatcher + NonCancellable) {
        aircraftTypeDao.clearDb()
        saveAircraftTypes(newTypes)
    }

    private fun replacePreloadedTypesDB(newPreloaded: List<PreloadedRegistration>) = launch (dispatcher + NonCancellable) {
        preloadedRegistrationsDao.clearDb()
        savePreloadedRegistrations(newPreloaded)
    }

    /********************************************************************************************
     * Functions to transform other types to [Aircraft] (private)
     ********************************************************************************************/

    private fun AircraftRegistrationWithType.toAircraft() = Aircraft(
        registration,
        type,
        Aircraft.KNOWN
    )

    private fun PreloadedRegistration.toAircraft(types: List<AircraftType>?) = Aircraft(
        registration,
        types?.firstOrNull{ it.name == type},
        Aircraft.PRELOADED
    )




    /********************************************************************************************
     * Data loading functions for [aircraftListLiveData] (private)
     ********************************************************************************************/

    // Caches for non-changed parts:
    private var acrwtCache: List<Aircraft> = emptyList()
    private var acrwtRegs: List<String> = emptyList()

    private var aircraftFromFlightsCache: List<Aircraft> = emptyList()
    private var aircraftFromFlightsRegs: List<String> = emptyList()

    private var conflictingFlightsCache: List<Aircraft> = emptyList()
    private var conflictingFlightsRegs: List<String> = emptyList()

    private var preloadedAircraftCache: List<Aircraft> = emptyList()
    private var preloadedAircraftRegs: List<String> = emptyList()

    private var consensusCache: List<Aircraft> = emptyList()
    private var consensusRegs: List<String> = emptyList()


    /**
     * fill [acrwtCache] and [acrwtRegs] with data from Database (ie. cache it)
     * Not really needed to do this async, but doing that anyway to stay in line with other filler functions
     */
    private fun fillAcrwtCacheAsync(acrwt: List<AircraftRegistrationWithType>) = async(Dispatchers.Default) {
        acrwt.map{it.toAircraft()}.let {
            acrwtCache = it
            acrwtRegs = it.map{acrwt -> acrwt.registration}
        }
    }

    /**
     * fill [aircraftFromFlightsCache] and [aircraftFromFlightsRegs],
     * as well as [conflictingFlightsCache] and [conflictingFlightsRegs]
     * with data from Database (ie. cache it)
     */
    private fun fillAircraftFromFlightsCacheAsync(flights: List<Flight>? = null, types: List<AircraftType>) = async(dispatcher){
        getAircraftTypesFromFlights(flights ?: FlightRepository.getInstance().getAllFlights(), types).let {
            aircraftFromFlightsCache = it.first
            conflictingFlightsCache = it.second
            aircraftFromFlightsRegs = it.first.map { acFromFlight -> acFromFlight.registration }
            conflictingFlightsRegs = it.second.map {acFromFlight -> acFromFlight.registration}
        }
    }

    /**
     * fill [preloadedAircraftCache] and [preloadedAircraftRegs] with data from Database (ie. cache it)
     */
    private fun fillPreloadedAircraftCacheAsync(preloadedRegs: List<PreloadedRegistration>, types: List<AircraftType>) = async(dispatcher){
        preloadedRegs.map{ it.toAircraft(types)}.let {
            preloadedAircraftCache = it
            preloadedAircraftRegs = it.map{ac -> ac.registration}
        }
    }

    /**
     * fill [consensusCache] and [consensusRegs] with data from Database (ie. cache it)
     */
    private fun fillConsensusCacheAsync(consensus: List<Aircraft>) = async(Dispatchers.Default) {
        consensusCache = consensus
        consensusRegs = consensus.map { ac -> ac.registration }
    }


    /**
     * fill _cachedAircraftList, from top to bottom:
     * - Own saved aircraft data
     * - aircraft data from flights database
     * - preloaded data
     * - consensus data
     * - aircraft data from imported flights that is conflicting with other data from imported flights
     */

    /**
     * This one is for when we need to force an aircraft map and all data may not have been loaded yet.
     * Uses cached data where available.
     */
    private suspend fun getFullAircraftList(): List<Aircraft> = withContext(dispatcher){
        val fillers = emptyList<Deferred<Unit>>().toMutableList()
        val types = aircraftTypesLiveData.value ?: aircraftTypeDao.requestAllAircraftTypes().map{it.toAircraftType()}
        if(acrwtCache.isEmpty()){
            val acrwt = acrwtLiveData.value ?: registrationDao.requestAllRegistrations().map{ AircraftRegistrationWithType(it) }
            fillers.add(fillAcrwtCacheAsync(acrwt))
        }
        // fill both flihgts and conflicting
        if (aircraftFromFlightsCache.isEmpty()){
            val flights = allFlights.value ?: FlightRepository.getInstance().getAllFlights()
            fillers.add(fillAircraftFromFlightsCacheAsync(flights, types))
        }
        if (preloadedAircraftCache.isEmpty()){
            val preloadedRegs = preloadedRegistrationsLiveData.value ?: preloadedRegistrationsDao.requestAllRegistrations()
            fillers.add(fillPreloadedAircraftCacheAsync(preloadedRegs, types))
        }
        if(consensusCache.isEmpty()){
            val consensus = consensusLiveData.value ?: aircraftTypeConsensusDao.getAllConsensusData().map{consensusData -> consensusData.toAircraftTypeConsensus().toAircraft()}
            fillers.add(fillConsensusCacheAsync(consensus))
        }
        fillers.awaitAll()
        mergeCaches()
    }


    private suspend fun updateAircraftListWithNewAcrwt(acrwt: List<AircraftRegistrationWithType>): List<Aircraft> = withContext(dispatcher) {
        fillAcrwtCacheAsync(acrwt).await()
        mergeCaches()
    }

    private suspend fun updateAircraftListWithNewFromFlights(flights: List<Flight>? = null, types: List<AircraftType>): List<Aircraft> = withContext(dispatcher) {
        fillAircraftFromFlightsCacheAsync(flights, types).await()
        mergeCaches()
    }

    private suspend fun updateAircraftListWithNewPreloaded(preloadedRegs: List<PreloadedRegistration>, types: List<AircraftType>): List<Aircraft> = withContext(dispatcher) {
        fillPreloadedAircraftCacheAsync(preloadedRegs, types).await()
        mergeCaches()
    }

    private suspend fun updateAircraftListWithNewConsensus(consensus: List<Aircraft>): List<Aircraft> = withContext(dispatcher) {
        // If not opted in, make sure consensusCache is empty
        fillConsensusCacheAsync(if (Preferences.consensusOptIn) consensus else emptyList()).await()
        mergeCaches()

    }

    /**
     * Merge all caches to one List<Aircraft>.
     * Also updates used registrations for each cache, as they might have been promoted to a higher priority cache (adding a duplicate to a lower priority gets discarded)
     * in order:
     * - ACRWT (AirCraft Registration With Type)
     * - Aicraft from Flights DB
     * - Preloaded aircraft from server
     * - Aircraft from server consensus
     * - aircraft data from imported flights that is conflicting with other data from imported flights
     */
    private fun mergeCaches(): List<Aircraft> =
        acrwtCache +
                aircraftFromFlightsCache.filter{it.registration !in acrwtRegs} +
                preloadedAircraftCache.filter{it.registration !in acrwtRegs + aircraftFromFlightsRegs} +
                consensusCache.filter {it.registration !in acrwtRegs + aircraftFromFlightsRegs + preloadedAircraftRegs} +
                conflictingFlightsCache.filter {it.registration !in acrwtRegs + aircraftFromFlightsRegs + preloadedAircraftRegs + consensusRegs}


    /**
     * Get aircraft types from a list of flights
     * Will return a pair of Lists of Aircraft with [Aircraft.source] = [Aircraft.FLIGHT] or [Aircraft.FLIGHT_CONFLICTING]
     * pair will be <list of aircraft without conflict> to < list of aircraft with conflict>
     * A conflicting flight is one where multiple solutions are found.
     */
    private suspend fun getAircraftTypesFromFlights(flights: List<Flight>, types: List<AircraftType>): Pair<List<Aircraft>, List<Aircraft>> = withContext(Dispatchers.Default){
        val typesMap = types.map { it.shortName.uppercase(Locale.ROOT) to it }.toMap()

        val regsToCheck = flights.filter{it.registration.isNotBlank()}
            .map { it.registration to it.aircraftType.uppercase(Locale.ROOT) } // now we have a list of registrations paired with types

        //conflicts is a list of those registrations that match multiple types in flights database
        val conflicts = findConflicts(regsToCheck)
        val conflictingRegs = conflicts.map{it.first}.toSet()

        //Lists of pairs or Reg to Type (one per registration)
        val nonConflictingAircraftRegAndType = regsToCheck.toSet().filter{it.first !in conflictingRegs && it.second in typesMap.keys}
        val conflictingAircraftRegAndType = conflictingRegs.map{distinctReg -> regsToCheck.filter{it.first == distinctReg && it.second in typesMap.keys}.mostCommonOrNull()}.filterNotNull()
        regsToCheck.filter{it !in nonConflictingAircraftRegAndType && it !in conflictingAircraftRegAndType}

        //return:
        nonConflictingAircraftRegAndType.map{rt ->  Aircraft(registration = rt.first, type = typesMap[rt.second], source = Aircraft.FLIGHT)} to
                conflictingAircraftRegAndType.map{rt ->  Aircraft(registration = rt.first, type = typesMap[rt.second], source = Aircraft.FLIGHT_CONFLICTING)}
    }

    /**
     * Will return all conflicts, as a list of (distinct_registration to all occurences (to make an educated guess which one is right)
     */
    private fun findConflicts(regsToCheck: List<Pair<String, String>>): List<Pair<String, String>>{
        val foundRegs = regsToCheck.map{it.first}.toSet()
        val regsMap = foundRegs.map {foundReg-> foundReg to regsToCheck.filter { it.first == foundReg }}.toMap()
        return regsMap.filterValues { it.distinct().size > 1 }.values.flatten()
    }

    /********************************************************************************************
     * Public observables and functions
     ********************************************************************************************/

    /**
     * Will make a map of short names (UPPERCASE) to AircraftType
     */
    fun getAircraftTypesMapShortNameAsync() = async {
        aircraftTypes?.map { it.shortName.uppercase(Locale.ROOT) to it }?.toMap()
    }

    /**
     * Get aircraft from registration, with or without '-'.
     * @return found flight, null if [reg] is null or nothing found.
     */
    suspend fun getAircraftFromRegistration(reg: String?): Aircraft? = withContext(dispatcher) {
        when {
            reg == null -> null
            // use [reg] if it contains at least one '-', if it doesn't try to match it to a knownRegistration minus the '-'
            "-" in reg -> requireMap()[reg.uppercase(Locale.ROOT)]
            else -> { requireMap().keys.firstOrNull { it.filter{ c -> c !in "- "} == reg}?.let{
                requireMap()[it]
            }
            }
        }
    }


    fun getAircraftType(typeName: String): AircraftType? =
        aircraftTypes?.firstOrNull{ it.name == typeName }

    fun getAircraftTypeByShortName(shortName: String): AircraftType? =
        aircraftTypes?.firstOrNull{ it.shortName == shortName }

    /**
     * Sorted registrations:
     * - First all used registrations from [FlightRepository], then all other regs as sorted in this repository
     */
    fun getSortedRegistrations(): List<String> =
        FlightRepository.getInstance().usedRegistrations +
        (aircraftListLiveData.value ?: emptyList()).map { ac -> ac.registration }
        .distinct()

    /**
     * Searches for a match in order of priority in aircraftMap
     * @see [findSortedHitsForRegistration] for ordering
     */
    fun getBestHitForPartialRegistration(r: String): Aircraft? =
        aircraftMap[findBestHitForRegistration(r, getSortedRegistrations())]

    /**
     * Save aircraft to ACRWT Database
     * TODO make SyncAircraftTypesWorker do consensus things
     * TODO check if SyncAircraftTypesWorker is doing those things and think about if that might cause concurrency problems and if so how to fix them
     */
    fun saveAircraft(aircraftToSave: Aircraft) {
        if (aircraftToSave.type?.name == null) return // do nothing as no new type data known
        val newAcrwt: AircraftRegistrationWithType = acrwtLiveData.value?.firstOrNull{it.registration == aircraftToSave.registration}?.let {
            //If we get here, an aircraft with that registration already exists. We need to update it's data for consensus. (or not, if it is the same)
            when{
                aircraftToSave.type == it.type  -> it                             // do nothing as this is already known as is
                !it.knownToServer -> it.copy(type = aircraftToSave.type)               // Just change type as server was not informed about changes yet
                else -> {  it.copy(                                               // Set previousType to current type, update type to new type and set!knownToServer
                    previousType = it.type,
                    type = aircraftToSave.type,
                    knownToServer = false
                )
                }
            }

        } ?: AircraftRegistrationWithType(aircraftToSave.registration, aircraftToSave.type, knownToServer = false)
        saveAircraftRegistrationWithTypeData(newAcrwt.toModel())
    }

    fun saveAircraft(aircraftToSave: List<Aircraft>) {
        aircraftToSave.forEach{
            saveAircraft(it)
        }
    }
/*
    fun deleteAircraft(aircraft: Aircraft){
        // Not implemented
        //Not sure if needed, but if it is, this will be where it goes
    }

 */

    /********************************************************************************************
     * Public sync related functions
     ********************************************************************************************/

    fun replaceAllTypesWith(newTypes: List<AircraftType>) = replaceAircraftTypesDB(newTypes)

    fun replaceAllPreloadedWith(newPreloaded: List<PreloadedRegistration>) = replacePreloadedTypesDB(newPreloaded)

 */

    /********************************************************************************************
     * Companion object
     ********************************************************************************************/

}

