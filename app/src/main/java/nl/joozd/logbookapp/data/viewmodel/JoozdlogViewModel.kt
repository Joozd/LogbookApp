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

package nl.joozd.logbookapp.data.viewmodel

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.ui.App
import nl.joozd.logbookapp.utils.mostRecentCompleteFlight
import nl.joozd.logbookapp.utils.reverseFlight


/**
 * JoozdlogViewModel will hold data pertaining to UI components
 * ie. a workingFlight that is used to set stuff in dialogs, and to be saved (or not) after editing it
 */


class JoozdlogViewModel: ViewModel() {

    private val repository: Repository = Repository.getInstance()

    // This will be filled with a flight that is being worked on, as well as its undo-version
    // Null / empty untill first used.
    val workingFlight: MutableLiveData<Flight> = MutableLiveData<Flight>()
    var undoFlight: Flight? = null

    //This is to be used for updating fields in UI
    val distinctWorkingFlight = distinctUntilChanged(workingFlight)

    //shorter term undoFlight to be able to cancel subdialogs such as TimePicker or NamePicker
    var unchangedFlightForUseInDialogs: Flight? = null

    //if true, NamePicker is working on name1, if false it is working on name2. If null, it is not set.
    var namePickerWorkingOnName1: Boolean? = null


    /**
     * Some boilerplate here to save some boilerplate in MainActivity
     * Use this function to pick a flight to edit.
     * It will set [workingFlight] to this flight, or if null, reverse of last complete flight, or if none, empty flight
     * It will then also set:
     * - [undoFlight] to this flight, so editing can be undone.
     * - [namePickerWorkingOnName1] to null
     */
    suspend fun setWorkingFlight(flight: Flight?){
        Log.d("setWorkingFlight", "started")

        Log.d("setWorkingFlight", "0")
        workingFlight.value = flight ?: reverseFlight(mostRecentCompleteFlight(repository.requestValidFlights()), 1 /*highestFlightId ?: getHighestFlightIdAsync().await() */)
        Log.d("setWorkingFlight", "1")
        undoFlight = flight
        Log.d("setWorkingFlight", "2")
        namePickerWorkingOnName1 = null
        Log.d("setWorkingFlight", "3")
    }

    /**
     * highestFlightIdea is the highest used flightID in local storage.
     */
    private fun getHighestFlightIdAsync() = viewModelScope.async {
        Log.d("getHighestFlightIdAsync", "started")
        withContext(Dispatchers.IO) { repository.highestFlightId() ?: 0 } }.also { Log.d("getHighestFlightIdAsync", "completed with $it") }

    //Deferred<Int>

    var highestFlightId: Int? = null
    init{
        viewModelScope.launch {
            highestFlightId = getHighestFlightIdAsync().await()
            repository.liveFlights.observeForever { launch(Dispatchers.Main) { highestFlightId = getHighestFlightIdAsync().await() } }
        }
    }

    /**
     * customAirports are those airports that are used in logbook but not in airport Database
     * eg. Wickenburg (E25)
     */
    private fun getCustomAirportsAsync(flights: List<Flight>? = null) = viewModelScope.async(Dispatchers.IO) {
        val fff = flights ?: repository.requestValidFlights()
        ((fff.map { it.orig } + fff.map { it.dest })
            .distinct()
            .filter { it !in (repository.requestAllIdents()) })
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
        viewModelScope.launch{
            liveCustomAirports.value = customAirports.await()
        }
        repository.liveFlights.observeForever {
            viewModelScope.launch { liveCustomAirports.value = getCustomAirportsAsync(it).await() }
        }
    }



    /**
     * allNames is a list of all names used in logbook
     * It will update when flights are updated in Repository
     */
    private fun buildNameListAsync(flights: List<Flight>? = null) = viewModelScope.async {
        val allFlights = flights ?: repository.requestValidFlights()
        val foundNames: MutableList<String> = mutableListOf()

        allFlights.forEach { flight ->
            flight.allNames.split(",").map { it.trim() }.forEach { name ->
                if (name !in foundNames) foundNames.add(name)
            }
        }
        foundNames
    }
    //Deferred<List<String>>
    var allNamesDeferred = buildNameListAsync()
    var allNames = listOf("SELF")
    init {

        viewModelScope.launch {
            allNames = allNamesDeferred.await()
        }
        repository.liveFlights.observeForever {
            viewModelScope.launch {
                allNamesDeferred = buildNameListAsync(it)
                allNames = allNamesDeferred.await()
            }
        }


    }

    /**
     * icaoToIataMap is a map of ICAO to IATA identifiers
     */

    private fun getIcaoToIataMapAsync(airports: List<Airport>? = null) = viewModelScope.async {
        repository.getIcaoToIataMap()
    }
    var icaoToIataMap = emptyMap<String, String>()

    init{
        viewModelScope.launch {
            icaoToIataMap = getIcaoToIataMapAsync().await()
        }
        repository.requestLiveAirports().observeForever {
            icaoToIataMap = it.map{a -> a.ident to a.iata_code}.toMap()
        }
    }












    // Get this from repository
    // val liveFlights: LiveData<List<Flight>> = repository.liveFlights


}