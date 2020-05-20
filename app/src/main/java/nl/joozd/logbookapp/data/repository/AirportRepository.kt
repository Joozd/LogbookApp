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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.helpers.FlowingAirportSearcher
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.AirportDao
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.util.*

class AirportRepository(private val airportDao: AirportDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope()  {
    private val _cachedAirports = MutableLiveData<List<Airport>>()
    init{
        launch{
            _cachedAirports.value=getAll(true)
        }
        getLive().observeForever {
            _cachedAirports.value = it
        }
    }
    val liveAirports: LiveData<List<Airport>> =
        Transformations.distinctUntilChanged(_cachedAirports)

    private val _icaoIataMap = MutableLiveData<Map<String, String>>()
    init{
        launch(Dispatchers.Main) { _icaoIataMap.value = getIcaoToIataMap(true) }
        liveAirports.observeForever {
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
        query?.let { query ->
            airportDao.searchAirportByIdent(query).firstOrNull()
                ?: airportDao.searchAirportByIata(query).firstOrNull()
                ?: airportDao.searchAirportByMunicipality(query).firstOrNull()
                ?: airportDao.searchAirportByName(query).firstOrNull()
        }
    }

    suspend fun getAirportOnce(query: String):Airport? = withContext(dispatcher){
        if (_cachedAirports.value == null) {
            searchAirportOnce(query)
        }
        else _cachedAirports.value!!.firstOrNull{it.ident.toUpperCase(Locale.ROOT) == query.toUpperCase(Locale.ROOT)}
            ?: _cachedAirports.value!!.firstOrNull{it.iata_code.toUpperCase(Locale.ROOT) == query.toUpperCase(Locale.ROOT)}
    }

    suspend fun searchAirportsOnce(query: String): List<Airport> = withContext(dispatcher) {
        airportDao.searchAirports("%${query.toUpperCase(Locale.ROOT)}%")
    }

    suspend fun getQueryFlow(query: String): Flow<List<Airport>> = FlowingAirportSearcher.makeFlow(getAll(), query)

    suspend fun getIcaoToIataMap(forceReload: Boolean = false): Map<String, String> =
        if (forceReload)
            withContext (dispatcher) { getAll().map { a -> a.ident to a.iata_code }.toMap() }
        else {
            _icaoIataMap.value ?: withContext (dispatcher){ getAll().map { a -> a.ident to a.iata_code }.toMap() }
    }


    /**
     * customAirports are those airports that are used in logbook but not in airport Database
     * eg. Wickenburg (E25)
     */
    /* TODO look at this. Decision: Make repositories speak to each other (seems best now) or make separate class

    private fun getCustomAirportsAsync(flights: List<FlightData>) = async(Dispatchers.IO) {
        ((flights.map { it.orig } + flights.map { it.dest })
            .distinct()
            .filter { it !in (getIdents()) })
            .map { name -> Airport(ident = name, name = "User airport")}
    }

    //Deferred<List<Airport>>
    var customAirports = getCustomAirportsAsync()
    val liveCustomAirports = MutableLiveData<List<Airport>>()
    val distinctLiveCustomAirports = Transformations.distinctUntilChanged(liveCustomAirports)

    init{
        launch{
            liveCustomAirports.value = customAirports.await()
        }
        liveFlights.observeForever {
            launch { liveCustomAirports.value = getCustomAirportsAsync(it).await() }
        }
    }

    val completeLiveAirports = MutableLiveData<List<Airport>>()
    init{
        completeLiveAirports.value = (distinctLiveCustomAirports.value ?: emptyList()) + (liveAirports.value?: emptyList())
        distinctLiveCustomAirports.observeForever { completeLiveAirports.value = (it ?: emptyList()) + (liveAirports.value?: emptyList()) }
        liveAirports.observeForever { completeLiveAirports.value = (distinctLiveCustomAirports.value ?: emptyList()) + (it ?: emptyList()) }
    }
    */

    /********************************************************************************************
     * Sync functions:
     ********************************************************************************************/

    fun getAirportsIfNeeded(){
        if (TimestampMaker.nowForSycPurposes - Preferences.airportUpdateTimestamp > MINIMUM_AIRPORT_CHECK_DELAY){
            JoozdlogWorkersHub.getAirportsFromServer(Preferences.updateLargerFilesOverWifiOnly)
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