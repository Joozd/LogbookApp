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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.repository.helpers.prepareForSave
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvent
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.FlightEditorOpenOrClosed
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reverseFlight

class WorkingFlightRepository(private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope() {

    /********************************************************************************************
     * Private parts
     ********************************************************************************************/

    private val flightRepository = FlightRepository.getInstance()
    private val airportRepository = AirportRepository.getInstance()

    private val _isOpenInEditor = MutableLiveData<Boolean>()
    private val _openInEditorEventTrigger
        get() = distinctUntilChanged(_isOpenInEditor)
    private val _flightIsOpen = MutableLiveData<FeedbackEvent>()

    private var saving: Boolean = false

    private val _workingFlight = MutableLiveData<Flight>()
    private var backupFlight: Flight? = null

    private val _origInDatabase = MutableLiveData<Boolean>()
    private val _destInDatabase = MutableLiveData<Boolean>()
    private val _dest = MutableLiveData<Airport>()


    init{
        _openInEditorEventTrigger.observeForever {
            _flightIsOpen.value =
                FeedbackEvent(if (it) FlightEditorOpenOrClosed.OPENED else FlightEditorOpenOrClosed.CLOSED)
        }
    }

    private fun initialSetWorkingFlight(flight: Flight) {
        saving = false
        updateWorkingFlight(flight)
        backupFlight = flight
    }

    /**
     * If needed, this will return a new flight with updated nighttime, if not it will return null
     */
    private fun Flight.updateNightTimeIfNeeded(old: Flight?): Flight?{
        return if ((old?.orig != orig || old.dest != dest || old.timeIn != timeIn || old.timeOut != timeOut) && autoFill) {
            val twilightCalculator = TwilightCalculator(timeOut)
            this.copy(nightTime = twilightCalculator.minutesOfNight(this@WorkingFlightRepository.orig.value,this@WorkingFlightRepository.dest.value, timeOut, timeIn))
        } else null
    }



    /********************************************************************************************
     * Working flight:
     ********************************************************************************************/
    //workingFlight is the flight that is being edited
    //set this before making fragment

    val workingFlight: LiveData<Flight>
        get() = _workingFlight

    private val flight: Flight?
        get() = workingFlight.value


    val origInDatabase: LiveData<Boolean>
        get() = distinctUntilChanged(_origInDatabase)


    val destInDatabase: LiveData<Boolean>
        get() = distinctUntilChanged(_destInDatabase)

    private val _orig = MutableLiveData<Airport>()
    val orig: LiveData<Airport>
        get() = distinctUntilChanged(_orig)


    val dest: LiveData<Airport>
        get() = distinctUntilChanged(_dest)

    var crew: Crew?
    get() = flight?.let { Crew.of(it.augmentedCrew) }
    set(crew){
        if (crew != null && flight != null)
            updateWorkingFlight(flight!!.copy(augmentedCrew = crew.toInt()))
    }

    /*********************************************************************************************
     * Functions for creating, updating and saving
     *********************************************************************************************/

    fun updateWorkingFlight(flight: Flight) {
        runBlocking {
            val oldFlight = _workingFlight.value
            _workingFlight.value = flight
            if (oldFlight?.orig != flight.orig)
            _origInDatabase.value = airportRepository.getAirportOnce(flight.orig)?.let {
                _orig.value = it
                true
            } ?: false
            if (oldFlight?.dest != flight.dest)
            _destInDatabase.value = airportRepository.getAirportOnce(flight.dest)?.let {
                _dest.value = it
                true
            } ?: false
            _workingFlight.value?.updateNightTimeIfNeeded(oldFlight)?.let { _workingFlight.value = it } // this will always update the most recently updated flight and oldFlight in case another thread has updated this
            if (saving) saveWorkingFlight()
        }
    }

    fun saveWorkingFlight() = _workingFlight.value?.let {
        saving = true
        flightRepository.save(it.prepareForSave()) }
    fun undoSaveWorkingFlight() = backupFlight?.let {
        saving = false
        flightRepository.save(it) }
        ?: workingFlight.value?.let { flightRepository.delete(it) } // if backupFlight is not set, undo means deleting new flight

    /**
     * Creates an empty flight which is the reverse of the most recent completed flight
     */
    suspend fun createNewWorkingFlight() {
        coroutineScope {
            val highestID = flightRepository.getHighestId()
            val mostRecentFlight = flightRepository.getMostRecentFlight()
            val done = async(Dispatchers.Main) {
                _workingFlight.value =
                    reverseFlight(mostRecentFlight.await() ?: Flight.createEmpty(), highestID.await() + 1)
            }
            backupFlight = null
            done.await()
        }
    }

    suspend fun fetchFlightByIdToWorkingFlight(id: Int): Flight?{
        val workingFlight = withContext(dispatcher) {
            flightRepository.fetchFlightByID(id)
        } ?: return null
        return workingFlight.also{
            withContext(Dispatchers.Main) { initialSetWorkingFlight(it) }
        }
    }

    /*********************************************************************************************
     * Observable status variables
     *********************************************************************************************/

    val flightOpenEvents: LiveData<FeedbackEvent>
        get() = _flightIsOpen

    fun setOpenInEditor(isOpen: Boolean){
        _isOpenInEditor.value = isOpen
    }

    /*********************************************************************************************
     * Companion object
     *********************************************************************************************/


    companion object{
        private var singletonInstance: WorkingFlightRepository? = null
        fun getInstance(): WorkingFlightRepository = synchronized(this) {
            singletonInstance
                ?: run {
                    singletonInstance = WorkingFlightRepository()
                    singletonInstance!!
                }
        }
    }

}