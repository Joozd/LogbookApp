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
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.lang.Exception
import java.util.*

class AirportRepository(private val airportDao: AirportDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope()  {

    //Mutex lock to make sure forced and scheduled workers don't interfere with each other
    private val lockedForWorker = Mutex()

    private val _cachedAirports = MediatorLiveData<List<Airport>>()
    init{
        launch{
            _cachedAirports.value=getAll(true)
        }
        _cachedAirports.addSource(getLive()){
            _cachedAirports.value = it
            launch {
                _icaoIataMap.value = getIcaoToIataMap(true)
            }
        }
    }
    val liveAirports: LiveData<List<Airport>> =
        Transformations.distinctUntilChanged(_cachedAirports)

    private val _icaoIataMap = MediatorLiveData<Map<String, String>>()
    init{
        launch(Dispatchers.Main) { _icaoIataMap.value = getIcaoToIataMap(true) }
        Log.d("XOXOXOXOXOXOXOX", "icaoIataMap is now size ${icaoIataMap.value?.size}")
        _icaoIataMap.addSource(liveAirports) {
            launch(Dispatchers.Main) {_icaoIataMap.value = getIcaoToIataMap() }
        }
    }
    val icaoIataMap: LiveData<Map<String, String>>
        get() = _icaoIataMap

    private val _useIataAirports = MutableLiveData<Boolean>()
    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Log.d("AirportRepository", "key = $key")
        if (key == Preferences::useIataAirports.name) _useIataAirports.value = Preferences.useIataAirports
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }
    val useIataAirports: LiveData<Boolean>
        get() = _useIataAirports

    private suspend fun getAll(forceReload: Boolean = false): List<Airport> = withContext(dispatcher) {
        if (forceReload) airportDao.requestAllAirports()
        else _cachedAirports.value ?: airportDao.requestAllAirports()
    }

    suspend fun getIdents() = withContext(dispatcher) {
        airportDao.requestAllIdents()
    }

    private fun getLive() =
        airportDao.requestLiveAirports()

    fun save(airports: List<Airport>) = launch(dispatcher + NonCancellable) {
        airportDao.insertAirports(*airports.toTypedArray())
    }

    fun clearDB() = launch(dispatcher + NonCancellable) {
            airportDao.clearDb()
    }

    fun replaceDbWith(airports: List<Airport>){
        launch(dispatcher + NonCancellable) {
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
     */
    suspend fun getAirportByIcaoIdentOrNull(query: String?):Airport? = withContext(dispatcher){
        when {
            query == null -> null
            query.isBlank() -> null
            _cachedAirports.value == null -> {
                searchAirportOnce(query).let{foundAP ->
                    if (foundAP?.ident?.toUpperCase(Locale.ROOT) == query.toUpperCase(Locale.ROOT)) foundAP else null
                }
            }
            else -> _cachedAirports.value!!.firstOrNull {
                it.ident.toUpperCase(Locale.ROOT) == query.toUpperCase(
                    Locale.ROOT
                )
            }
                ?: _cachedAirports.value!!.firstOrNull {
                    it.iata_code.toUpperCase(Locale.ROOT) == query.toUpperCase(
                        Locale.ROOT
                    )
                }
        }
    }

    suspend fun searchAirportsOnce(query: String): List<Airport> = withContext(dispatcher) {
        airportDao.searchAirports("%${query.toUpperCase(Locale.ROOT)}%")
    }

    suspend fun getQueryFlow(query: String): Flow<List<Airport>> = FlowingAirportSearcher.makeFlow(getAll(), query)

    suspend fun getIcaoToIataMap(forceReload: Boolean = false): Map<String, String> =
        if (forceReload)
            withContext (dispatcher) { getAll().map { a -> a.ident.toUpperCase(Locale.ROOT) to a.iata_code.toUpperCase(Locale.ROOT) }.toMap() }
        else {
            _icaoIataMap.value ?: withContext (dispatcher){ getAll().map { a -> a.ident.toUpperCase(Locale.ROOT) to a.iata_code.toUpperCase(Locale.ROOT) }.toMap() }
    }

    fun getIcaoIataMapAsync() = async { getIcaoToIataMap()}


    /**
     * customAirports are those airports that are used in logbook but not in airport Database
     * eg. Wickenburg (E25)
     */

    //TODO make this

    /********************************************************************************************
     * Sync functions:
     ********************************************************************************************/

    fun getAirportsIfNeeded(){
        if (TimestampMaker.nowForSycPurposes - Preferences.airportUpdateTimestamp > MINIMUM_AIRPORT_CHECK_DELAY){
            JoozdlogWorkersHub.periodicGetAirportsFromServer(Preferences.updateLargerFilesOverWifiOnly)
        }
    }

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

    suspend fun acquireLock(){
        lockedForWorker.lock()
    }

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