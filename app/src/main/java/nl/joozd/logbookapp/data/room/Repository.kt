/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.data.room


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.utils.aircraftdbbuilder.AircraftType
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.room.dao.*
import nl.joozd.logbookapp.data.room.model.toAircraftType
import nl.joozd.logbookapp.data.room.model.toFlight
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.ui.App
import java.time.Instant


/**
 * This is the handle for accessing the Database
 * It will take care of caching as well as saving/loading to/from DB
 * Initialise:
 * @param flightDao = Dao to access flightsDatabase
 * @param dispatcher = dispatcher for coroutines, standard uses Dispatchers.IO
 *
 * Get a singleton instance with Repository.getInstance()
 */
class Repository(
    private val flightDao: FlightDao,
    private val airportDao: AirportDao,
    private val aircraftTypeDao: AircraftTypeDao,
    private val registrationDao: RegistrationDao,
    private val aircraftTypeConsensusDao: AircraftTypeConsensusDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {

    /*********************************************************************************************
     * Flight DB functions
     ********************************************************************************************/

    // cached data
    private val _cachedFlights = MutableLiveData<List<Flight>>()
    init {
        // May be some delay in filling stuff listening to this
        launch {
            _cachedFlights.value = flightDao.requestValidFlights().map {it.toFlight() }
        }
        requestValidLiveFlightData().observeForever { _cachedFlights.value = it }
    }

    private var _cachedAirports = MutableLiveData<List<Airport>>()
    init{
        launch{
            _cachedAirports.value=requestAllAirports(true)
        }
        requestLiveAirports().observeForever {
            _cachedAirports.value = it
        }
    }
    private var _cachedAircraftTypes: List<AircraftType>? = null
    init{
        launch{
            _cachedAircraftTypes = getAllAircraftTypesAsync().await()
        }
    }
    val liveFlights: LiveData<List<Flight>> = distinctUntilChanged(_cachedFlights)
    val liveAirports: LiveData<List<Airport>> = distinctUntilChanged(_cachedAirports)


    /**
     * update cached data and save to disk
     */
    suspend fun saveFlights(flights: List<Flight>) = withContext(NonCancellable) {
        //update cached flights
        launch(Dispatchers.Main) {
            _cachedFlights.value = ((_cachedFlights.value
                ?: emptyList()).filter { it.flightID !in flights.map { it.flightID } } + flights.filter { it.DELETEFLAG == 0 }).sortedByDescending { it.timeOut }
        }

        //Save flights to disk
        flightDao.insertFlights(*(flights.map { it.toModel() }.toTypedArray()))
    }

    /**
     * update cached data and save to disk
     */
    suspend fun saveFlight(flight: Flight) = withContext(NonCancellable) {
        //update cached flights
        launch(Dispatchers.Main) {
            _cachedFlights.value = ((_cachedFlights.value
                ?: emptyList()).filter { it.flightID != flight.flightID } + listOf(flight).filter { it.DELETEFLAG == 0 }).sortedByDescending { it.timeOut }
        }
        //Save flight to disk
        flightDao.insertFlights(flight.toModel())
    }

    /**
     * update cached data and delete from disk
     */
    suspend fun deleteFlightHard(flight: Flight) = withContext(NonCancellable) {
        launch(Dispatchers.Main) {
            _cachedFlights.value = ((_cachedFlights.value
                ?: emptyList()).filter { it.flightID != flight.flightID }).sortedByDescending { it.timeOut }
        }
        flightDao.delete(flight.toModel())
    }

    /**
     * update cached data and delete multiple flights from disk
     */
    suspend fun deleteFlightsHard(flights: List<Flight>) = withContext(NonCancellable) {
        launch(Dispatchers.Main) {
            _cachedFlights.value = ((_cachedFlights.value
                ?: emptyList()).filter { it.flightID !in flights.map { f -> f.flightID } }).sortedByDescending { it.timeOut }
        }
        flightDao.deleteMultipleByID(flights.map { it.flightID })
    }


    /**
     * Delete flight from disk if not known to server, else set DELETEFLAG to 1 and update timestamp
     */
    suspend fun deleteFlight(flight: Flight) {
        if (flight.changed > 0) deleteFlightHard(flight)
        else saveFlight(
            flight.copy(
                DELETEFLAG = 1,
                timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset
            )
        )
    }

    /**
     * Same as deleteFlight, but with a list of multiple Flights
     */
    suspend fun deleteFlights(flights: List<Flight>) {
        deleteFlightsHard(flights.filter { it.changed > 0 })
        saveFlights(flights.filter { it.changed == 0 }.map { f ->
            f.copy(
                DELETEFLAG = 1,
                timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset
            )
        })
    }

    suspend fun requestAllFlights() = withContext(dispatcher) {
        flightDao.requestAllFlights().map {it.toFlight() }
    }

    /**
     * Will return cached flights if available, otherwise will get it from disk (ie. if too soon after initialization)
     */
    suspend fun requestValidFlights(): List<Flight> = withContext(dispatcher) {
        Log.d("requestValidFlights", "started")
        _cachedFlights.value ?: flightDao.requestValidFlights().map {it.toFlight() }.also{ Log.d("requestValidFlights", "requesting fromDao")}
    }.also{ Log.d("requestValidFlights", "Done") }

    suspend fun highestFlightId(): Int? = withContext(dispatcher) {
        Log.d("highestFlightId", "started")
        flightDao.highestId().also{Log.d("from Dao:", "$it")}
    }

    /**
     * In case you want live data from all flights including deleted
     */

    fun requestLiveFlightData() =
        Transformations.map(flightDao.requestLiveData()) { fff ->
            fff.map { f -> f.toFlight() }
        }

    fun requestValidLiveFlightData() =
        Transformations.map(flightDao.requestNonDeletedLiveData()) { fff ->
            fff.map { f -> f.toFlight() }
        }

    suspend fun clearFlightDB() = withContext(dispatcher) {
        launch(Dispatchers.Main) { _cachedFlights.value = emptyList() }
        flightDao.clearDb()
    }

    /*********************************************************************************************
     * Airport DB Functions
     ********************************************************************************************/
    suspend fun requestAllAirports(forceReload: Boolean = false): List<Airport> = withContext(dispatcher) {
        if (forceReload) airportDao.requestAllAirports()
        else _cachedAirports.value ?: airportDao.requestAllAirports()
    }

    suspend fun requestAllIdents() = withContext(dispatcher) {
        airportDao.requestAllIdents()
    }

    fun requestLiveAirports() =
        airportDao.requestLiveAirports()

    suspend fun saveAirports(airports: List<Airport>) = withContext(NonCancellable) {
        airportDao.insertAirports(*airports.toTypedArray())
    }

    suspend fun clearAirportsDB() = withContext(dispatcher) {
        launch {
            airportDao.clearDb()
        }
    }

    /**
     * Returns one airport, or null if none found
     * Searches in order: ICA Ident - Iata - Municipality - Airport Name
     * eg. EHAM - AMS - Amsterdam - Schiphol
     */
    suspend fun searchAirport(query: String): Airport? = withContext(dispatcher) {
        airportDao.searchAirportByIdent(query).firstOrNull()
            ?: airportDao.searchAirportByIata(query).firstOrNull()
            ?: airportDao.searchAirportByMunicipality(query).firstOrNull()
            ?: airportDao.searchAirportByName(query).firstOrNull()
    }

    suspend fun searchAirports(query: String): List<Airport> = withContext(dispatcher) {
        airportDao.searchAirports("%${query.toUpperCase()}%")
    }

    suspend fun getIcaoToIataMap(): Map<String, String> =
        requestAllAirports().map { a -> a.ident to a.iata_code }.toMap()

    /**
     * customAirports are those airports that are used in logbook but not in airport Database
     * eg. Wickenburg (E25)
     */
    private fun getCustomAirportsAsync(flights: List<Flight>? = null) = async(Dispatchers.IO) {
        val fff = flights ?: requestValidFlights()
        ((fff.map { it.orig } + fff.map { it.dest })
            .distinct()
            .filter { it !in (requestAllIdents()) })
            .map { name ->
                Airport(
                    ident = name,
                    name = "User airport"
                )
            }
    }
    //Deferred<List<Airport>>
    var customAirports = getCustomAirportsAsync()
    val liveCustomAirports = MutableLiveData<List<Airport>>()
    val distinctLiveCustomAirports = distinctUntilChanged(liveCustomAirports)

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

    /********************************************************************************************
     * Aircraft Functions
     ********************************************************************************************/

    fun getAllAircraftTypesAsync() = async(dispatcher) {
        _cachedAircraftTypes ?: aircraftTypeDao.requestAllAircraftTypes().map { it.toAircraftType() }
    }

    suspend fun getAircraftType(name: String): AircraftType? = withContext(dispatcher) {
        aircraftTypeDao.getAircraftType(name)?.toAircraftType()
    }

    fun getAircraftTypeAsync(name: String): Deferred<AircraftType?> = async {
        aircraftTypeDao.getAircraftType(name)?.toAircraftType()
    }

    /********************************************************************************************
     * Aircraft Type Consensus Functions
     ********************************************************************************************/

    //TODO


    /********************************************************************************************
     * Companion Object
     ********************************************************************************************/

    companion object{
        private var singletonInstance: Repository? = null
        fun getInstance(): Repository = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val flightsDao = dataBase.flightDao()
                    val airportDao = dataBase.airportDao()
                    val aircraftTypeDao = dataBase.aircraftTypeDao()
                    val registrationDao = dataBase.registrationDao()
                    val aircraftTypeConsensusDao = dataBase.aircraftTypeConsensusDao()
                    singletonInstance = Repository(flightsDao, airportDao, aircraftTypeDao, registrationDao, aircraftTypeConsensusDao)
                    singletonInstance!!
                }
        }
    }


}