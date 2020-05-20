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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.AircraftTypeConsensusDao
import nl.joozd.logbookapp.data.room.dao.AircraftTypeDao
import nl.joozd.logbookapp.data.room.dao.RegistrationDao
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.room.dao.PreloadedRegistrationsDao
import nl.joozd.logbookapp.data.room.model.*
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.util.*

/**
 * Gets data for aircraft (types, specific aircraft (by registration) or consensus (reg + probable type)
 */

@Deprecated ("Switch to AircraftRepository")
class AircraftRepositoryOld(
    private val aircraftTypeDao: AircraftTypeDao,
    private val registrationDao: RegistrationDao,
    private val aircraftTypeConsensusDao: AircraftTypeConsensusDao,
    private val preloadedRegistrationsDao: PreloadedRegistrationsDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {

    private val _cachedAircraftTypes = MutableLiveData<List<AircraftType>>()
    init{
        launch{
            _cachedAircraftTypes.value = getAllAircraftTypesAsync(true).await()
        }
        aircraftTypeDao.requestLiveAircraftTypes().observeForever { _cachedAircraftTypes.value = it.map{it.toAircraftType()} }
    }
    private val _cachedConsensus= MutableLiveData<List<AircraftTypeConsensus>>()
    init {
        launch { _cachedConsensus.value = getConsensusData(true) }
        aircraftTypeConsensusDao.getLiveConsensusData().observeForever { _cachedConsensus.value = it.map {model -> model.toAircraftTypeConsensus() }}
    }
    private val _cachedRegistrations = MutableLiveData<List<AircraftRegistrationWithTypeData>>()
    init{
        launch { _cachedRegistrations.value = getAircraftRegistrationsWithType(true) }
        getLiveRegistrations().observeForever { _cachedRegistrations.value = it }
    }
    private val _cachedPreloadedRegistrations = MutableLiveData<Map<String, String>>()
    init{
        launch{ getPreloadedRegistrations(true) }
        getLivePreloadedRegistrations().observeForever { _cachedPreloadedRegistrations.value = it }
    }

    val liveAircraftTypes: LiveData<List<AircraftType>> =
        Transformations.distinctUntilChanged((_cachedAircraftTypes))
    val liveConsensus: LiveData<List<AircraftTypeConsensus>> =
        Transformations.distinctUntilChanged(_cachedConsensus)
    val liveRegistrationWithTypes: LiveData<List<AircraftRegistrationWithTypeData>> =
        Transformations.distinctUntilChanged(_cachedRegistrations)


    private val _activeAircraft = MutableLiveData<Aircraft>()
    val activeAircraft: LiveData<Aircraft>
        get() = _activeAircraft
    /**
     * if updateActiveAircraft() gets both a [reg] and a [type], it ends up with
     * an aircraft with that reg and type and source = MANUAL.
     * If it only gets a [reg], it will search for a type to go with that, using [flight] if provided.
     * If it only gets a [type], it will set previous aircraft to that type, null stays null
     */
    fun updateActiveAircraft(reg: String? = null, type: AircraftType? = null, flight: Flight? = null){
        val registration =
            if (reg == null && type == null) // if only flight entered, set reg from flight
                flight?.registration
            else reg
        registration?.let{r ->
            val oldAircraftType = activeAircraft.value?.type
            launch(dispatcher) {
                var newAircraft = findAircraftByRegistration(r, flight)
                if (newAircraft.type == null && oldAircraftType != null) newAircraft = newAircraft.copy(type = oldAircraftType, source = -1)
                launch (Dispatchers.Main) { _activeAircraft.value =  newAircraft }
            }
        }
        type?.let {t ->
            _activeAircraft.value?.let{oldAircraft ->
                _activeAircraft.value = oldAircraft.copy(type = t, source = -1)
            }
        }
        Log.d("AircraftRepository", "activeAircraft is now ${activeAircraft.value}")
    }

    /**
     * Finds an Aircraft in DB
     * If not found, returns null
     * Searches by exact match first, then end to start (so XZ will return PH-EXZ before PH-XZA)
     */
    suspend fun findAircraft(reg: String?): Aircraft? = reg?.let { r ->
        Log.d("AircraftRepository", "searcing for registration: $reg")
        val knownRegsInDb =
            withContext(dispatcher) { getAircraftRegistrationsWithType().map { arwt -> arwt.registration } }
        Log.d("AircraftRepository", "${knownRegsInDb.size} regs found: $knownRegsInDb")

        withContext(Dispatchers.Default) { r.findBestHitForRegistration(knownRegsInDb) }?.let { foundRegs ->
            withContext(dispatcher) { findAircraftByRegistration(foundRegs) }
        }
    }. also{
        Log.d("AircraftRepository", "found: $it")
    }





    /********************************************************************************************
     * Aircraft Types Functions
     ********************************************************************************************/

    fun getAllAircraftTypesAsync(forceReload: Boolean = false) = async(dispatcher) {
        if (forceReload) aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }
        else _cachedAircraftTypes.value ?: aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }
    }

    fun getCachedAircraftTypes() = _cachedAircraftTypes.value

    suspend fun getAircraftType(name: String): AircraftType? = withContext(dispatcher) {
        if (_cachedAircraftTypes.value == null)
            aircraftTypeDao.getAircraftType(name)?.toAircraftType()
        else getCachedAircraftTypes()!!.firstOrNull{it.name == name}
    }

    suspend fun getAircraftTypeFromShortName(shortname: String): AircraftType? = withContext(dispatcher) {
        aircraftTypeDao.getAircraftTypeFromShortName(shortname)?.toAircraftType()
    }

    /**
     * searches for an aircrafttype. Needs exact name or shortName. If not specified which one, uses shortName.
     * If both entered, looks for [name] first, if no hits there it tries [shortName]
     */
    fun getAircraftTypeIfBuffered(shortName: String? = null, name: String? = null): AircraftType? {
        return name?.let { n ->
            _cachedAircraftTypes.value?.firstOrNull{it.name == n}
        } ?: shortName?.let { n ->
            return _cachedAircraftTypes.value?.firstOrNull{it.shortName == n}
        }
    }

    fun getAircraftTypeAsync(name: String): Deferred<AircraftType?> = async {
        aircraftTypeDao.getAircraftType(name)?.toAircraftType()
    }

    suspend fun saveAircraftTypes(types: List<AircraftType>) = withContext(NonCancellable) {
        Log.d(this::class.simpleName, "saving ${types.size} flights")
        aircraftTypeDao.save(*(types.map{it.toModel()}.toTypedArray()))
    }

    suspend fun clearAircraftTypeDb() = withContext(NonCancellable) { aircraftTypeDao.clearDb() }

    suspend fun replaceAllTypesWith(types: List<AircraftType>){
        clearAircraftTypeDb()
        saveAircraftTypes(types)
    }


    /********************************************************************************************
     * Aircraft Type Consensus Functions
     ********************************************************************************************/

    fun getLiveConsensusData() = aircraftTypeConsensusDao.getLiveConsensusData()

    suspend fun getConsensusData(forceReload: Boolean = false) = withContext(dispatcher){
        if (forceReload)
            aircraftTypeConsensusDao.getAllConsensusData().map{ it.toAircraftTypeConsensus() }
        else _cachedConsensus.value ?: aircraftTypeConsensusDao.getAllConsensusData().map{ it.toAircraftTypeConsensus() }
    }

    suspend fun getConsensusType(registration: String?): AircraftType? = withContext(dispatcher) {
        aircraftTypeConsensusDao.getConsensus(registration)?.serializedType?.let {
            AircraftType.deserialize(it)
        }
    }

    /**
     * Find registrations that match [registration] in consensus DB
     * If [registration] has an exact match, return that registration
     * If [registration] matches multiple registrations (ie. XZ in PH-EXZ and PH-XZA) return all of those
     */
    suspend fun getRegistrationsWithConsensus(registration: String): List<String>{
        val allRegs = withContext(dispatcher) { getConsensusData().map { it.registration } }
        allRegs.firstOrNull{it == registration}?.let { return listOf(registration) }
        return allRegs.filter{registration in it}
    }

    /********************************************************************************************
     * Preloaded registrations Functions
     ********************************************************************************************/

    private fun getLivePreloadedRegistrations() = Transformations.map(preloadedRegistrationsDao.requestLiveRegistrations()){
        it.map{pr -> pr.registration to pr.type}.toMap()
    }

    suspend fun getPreloadedRegistrations(forceReload: Boolean = false): Map<String, String> {
        return if (forceReload || _cachedPreloadedRegistrations.value == null){
            preloadedRegistrationsDao.requestAllRegistrations()
                .map{ it.registration to it.type}
                .toMap()
                .also{
                    withContext (Dispatchers.Main){
                        _cachedPreloadedRegistrations.value = it
                    }
            }
        }
        else _cachedPreloadedRegistrations.value!!
    }

    suspend fun getAircraftFromPreloaded(reg: String): Aircraft?{
        val typeName = getPreloadedRegistrations()[reg] ?: return null
        return Aircraft(
            reg,
            getAircraftType(typeName),
            Aircraft.PRELOADED
        )
    }

    suspend fun findPreloadedRegistration(reg: String?): Aircraft? = reg?.let { r ->
        withContext(Dispatchers.Default) { r.findBestHitForRegistration(getPreloadedRegistrations().keys.toList()) }?.let{
            getAircraftFromPreloaded(it)
        }
    }


    /********************************************************************************************
     * Aircraft Functions
     ********************************************************************************************/

    suspend fun getAircraftRegistrationsWithType(forceReload: Boolean = false): List<AircraftRegistrationWithTypeData> = withContext(Dispatchers.IO) {
        if (forceReload) registrationDao.requestAllRegistrations()
        else _cachedRegistrations.value ?: registrationDao.requestAllRegistrations()
    }

    fun getLiveRegistrations() = registrationDao.requestLiveRegistrations()

    suspend fun saveAircraftRegistrationsWithType(acrwt: List<AircraftRegistrationWithTypeData>) = withContext(dispatcher + NonCancellable){
        registrationDao.save(*acrwt.toTypedArray())
    }
    suspend fun saveAircraftRegistrationsWithType(acrwt: AircraftRegistrationWithTypeData) = withContext(dispatcher + NonCancellable){
        registrationDao.save(acrwt)
    }

    fun updateAircraftRegistrationWithType(registration: String, type: AircraftType){
        val reg = registration.toUpperCase(Locale.ROOT)
        launch(NonCancellable) {
            val currentAcrwt =
                withContext(dispatcher) { getAircraftRegistrationsWithType() }.firstOrNull { it.registration == reg }
            val newAcrwt = when {
                currentAcrwt == null -> AircraftRegistrationWithTypeData(
                    reg,
                    type.name,
                    false,
                    timestamp = TimestampMaker.nowForSycPurposes
                )
                currentAcrwt.type == type.name -> currentAcrwt
                currentAcrwt.knownToServer -> AircraftRegistrationWithTypeData(
                    reg,
                    type.name,
                    false,
                    currentAcrwt.type,
                    TimestampMaker.nowForSycPurposes
                )
                else -> currentAcrwt.copy(
                    type = type.name,
                    timestamp = TimestampMaker.nowForSycPurposes
                )
            }
            saveAircraftRegistrationsWithType(newAcrwt).also{
                Log.d("AircraftRepository", "acrwt = $newAcrwt")
            }
        }
    }

    /**
     * Get an aircraft from it's registration
     * @param registration
     * @return AircraftRegistrationWithType object or null if none found
     */

    suspend fun getAircraftByRegistration(reg: String?): AircraftRegistrationWithTypeData? = withContext(Dispatchers.IO) {
        getAircraftRegistrationsWithType().firstOrNull { it.registration == reg }
    }

    //non-suspend version which only works if data cached (which it should be)
    fun getAircraftByRegistrationIfCached(reg: String?): AircraftRegistrationWithTypeData? =
        liveRegistrationWithTypes.value?.firstOrNull { it.registration == reg }


    companion object {
        private var singletonInstance: AircraftRepositoryOld? = null
        fun getInstance(): AircraftRepositoryOld = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val aircraftTypeDao = dataBase.aircraftTypeDao()
                    val registrationDao = dataBase.registrationDao()
                    val aircraftTypeConsensusDao = dataBase.aircraftTypeConsensusDao()
                    val preloadedRegistrationsDao = dataBase.preloadedRegistrationsDao()
                    singletonInstance = AircraftRepositoryOld(aircraftTypeDao, registrationDao, aircraftTypeConsensusDao, preloadedRegistrationsDao)
                    singletonInstance!!
                }
        }
    }

    private suspend fun findAircraftByRegistration(registration: String, flightData: Flight?): Aircraft {
        var source = Aircraft.NONE
        getAircraftByRegistration(registration)?.let{acwtd ->
            getAircraftType(acwtd.type)?.let{foundType ->
                return Aircraft(
                    registration,
                    foundType,
                    Aircraft.KNOWN
                )
            }?: Log.w("Aircraft", "saved type from acwtd \"${acwtd.type}\" not found in AircraftTypes DB")
        }
        //If we get here, no saved aircraft was found (or one was found but it's not in DB
        flightData?.let{ flight ->
            getAircraftTypeFromShortName(flight.aircraft)?.let{foundType ->
                return Aircraft(
                    registration,
                    foundType,
                    Aircraft.FLIGHT
                )
            }?: Log.w("Aircraft", "saved type from flight \"${flight.aircraft}\" not found in AircraftTypes DB")
        }
        //If we get here, no AircraftType found in saved aircraft, nor in [flightData]

        getConsensusType(registration)?.let{ foundType ->
            return Aircraft(
                registration,
                foundType,
                Aircraft.CONSENSUS
            )
        }
        //If we get here, no type found
        return Aircraft(
            registration,
            null,
            Aircraft.NONE
        )
    }

    private suspend fun findAircraftByRegistration(registration: String): Aircraft {
        getAircraftByRegistration(registration)?.let{acwtd ->
            getAircraftType(acwtd.type)?.let{foundType ->
                return Aircraft(
                    registration,
                    foundType,
                    Aircraft.KNOWN
                )
            }?: Log.w("Aircraft", "saved type from acwtd \"${acwtd.type}\" not found in AircraftTypes DB")
        }
        return Aircraft(
            registration,
            null,
            Aircraft.NONE
        )
    }

    /********************************************************************************************
     * Sync Functions
     ********************************************************************************************/

    fun checkIfAircraftTypesUpToDate(){
        JoozdlogWorkersHub.synchronizeAircraftTypes()
    }


}