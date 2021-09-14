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

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.helpers.FlowingAirportSearcher
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.AirportDao
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.reversed
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.lang.Exception
import java.util.*

class AirportRepository(private val airportDao: AirportDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope()  {

    //Mutex lock to make sure forced and scheduled workers don't interfere with each other
    private val lockedForWorker = Mutex()

    private val _live = airportDao.requestLiveAirports()

    /**
     * All airports, cached and held in a LiveData
     */
    private val _cachedAirports = MediatorLiveData<List<Airport>>().apply{
        launch{ value=getAll(true) } // this forces _cachedAirports to load data even when not observed
        addSource(_live){
            value = it
            launch {
                _icaoIataMap.value = getIcaoToIataMap(true)
            }
        }
    }


    /**
     * ICAO <-> IATA map, cached and held in a LiveData
     */
    private val _icaoIataMap = MediatorLiveData<Map<String, String>>().apply{
        launch{ value = getIcaoToIataMap(true) }

        addSource(_live) {
            value = getIcaoToIataMap(it)
        }
    }


    /**
     * 'use IATA' preference
     * TODO this should be done in model layer, not here
     */
    private val _useIataAirports = MutableLiveData(Preferences.useIataAirports)


    /**
     * Get all Airports from cache or from disk
     */
    private suspend fun getAll(forceReload: Boolean = false): List<Airport> = withContext(dispatcher) {
        if (forceReload) airportDao.requestAllAirports()
        else _cachedAirports.value ?: airportDao.requestAllAirports()
    }

    /**
     * Update on changed SharedPrefs
     */
    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Preferences::useIataAirports.name) _useIataAirports.value = Preferences.useIataAirports
    }

    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }



    /*******************************************************************************************************************
     *  EXPOSED PARTS
     *******************************************************************************************************************/

    /**
     * Exposed ICAO - ICTA map
     */
    val icaoIataMap: LiveData<Map<String, String>>
        get() = _icaoIataMap

    /**
     * Exposed live Airports
     */
    val liveAirports: LiveData<List<Airport>>
        get() = _cachedAirports

    /**
     * Exposed 'use IATA' preference
     * TODO this should be done in model layer, not here
     */
    val useIataAirports: LiveData<Boolean>
        get() = _useIataAirports

    /**
     * Save airports to disk. Does not replace entire DB but adds to it.
     */
    fun save(airports: List<Airport>) = launch(dispatcher + NonCancellable) {
        airportDao.insertAirports(*airports.toTypedArray())
    }

    /**
     * Clear Airport DB
     */
    fun clearDB() = launch(dispatcher + NonCancellable) {
            airportDao.clearDb()
    }

    /**
     * Save airports to disk. This does replace entire DB
     */
    fun replaceDbWith(airports: List<Airport>){
        launch(dispatcher + NonCancellable) {
            //Clear DB and wait for that to  finish
            clearDB().join()
            save(airports)
        }
    }

    /**********************************************************************************************
     * Airport Search Functions
     **********************************************************************************************/


    /**
     * Returns one airport, or null if none found
     * Searches in order: ICA Ident - Iata - Municipality - Airport Name
     * eg. EHAM - AMS - Amsterdam - Schiphol
     */
    suspend fun searchAirportOnce(query: String?): Airport? = withContext(dispatcher) {
        query?.nullIfEmpty()?.let { query ->
            airportDao.searchAirportByIdent(query).firstOrNull()
                ?: airportDao.searchAirportByIata(query).firstOrNull()
                ?: airportDao.searchAirportByMunicipality(query).firstOrNull()
                ?: airportDao.searchAirportByName(query).firstOrNull()
        }
    }

    /**
     * Gets an airport from database by ICAO identifier
     * @return [Airport] found, or null if not found or query was null
     */
    suspend fun getAirportByIcaoIdentOrNull(query: String?):Airport? = withContext(dispatcher){
        when {
            query == null -> null
            query.isBlank() -> null
            _cachedAirports.value == null -> {
                searchAirportOnce(query).let{foundAP ->
                    if (foundAP?.ident?.uppercase(Locale.ROOT) == query.uppercase(Locale.ROOT)) foundAP else null
                }
            }
            else -> _cachedAirports.value!!.firstOrNull {
                it.ident.uppercase(Locale.ROOT) == query.uppercase(
                    Locale.ROOT
                )
            }
                ?: _cachedAirports.value!!.firstOrNull {
                    it.iata_code.uppercase(Locale.ROOT) == query.uppercase(
                        Locale.ROOT
                    )
                }
        }
    }

    /**
     * @see getAirportByIcaoIdentOrNull but then async
     */
    fun getAirportByIcaoIdentOrNullAsync(query: String?) = async { getAirportByIcaoIdentOrNull(query) }

    suspend fun searchAirportsOnce(query: String): List<Airport> = withContext(dispatcher) {
        airportDao.searchAirports("%${query.uppercase(Locale.ROOT)}%")
    }

    suspend fun getQueryFlow(query: String): Flow<List<Airport>> = FlowingAirportSearcher.makeFlow(getAll(), query)

    suspend fun getIcaoToIataMap(forceReload: Boolean = false): Map<String, String> =
        if (forceReload)
            withContext (dispatcher) { getAll().map { a -> a.ident.uppercase(Locale.ROOT) to a.iata_code.uppercase(Locale.ROOT) }.toMap() }
        else {
            _icaoIataMap.value ?: withContext (dispatcher){ getAll().map { a -> a.ident.uppercase(Locale.ROOT) to a.iata_code.uppercase(Locale.ROOT) }.toMap() }
    }

    /**
     * Non-suspend version that requires a list of airports to be provided
     */
    fun getIcaoToIataMap(airports: List<Airport>): Map<String, String> =
            airports.map { a -> a.ident.uppercase(Locale.ROOT) to a.iata_code.uppercase(Locale.ROOT) }.toMap()

    fun getIcaoIataMapAsync() = async(Dispatchers.IO) { getIcaoToIataMap() }
    fun getIataIcaoMapAsync() = async(Dispatchers.IO) { getIcaoToIataMap().reversed() }


    /**
     * customAirports are those airports that are used in logbook but not in airport Database
     * eg. Wickenburg (E25)
     */

    //TODO make this

    /********************************************************************************************
     * Sync functions:
     ********************************************************************************************/

    /*
    //This is now checked daily as scheduled work from [App]
    fun getAirportsIfNeeded(){
        if (TimestampMaker.nowForSycPurposes - Preferences.airportUpdateTimestamp > MINIMUM_AIRPORT_CHECK_DELAY){
            JoozdlogWorkersHub.periodicGetAirportsFromServer(Preferences.updateLargerFilesOverWifiOnly)
        }
    }

    */

    /**
     * Observable for worker to send progress to
     * progress is 0-100, to signal worker has stopped]
     */
    private val _airportSyncProgress = MutableLiveData<Int>(-1)
    val airportSyncProgress: LiveData<Int>
        get() = _airportSyncProgress
    fun setAirportSyncProgress(progress: Int){
        require (progress in (-1..100)) {"Progress reported to setAirportSyncProgress not in range -1..100"}
        launch (Dispatchers.Main) {
            _airportSyncProgress.value = progress
        }
    }

    /**
     * Locks [lockedForWorker]
     */
    suspend fun acquireLock(){
        lockedForWorker.lock()
    }

    /**
     * Releases [lockedForWorker]
     */
    fun releaseLock(){
        try{
            lockedForWorker.unlock()
        } catch (e: Exception){
            Log.w("AirportRepo", "Requested to unlock an already unlocked lockedForWorker\n${e.stackTraceToString()}")
        }
    }


    companion object{
        private var singletonInstance: AirportRepository? = null
        fun getInstance(): AirportRepository = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val airportDao = dataBase.airportDao()
                    singletonInstance = AirportRepository(airportDao)
                    singletonInstance!!
                }
        }
        const val MINIMUM_AIRPORT_CHECK_DELAY: Int = 0 * 60 // seconds
    }
}