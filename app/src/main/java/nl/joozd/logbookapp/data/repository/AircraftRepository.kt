/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

package nl.joozd.logbookapp.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ConsensusData
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.AircraftTypeConsensusDao
import nl.joozd.logbookapp.data.room.dao.AircraftTypeDao
import nl.joozd.logbookapp.data.room.dao.PreloadedRegistrationsDao
import nl.joozd.logbookapp.data.room.dao.RegistrationDao
import nl.joozd.logbookapp.data.room.model.*
import nl.joozd.logbookapp.extensions.mostCommonOrNull
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.util.*

//TODO replace all observeForevers with getters

/**
 * New implementation of aircraftRepository.
 * Now everything boils down to serving ready-made Aircraft.
 */
class AircraftRepository(
    private val aircraftTypeDao: AircraftTypeDao,
    private val registrationDao: RegistrationDao,
    private val aircraftTypeConsensusDao: AircraftTypeConsensusDao,
    private val preloadedRegistrationsDao: PreloadedRegistrationsDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {

    /********************************************************************************************
     * Private parts that make it all work (private)
     ********************************************************************************************/

    // Cached data as MutableLiveData with getters/setters for local access
    private val _cachedAircraftTypes = MutableLiveData<List<AircraftType>>()

    //This sets its field async!
    private var cachedAircraftTypes
        get() = _cachedAircraftTypes.value
        set(it) { launch(Dispatchers.Main) { _cachedAircraftTypes.value = it } }

    private val _cachedConsensus= MutableLiveData<List<AircraftTypeConsensus>>()
    private var cachedConsensus
        get() = _cachedConsensus.value
        set(it) { launch(Dispatchers.Main) { _cachedConsensus.value = it } }
    private val _cachedAircraftRegistrationWithTypeData = MutableLiveData<List<AircraftRegistrationWithTypeData>>()
    private var cachedAircraftRegistrationWithTypeData
        get() = _cachedAircraftRegistrationWithTypeData.value
        set(it) { launch(Dispatchers.Main) { _cachedAircraftRegistrationWithTypeData.value = it} }
    //this one is a map because better
    private val _cachedPreloadedRegistrations = MutableLiveData<List<PreloadedRegistration>>()
    private var cachedPreloadedRegistrations
        get() = _cachedPreloadedRegistrations.value
        set(it) { launch(Dispatchers.Main) { _cachedPreloadedRegistrations.value = it} }
    private var preloadedRegistrationsMap: Map<String, String>? = null

    private val _cachedAircraftList = MediatorLiveData<List<Aircraft>>()
    private var cachedAircraftList
        get() = _cachedAircraftList.value ?: emptyList()
        set(it) { launch(Dispatchers.Main) { _cachedAircraftList.value = it} }

    private val aircraftMap: Map<String, Aircraft>?
        get() = _cachedAircraftList.value?.map{it.registration to it}?.reversed()?.toMap()

    private val aircraftTypesMap: Map<String, AircraftType>?
        get() = _cachedAircraftTypes.value?.map{act -> act.name to act}?.toMap()

    init{
        launch {
             cachedAircraftList = buildCachedAircraftListFromScratch()
        }
        getAircraftTypeLiveData().observeForever { cachedAircraftTypes = it }
        getConsensusLiveData().observeForever { cachedConsensus = it }
        getAircraftRegistrationsWithTypeLiveData().observeForever { cachedAircraftRegistrationWithTypeData = it }
        getPreloadedRegistrationsLiveData().observeForever {
            cachedPreloadedRegistrations = it
        }
/*
replaced with getter
        _cachedAircraftTypes.observeForever {
            aircraftTypesMap = it.map{act -> act.name to act}.toMap()
        }

 */


        _cachedPreloadedRegistrations.observeForever {
            preloadedRegistrationsMap = it.map {plr ->  plr.registration to plr.type }.toMap()
        }

        _cachedAircraftList.addSource(_cachedAircraftTypes){
            launch { cachedAircraftList = buildCachedAircraftListFromScratch() }
        }
        _cachedAircraftList.addSource(_cachedAircraftRegistrationWithTypeData){
            launch { cachedAircraftList = updateAircraftListWithNewAcrwt() }
        }
        _cachedAircraftList.addSource(_cachedPreloadedRegistrations){
            launch { cachedAircraftList = updateAircraftListWithNewPreloaded() }
        }
        _cachedAircraftList.addSource(_cachedConsensus){
            launch { cachedAircraftList = updateAircraftListWithNewConsensus() }
        }

        /*
         * replaced with getter
        _cachedAircraftList.observeForever { acl ->
            aircraftMap = acl.map{it.registration to it}.reversed().toMap()     // reversed to higher priority registrations will overwrite lower ones
        }

         */
    }

    /********************************************************************************************
     * Data loading functions from Dao (private)
     ********************************************************************************************/

    private fun getAircraftTypeLiveData() = Transformations.map(aircraftTypeDao.requestLiveAircraftTypes()) {it.map {atd -> atd.toAircraftType()}}
    private suspend fun getAircraftTypes(forceReload: Boolean = false) = withContext(dispatcher) {
        if (forceReload)
            cachedAircraftTypes = aircraftTypeDao.requestAllAircraftTypes().map{it.toAircraftType()}
        cachedAircraftTypes ?: aircraftTypeDao.requestAllAircraftTypes().map{it.toAircraftType()}.also{cachedAircraftTypes = it}
    }

    private fun getConsensusLiveData() = Transformations.map(aircraftTypeConsensusDao.getLiveConsensusData()) { it.map{cd -> cd.toAircraftTypeConsensus() }}
    private suspend fun getConsensusData(forceReload: Boolean = false) = withContext(dispatcher){
        if (forceReload)
            cachedConsensus = aircraftTypeConsensusDao.getAllConsensusData().map{it.toAircraftTypeConsensus()}
        cachedConsensus ?: aircraftTypeConsensusDao.getAllConsensusData().map{it.toAircraftTypeConsensus()}.also{cachedConsensus = it}
    }

    private fun getAircraftRegistrationsWithTypeLiveData() = registrationDao.requestLiveRegistrations()
    private suspend fun getAircraftRegistrationsWithType(forceReload: Boolean = false) = withContext(dispatcher) {
        if (forceReload)
            cachedAircraftRegistrationWithTypeData = registrationDao.requestAllRegistrations()
        cachedAircraftRegistrationWithTypeData ?: registrationDao.requestAllRegistrations().also{cachedAircraftRegistrationWithTypeData = it}
    }

    private fun getPreloadedRegistrationsLiveData() = preloadedRegistrationsDao.requestLiveRegistrations()
    private suspend fun getPreloadedRegistrations(forceReload: Boolean = false) = withContext(dispatcher) {
        if (forceReload)
            cachedPreloadedRegistrations = preloadedRegistrationsDao.requestAllRegistrations()
        cachedPreloadedRegistrations ?: preloadedRegistrationsDao.requestAllRegistrations().also{ cachedPreloadedRegistrations = it }
    }

    /********************************************************************************************
     * Data saving functions to Dao (private)
     ********************************************************************************************/

    private fun saveAircraftRegistrationWithTypeData(dataToSave: AircraftRegistrationWithTypeData) = launch(dispatcher + NonCancellable) {
        registrationDao.save(dataToSave.apply{timestamp = TimestampMaker.nowForSycPurposes})
        launch{
            getAircraftTypes(true)
        }
    }

    private fun saveAircraftRegistrationWithTypeData(dataToSave: List<AircraftRegistrationWithTypeData>) = launch(dispatcher + NonCancellable) {
        registrationDao.save( *(dataToSave.map{it.apply {timestamp = TimestampMaker.nowForSycPurposes}}.toTypedArray()))
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
    private fun saveConsensus(dataToSave: List<AircraftTypeConsensus>) = launch (dispatcher + NonCancellable) {
        aircraftTypeConsensusDao.save(*(dataToSave.map{it.toModel()}.toTypedArray()))
    }
    private fun saveConsensus(dataToSave: ConsensusData) = launch (dispatcher + NonCancellable) {
        aircraftTypeConsensusDao.save(dataToSave.toModel())
    }
    // Stupid java won't let me call this the same
    private fun saveConsensusFromCommon(dataToSave: List<ConsensusData>) = launch (dispatcher + NonCancellable) {
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

    private suspend  fun getAircraftTypeFromName(name: String): AircraftType? = withContext(dispatcher) { getAircraftTypes().firstOrNull{ it.name == name} }

    private suspend fun AircraftRegistrationWithTypeData.toAircraft() =
        Aircraft(
            registration,
            getAircraftTypeFromName(type),
            Aircraft.KNOWN
        )

    private suspend fun PreloadedRegistration.toAircraft() =
        Aircraft(
            registration,
            getAircraftTypeFromName(type),
            Aircraft.PRELOADED
        )

    private fun AircraftTypeConsensus.toAircraft() =
        Aircraft(
            registration,
            aircraftType,
            Aircraft.CONSENSUS
        )


    /********************************************************************************************
     * Data loading functions for [_cachedAircraftList] (private)
     ********************************************************************************************/

    // Caches for non-changed parts:
    private var acrwtCache: List<Aircraft> = emptyList()
    private var acrwtRegs: List<String> = emptyList()

    private var flightsCache: List<Aircraft> = emptyList()
    private var flightsRegs: List<String> = emptyList()

    private var conflictingFlightsCache: List<Aircraft> = emptyList()
    private var conflictingFlightsRegs: List<String> = emptyList()

    private var preloadedCache: List<Aircraft> = emptyList()
    private var preloadedRegs: List<String> = emptyList()

    private var consensusCache: List<Aircraft> = emptyList()
    private var consensusRegs: List<String> = emptyList()

    /**
     * fill _cachedAircraftList, from top to bottom:
     * - Own saved aircraft data
     * - aircraft data from flights database
     * - preloaded data
     * - consensus data
     * - aircraft data from imported flights that is conflicting with other data from imported flights
     */
    private suspend fun buildCachedAircraftListFromScratch(): List<Aircraft> = withContext(dispatcher) {
        val allAircraftAsync = async { FlightRepository.getInstance().getAllFlights() }
        ((getAircraftRegistrationsWithType().map{it.toAircraft()})
            .also {
            acrwtCache = it
            acrwtRegs = it.map{acrwt -> acrwt.registration}
        }) +
                getAircraftTypesFromFlights(allAircraftAsync.await(), exclude = acrwtRegs).also {
                    flightsCache = it.first
                    conflictingFlightsCache = it.second
                    flightsRegs = it.first.map { acFromFlight -> acFromFlight.registration }
                    conflictingFlightsRegs = it.second.map {acFromFlight -> acFromFlight.registration}
                }.first +
                (getPreloadedRegistrations().map{ it.toAircraft()})
                    .filter{it.registration !in acrwtRegs}
                    .also {
                        preloadedCache = it
                        preloadedRegs = it.map{ac -> ac.registration}
                    } +
                getConsensusData().map{it.toAircraft()}
                    .filter{it.registration !in acrwtRegs && it.registration !in preloadedRegs}
                    .also{
                        consensusCache = it
                        consensusRegs = it.map{ac -> ac.registration}
                    } +
                conflictingFlightsCache.filter{it.registration !in preloadedRegs + consensusRegs}

    }

    private suspend fun updateAircraftListWithNewAcrwt(): List<Aircraft> = withContext(dispatcher) {
        (getAircraftRegistrationsWithType().map{it.toAircraft()}
            .also {
            acrwtCache = it
            acrwtRegs = it.map{acrwt -> acrwt.registration}
        }) +
                flightsCache.filter{it.registration !in acrwtRegs}.also{
                    flightsCache = it
                    flightsRegs = it.map{ac -> ac.registration}
                } + preloadedCache.filter{it.registration !in acrwtRegs}.also{
                    preloadedCache = it
                    preloadedRegs = it.map{ac -> ac.registration}
                } + consensusCache.filter { it.registration !in acrwtRegs}.also {
                    consensusCache = it
        }

    }

    private suspend fun updateAircraftListWithNewFromFlights(): List<Aircraft> = withContext(dispatcher) {
        val flights = FlightRepository.getInstance().getAllFlights()
        acrwtCache +
                getAircraftTypesFromFlights(flights, acrwtRegs).also{
                    flightsRegs = it.first.map{ac -> ac.registration}
                    flightsCache = it.first

                    conflictingFlightsRegs = it.second.map{ac -> ac.registration}
                    conflictingFlightsCache = it.second
                }.first + preloadedCache.filter{it.registration !in flightsRegs}.also{
                    preloadedCache = it
                    preloadedRegs = it.map{ac -> ac.registration}
                } + consensusCache.filter { it.registration !in flightsRegs}.also {
                    consensusCache = it
                } + conflictingFlightsCache.filter { it.registration !in preloadedRegs + consensusRegs }


    }

    private suspend fun updateAircraftListWithNewPreloaded(): List<Aircraft> = withContext(dispatcher) {
        acrwtCache + flightsCache +(getPreloadedRegistrations().map{ it.toAircraft()}
            .filter{it.registration !in acrwtRegs && it.registration !in flightsRegs}
            .also {
                preloadedCache = it
                preloadedRegs = it.map{p -> p.registration}
            }) + consensusCache.filter { it.registration !in preloadedRegs}.also{
            consensusCache = it
        }
    }

    private suspend fun updateAircraftListWithNewConsensus(): List<Aircraft> = withContext(dispatcher) {
        acrwtCache + flightsCache + preloadedCache + (getConsensusData().map{it.toAircraft()}
            .filter{it.registration !in acrwtRegs && it.registration !in flightsRegs && it.registration !in preloadedRegs}
            .also{ consensusCache = it})
    }

    /**
     * Get aircraft types from a list of flights
     * Will return a pair of Lists of Aircraft with [Aircraft.source] = [Aircraft.FLIGHT] or [Aircraft.FLIGHT_CONFLICTING]
     * pair will be <list of aircraft without conflict> to < list of aircraft with conflict>
     * A conflicting flight is one where multiple solutions are found.
     */
    private suspend fun getAircraftTypesFromFlights(flights: List<Flight>, exclude: List<String>): Pair<List<Aircraft>, List<Aircraft>> = withContext(Dispatchers.Default){
        val typesMapAsync = getAircraftTypesMapShortNameAsync()

        val regsToCheck = flights.filter{it.registration.isNotBlank() && it.registration !in exclude}
            .map{it.registration to it.aircraftType.toUpperCase(Locale.ROOT)} // now we have a list of registrations paired with types

        //conflicts is a list of those registrations that match multiple types in flights database
        val conflicts = findConflicts(regsToCheck).also{
            Log.d("FOUND CONFLICTS", "Found conflicts: ${it.distinct()}")
        }
        val conflictingRegs = conflicts.map{it.first}.toSet()

        val typesMap = typesMapAsync.await()

        //Lists of pairs or Reg to Type (one per registration)
        val nonConflictingAircraftRegAndType = regsToCheck.toSet().filter{it.first !in conflictingRegs && it.second in typesMap.keys}
        val conflictingAircraftRegAndType = conflictingRegs.map{distinctReg -> regsToCheck.filter{it.first == distinctReg && it.second in typesMap.keys }.mostCommonOrNull()}.filterNotNull()
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
     * async map / list builders in case data not loaded (private)
     ********************************************************************************************/

    private fun getAircraftMap() = async {
        aircraftMap
            ?: buildCachedAircraftListFromScratch()
                .also {cachedAircraftList = it}
                .map{it.registration to it}
                .toMap()
    }

    private fun getAircraftTypesMapAsync() = async {
        aircraftTypesMap
            ?: getAircraftTypes()
                // .also {cachedAircraftTypes = it} No need to set that twice I reckon? GetAircraftTypes already sets it
                .map{ it.name to it}
                .toMap()
    }

    private fun getKnownRegistrations() = async {
        getAircraftMap().await().keys
    }

    /********************************************************************************************
     * Misc helper functions (private)
     ********************************************************************************************/


    /********************************************************************************************
     * Public observables and functions
     ********************************************************************************************/

    val liveAircraftTypes: LiveData<List<AircraftType>>
        get() = _cachedAircraftTypes

    val liveAircraftList: LiveData<List<Aircraft>>
        get() = _cachedAircraftList

    suspend fun requestAircraftMap() = withContext(dispatcher) { getAircraftMap().await() }

    suspend fun requestAircraftTypesMap() = withContext(dispatcher) { getAircraftTypesMapAsync().await() }

    /**
     * Will make a map of short names (UPPERCASE) to AircraftType
     */
    fun getAircraftTypesMapShortNameAsync() = async {
        getAircraftTypes().map{ it.shortName.toUpperCase(Locale.ROOT) to it}.toMap()
    }

    suspend fun getAircraftFromRegistration(reg: String?): Aircraft? =
        if (reg == null) null else
        // use [reg] if it contains at least one '-', if it doesn't try to match it to a knownRegistration minus the '-'
        (if ("-" in reg) reg else getKnownRegistrations().await().firstOrNull { r -> r.filter{it != '-'} == reg } ?: reg ).let {
            getAircraftMap().await()[it.toUpperCase(Locale.ROOT)]
        }


    suspend fun searchAircraftFromRegistration(reg: String): List<Aircraft> {
        val map = getAircraftMap()
        return getKnownRegistrations().await()
            .filter { reg.toUpperCase(Locale.ROOT) in it }
            .mapNotNull { map.await()[it] }
    }

    suspend fun getAircraftType(typeName: String): AircraftType? =
        getAircraftTypesMapAsync().await()[typeName]

    suspend fun getAircraftTypeByShortName(shortName: String) = getAircraftTypes().firstOrNull {type -> type.shortName == shortName }


    /**
     * Searches for a match in order of priority in aircraftMap
     * - Hits at end of registration first (ie. "XZ" hits PH-XZA before "XZ-PHK"
     */
    suspend fun getBestHitForPartialRegistration(r: String): Aircraft? = r.toUpperCase(Locale.ROOT).let { reg ->
        val map = getAircraftMap().await()
        map[reg]
            ?: map[reg.findBestHitForRegistration(acrwtCache.map { it.registration })]
            ?: map[reg.findBestHitForRegistration(preloadedCache.map { it.registration })]
            ?: map[reg.findBestHitForRegistration(consensusCache.map { it.registration })]
    }

    suspend fun saveAircraft(aircraftToSave: Aircraft): Boolean = withContext(dispatcher + NonCancellable){
        if (aircraftToSave.type == null) return@withContext false
        val newAcrwt: AircraftRegistrationWithTypeData = getAircraftRegistrationsWithType().firstOrNull{it.registration == aircraftToSave.registration}?.apply {
            when{
                aircraftToSave.registration == registration -> {} // do nothing
                !knownToServer -> type = aircraftToSave.type.name
                else -> {
                    previousType = type
                    type = aircraftToSave.type.name
                    knownToServer = false
                }
            }

        } ?: AircraftRegistrationWithTypeData(aircraftToSave.registration, aircraftToSave.type.name)
        //TODO write to cache and to livedata
        saveAircraftRegistrationWithTypeData(newAcrwt)
        true
    }

    fun saveAircraft(aircraftToSave: List<Aircraft>) {
        TODO("Not implemented")
        // Put them in ACRWT database
    }

    fun deleteAircraft(aircraft: Aircraft){
        TODO("Not implemented")
        //Not sure if needed, but if it is, this will be where it goes
    }

    /********************************************************************************************
     * Public sync related functions
     ********************************************************************************************/

    fun replaceAllTypesWith(newTypes: List<AircraftType>) = replaceAircraftTypesDB(newTypes)

    fun replaceAllPreloadedWith(newPreloaded: List<PreloadedRegistration>) = replacePreloadedTypesDB(newPreloaded)

    fun checkIfAircraftTypesUpToDate(){
        Log.d(this::class.simpleName,"Firing JoozdlogWorkersHub.synchronizeAircraftTypes()")
        JoozdlogWorkersHub.synchronizeAircraftTypes()
    }


        /********************************************************************************************
         * Companion object
         ********************************************************************************************/

        companion object {
        private var singletonInstance: AircraftRepository? = null
        fun getInstance(): AircraftRepository = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val aircraftTypeDao = dataBase.aircraftTypeDao()
                    val registrationDao = dataBase.registrationDao()
                    val aircraftTypeConsensusDao = dataBase.aircraftTypeConsensusDao()
                    val preloadedRegistrationsDao = dataBase.preloadedRegistrationsDao()
                    singletonInstance = AircraftRepository(
                        aircraftTypeDao,
                        registrationDao,
                        aircraftTypeConsensusDao,
                        preloadedRegistrationsDao
                    )
                    singletonInstance!!
                }
        }
    }
}