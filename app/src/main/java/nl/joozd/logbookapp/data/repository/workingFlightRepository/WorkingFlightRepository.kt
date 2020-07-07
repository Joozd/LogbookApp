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

package nl.joozd.logbookapp.data.repository.workingFlightRepository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.prepareForSave
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvent
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.FlightEditorOpenOrClosed
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTakeoffLandings
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reverseFlight

class WorkingFlightRepository(private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope() {

    /********************************************************************************************
     * Private parts
     ********************************************************************************************/

    private val flightRepository = FlightRepository.getInstance()
    private val airportRepository =AirportRepository.getInstance()

    private val origDestAircraftWorker = OrigDestAircraftWorker()

    private val _isOpenInEditor = MutableLiveData<Boolean>()
    private val _openInEditorEventTrigger
        get() = distinctUntilChanged(_isOpenInEditor)
    private val _flightIsOpen = MutableLiveData<FeedbackEvent>()

    private var saving: Boolean = false

    /**
     * _workingFlight with it's sources:
     */
    private val _workingFlight = MediatorLiveData<Flight>()
    init{
        _workingFlight.addSource(origin) {
            if (it?.ident != flight?.orig)
                _workingFlight.value = flight?.autoValues()
        }

        _workingFlight.addSource(destination) {
            if (it?.ident != flight?.dest)
                _workingFlight.value = flight?.autoValues()
        }

        /**
         * Updates IFR time if aircraft type changed
         * IF new type is IFR, it sets ifrTime to 0 and calls autovalues
         * if new type is VRF it sets ifrTime to -1
         * if new type is unknown, does nothing
         * Checks if changed aircraft type is always flown as IFR or VFR, if not the same it will
         * recalculate IFR times and update _workingFlight
         */
        _workingFlight.addSource(aircraft) {
            val workingFlightSnapshot = flight
            var ifrTime: Int? = null
            var needsAutoTimes = false

            if (it?.type?.shortName != flight?.aircraftType && it?.type != null) {
                launch {
                    do {
                        when (thisAircraftIsIFR(it.type.shortName)) {
                            true -> flight?.let { f ->
                                if (f.ifrTime < 0)
                                    ifrTime = 0
                                needsAutoTimes = true
                            } // else there is no change
                            false -> {
                                ifrTime = -1
                                needsAutoTimes = false
                            } // -1 IFR Time means VFR
                            null -> ifrTime = null // do nothing
                        }
                    } while (workingFlightSnapshot != flight) // if flight changed while doing this, do it again
                    flight?.let{f ->
                        ifrTime?.let {time ->
                            _workingFlight.value = if (needsAutoTimes) f.copy(ifrTime = time).autoValues() else f.copy(ifrTime = time)
                        }
                    }
                }
            }
        }
    }


    private var backupFlight: Flight? = null



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

    /*************************
     * Auto fill logic:
     *************************/

    /**
     * Applies all automatically calculated values to a flight
     */
    private fun Flight.autoValues(): Flight = if (!autoFill) this else {
        this
            .withTakeoffLandings(if (isPF)1 else 0, origin.value, destination.value)
            .updateNightTime(_workingFlight.value)
            .updateIFRTime()
        //TODO calculate IFR time if needed
    }

    /**
     * this will return a new flight with updated nighttime, corrected for augmented crew (same ratio)
     */

    private fun Flight.updateNightTime(old: Flight?): Flight{
        return if ((old?.orig != orig || old.dest != dest || old.timeIn != timeIn || old.timeOut != timeOut) && autoFill) {
            val twilightCalculator = TwilightCalculator(timeOut)
            val totalNightTime = twilightCalculator.minutesOfNight(this@WorkingFlightRepository.origin.value,this@WorkingFlightRepository.destination.value, timeOut, timeIn)
            val correctedNightTime = crew?.getLogTime(totalNightTime, isPIC) ?: totalNightTime
            this.copy(nightTime = correctedNightTime)
        } else this
    }

    private fun Flight.updateIFRTime(): Flight {
        val ifrTimeToSet: Int = if (ifrTime == -1) -1 else duration()
        return copy(ifrTime = ifrTimeToSet)
    }

    /**
     * Checks if this aircraft always or never flies IFR
     * @return: true if always IFR
     *          false if never IFR
     *          null if unknown of not consistent
     */
    private suspend fun thisAircraftIsIFR(reg: String): Boolean?{
        val previousFlights = flightRepository.getAllFlights().filter{it.registration == reg}
        return when{
            previousFlights.isEmpty() -> null
            previousFlights.all{it.ifrTime >= 0} -> true
            previousFlights.all{it.ifrTime < 0} -> false
            else -> null
        }
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
        get() = Transformations.map (origDestAircraftWorker.origAirport) {
            (it != null).also{go ->
                if (go && saving)
                    saveWorkingFlight(flight?.autoValues())
            }
        }

    val destInDatabase: LiveData<Boolean>
        get() = Transformations.map (origDestAircraftWorker.destAirport) {
            (it != null).also{go ->
                if (go && saving)
                    saveWorkingFlight(flight?.autoValues())
            }
        }

    val origin: LiveData<Airport>
        get() = origDestAircraftWorker.origAirport

    val destination: LiveData<Airport>
        get() = Transformations.map(origDestAircraftWorker.destAirport){
            it.also{
                if (it != null && saving)
                    saveWorkingFlight(flight?.autoValues())
            }
        }

    val aircraft: LiveData<Aircraft>
        get() = origDestAircraftWorker.aircraft

    var crew: Crew?
    get() = flight?.let { Crew.of(it.augmentedCrew) }
    set(crew){
        if (crew != null && flight != null)
            updateWorkingFlight(flight!!.copy(augmentedCrew = crew.toInt()))
    }



    /*********************************************************************************************
     * Functions for creating, updating and saving
     *********************************************************************************************/

    /**
     * Updates working flight.
     */
    fun updateWorkingFlight(flight: Flight) {
        origDestAircraftWorker.flight = flight
        _workingFlight.value = flight.autoValues()
        if (saving) saveWorkingFlight()
    }

    /**
     * Saves working flight. Flight to save can be overridden but only private
     */
    fun saveWorkingFlight() =
        _workingFlight.value?.let {
            saving = true
            flightRepository.save(it.prepareForSave())
        }
    private fun saveWorkingFlight(override: Flight?) =
        override ?: _workingFlight.value?.let {
            saving = true
            flightRepository.save(it.prepareForSave())
        }

    fun undoSaveWorkingFlight() = backupFlight?.let {
        saving = false
        flightRepository.save(it) }
        ?: workingFlight.value?.let { flightRepository.delete(it) } // if backupFlight is not set, undo means deleting new flight

    /**
     * Creates an empty flight which is the reverse of the most recent completed flight
     */
    suspend fun createNewWorkingFlight() {
        coroutineScope {
            val highestID = flightRepository.getHighestIdAsync()
            val mostRecentFlight = flightRepository.getMostRecentFlightAsync()
            saving = false
            val done = async(Dispatchers.Main) {
                updateWorkingFlight(reverseFlight(mostRecentFlight.await().also{Log.d("MostRecentFlight", "$it")} ?: Flight.createEmpty(), highestID.await() + 1))
            }
            backupFlight = null
            done.await()
        }
    }

    /**
     * Fetch a flight from FlightRepository and set it as working flight. Returns that flight if successful
     */
    suspend fun fetchFlightByIdToWorkingFlight(id: Int): Flight?{
        val workingFlight = withContext(dispatcher) {
            flightRepository.fetchFlightByID(id)
        } ?: return null
        return workingFlight.also{
            withContext(Dispatchers.Main) { initialSetWorkingFlight(it) }
        }
    }

    /**
     * Updates working flight with same aircraft, name, name2 and isPic as last completed flight
     */
    fun updateWorkingFlightWithMostRecentData(){
        launch(dispatcher) {
            flightRepository.getMostRecentFlightAsync().await()?.let { f ->
                _workingFlight.value?.let {
                    val aircraft = it.aircraftType.nullIfEmpty() ?: f.aircraftType
                    val registration = it.registration.nullIfEmpty() ?: f.registration
                    val name = it.name.nullIfEmpty() ?: f.name
                    val name2 = it.name2.nullIfEmpty() ?: f.name2
                    val isPIC = f.isPIC
                    launch(Dispatchers.Main) {
                        _workingFlight.value = it.copy(aircraftType = aircraft, registration = registration, name = name, name2 = name2, isPIC = isPIC)
                    }
                }
            }
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
                    singletonInstance =
                        WorkingFlightRepository()
                    singletonInstance!!
                }
        }
    }

}