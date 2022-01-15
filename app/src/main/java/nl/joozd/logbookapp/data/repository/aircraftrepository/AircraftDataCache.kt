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
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.AircraftRegistrationWithType
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration
import nl.joozd.logbookapp.data.room.dao.AircraftTypeConsensusDao
import nl.joozd.logbookapp.data.room.dao.AircraftTypeDao
import nl.joozd.logbookapp.data.room.dao.PreloadedRegistrationsDao
import nl.joozd.logbookapp.data.room.dao.RegistrationDao
import nl.joozd.logbookapp.data.room.model.*
import java.io.Closeable
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext

class AircraftDataCache(
    private val aircraftTypeDao: AircraftTypeDao,
    private val registrationDao: RegistrationDao,
    private val aircraftTypeConsensusDao: AircraftTypeConsensusDao,
    private val preloadedRegistrationsDao: PreloadedRegistrationsDao,

    // coroutineContext must have the same lifetime as the object creating this
    // otherwise a memory leak will occur.
    coroutineContext: CoroutineContext,
): CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext = coroutineContext + SupervisorJob()

    //Main Caches; functions will get from this and collectors will update this.
    private var cachedAircraftTypeData: List<AircraftType>? = null
    private var cachedRegistrationData: List<AircraftRegistrationWithType>? = null
    private var cachedAircraftTypeConsensusData: List<Aircraft>? = null
    private var cachedPreloadedRegistrationsData: List<PreloadedRegistration>? = null

    // registrations are all caps, only A-Z
    private val registrationToAircraftMap = HashMap<String, Aircraft>()


    private val _registrationToAircraftMapFlow = MutableStateFlow(registrationToAircraftMap)
    val registrationToAircraftMapFlow: StateFlow<Map<String, Aircraft>> = _registrationToAircraftMapFlow

    //This is the job that runs when updating
    private val initialUpdateJob = launch(Dispatchers.IO) {
        listOf(
            launch(Dispatchers.IO) {
                cachedAircraftTypeData = getAircraftTypes()
            },
            launch(Dispatchers.IO) {
                cachedRegistrationData = getRegistrationData()
            },
            launch(Dispatchers.IO) {
                cachedAircraftTypeConsensusData = getAircraftTypeConsensusData()
            },
            launch(Dispatchers.IO) {
                cachedPreloadedRegistrationsData = getPreloadedRegistrationsData()
            }
        ).joinAll()
        updateRegistrationToAircraftMap()
    }

    suspend fun getRegistrationToAircraftMap(): Map<String, Aircraft> {
        initialUpdateJob.join()
        return registrationToAircraftMap
    }

    //This can return a partial map if ran while map s being filled
    fun getRegistrationToAircraftMapOrEmptyMapIfNotLoadedYet() : Map<String, Aircraft> {
        return registrationToAircraftMap
    }

    suspend fun getAircraftTypes(): List<AircraftType> =
        cachedAircraftTypeData
            ?: aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }

    private suspend fun getRegistrationData(): List<AircraftRegistrationWithType> =
        cachedRegistrationData
            ?: registrationDao.requestAllRegistrations().map { it.toAircraftRegistrationWithType() }

    private suspend fun getAircraftTypeConsensusData(): List<Aircraft> =
        cachedAircraftTypeConsensusData
            ?: aircraftTypeConsensusDao.getAllConsensusData()
                .map { it.toAircraftTypeConsensus().toAircraft() }

    private suspend fun getPreloadedRegistrationsData(): List<PreloadedRegistration> =
        cachedPreloadedRegistrationsData
            ?: preloadedRegistrationsDao.requestAllRegistrations()


    private val collectionJobs = listOf(
        launchAircraftTypeFlowCollector(),
        launchAircraftWithRegistrationFlowCollector(),
        launchConsensusFlowCollector(),

    )

    private fun launchAircraftTypeFlowCollector() =
        getAndTransformAircraftTypeFlow().onEach {
            cachedAircraftTypeData = it
            updateRegistrationToAircraftMap()
        }.launchIn(this)

    private fun launchAircraftWithRegistrationFlowCollector() =
        getAndTransformAircraftRegistrationDataFlow().onEach {
            cachedRegistrationData = it
            updateRegistrationToAircraftMap()
        }.launchIn(this)

    private fun launchConsensusFlowCollector() =
        getAndTransformConsensusDataFlow().onEach{
            cachedAircraftTypeConsensusData = it
            updateRegistrationToAircraftMap()
        }.launchIn(this)

    private fun launchPreloadedRegistrationsFlowCollector() =
        getPreloadedRegistrationsFlow().onEach{
            cachedPreloadedRegistrationsData = it
            updateRegistrationToAircraftMap()
        }.launchIn(this)


    private fun updateRegistrationToAircraftMap(){
        registrationToAircraftMap.clear()
        //The order matters as values will be overwritten when we get down the list
        //If this takes too much time we can wrap this in a `launch(Dispatchers.Default)` block
        addConsensusDataToAircraftMap()
        addPreloadedRegistrationsDataToAircraftMap()
        addAircraftWithRegistrationsToAircraftMap()

        updateCacheUpdatedFlow()
    }

    private fun addConsensusDataToAircraftMap(){
        cachedAircraftTypeConsensusData?.forEach{
            registrationToAircraftMap[formatRegistration(it.registration)] = it
        }
    }

    /*
     * This will not do anything if cachedAircraftTypeData is not filled yet,
     * but it will run again once it is.
     */
    private fun addPreloadedRegistrationsDataToAircraftMap(){
        cachedAircraftTypeData?.let { types ->
            cachedPreloadedRegistrationsData?.forEach {
                val aircraft = it.toAircraft(types)
                registrationToAircraftMap[formatRegistration(aircraft.registration)] = aircraft
            }
        }
    }

    private fun addAircraftWithRegistrationsToAircraftMap(){
        cachedRegistrationData?.forEach {
            registrationToAircraftMap[formatRegistration(it.registration)] = it.toAircraft()
        }
    }

    private fun updateCacheUpdatedFlow(){
        _registrationToAircraftMapFlow.value = registrationToAircraftMap
    }



    private fun getAndTransformAircraftTypeFlow() =
        aircraftTypeDao.aircraftTypesFlow().map { aircraftTypesDataToAircraftTypes(it) }

    private fun getAndTransformAircraftRegistrationDataFlow() =
        registrationDao.allRegistrationsFlow().map {
            aircraftRegistrationWithTypeDataListToAircraftRegistrationWithTypeList(it)
        }

    private fun getAndTransformConsensusDataFlow() =
        aircraftTypeConsensusDao.consensusDataFlow().map{
            aircraftTypeConsensusDataListToAircraftList(it)
        }

    private fun getPreloadedRegistrationsFlow() =
        preloadedRegistrationsDao.registrationsFlow()

    private fun aircraftTypesDataToAircraftTypes(l: List<AircraftTypeData>) =
        l.map { it.toAircraftType() }

    private fun aircraftRegistrationWithTypeDataListToAircraftRegistrationWithTypeList(l: List<AircraftRegistrationWithTypeData>) =
        l.map { it.toAircraftRegistrationWithType() }

    private fun aircraftTypeConsensusDataListToAircraftList(l: List<AircraftTypeConsensusData>) =
        l.map { it.toAircraftTypeConsensus().toAircraft() }

    /*
     * Closing this object will stop it from updating, but it can still be read.
     */
    override fun close() {
        collectionJobs.forEach{
            it.cancel(null)
        }
    }
}