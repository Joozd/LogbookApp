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

import android.os.Looper
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
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.isSamedPlannedFlightAs
import nl.joozd.logbookapp.data.repository.helpers.prepareForSave
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.FlightEditorOpenOrClosed
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTakeoffLandings
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reverseFlight
import java.time.Instant

class WorkingFlightRepository(private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope() {

    // This does the operations on all flights. used for saving and loading workingFlight
    private val flightRepository = FlightRepository.getInstance()


    /********************************************************************************************
     * Private parts
     ********************************************************************************************/

    /********************************************************************************************
     * Private variables and livedata's
     ********************************************************************************************/


    /**
     * MutableLiveData's that are made public via an immutable getter
     */
    // This holds the actual working flight
    private val _workingFlight = MediatorLiveData<Flight>()

    // true if a flight is open in EditFlightFragment. Fragment takes care of setting via setOpenInEditor()
    private val _isOpenInEditor = MutableLiveData<Boolean>()

    // Triggers for use in places that want to track opening and closing of WorkingFlight
    private val _openInEditorEventTrigger
        get() = distinctUntilChanged(_isOpenInEditor)
    private val _flightIsOpen = MediatorLiveData<FeedbackEvent>()

    /**
     * Internal liveData
     */
    // Helper variable for setting
    private val externallyUpdatedFlight = MutableLiveData<Flight>()

    // Helper class for async parsing of airports and aircraft
    private val origDestAircraftWorker = OrigDestAircraftWorker()

    // true if flight seems to be IFR
    private var thisFlightIsIFR = MutableLiveData<Boolean>()




    // private val _feedbackEvent = MutableLiveData<FeedbackEvent>()

    private var backupFlight: Flight? = null
    private var saving: Boolean = false
    private var savedAndClosed: Boolean = false


    /**
     * _workingFlight it's sources:
     */
    init {
        _workingFlight.addSource(origin) { airport ->
            flight?.let { f ->
                airport?.ident?.let { ident ->
                    if (ident != f.orig)
                        _workingFlight.value = f.copy(orig = ident).autoValues()
                }
            }
        }
        _workingFlight.addSource(destination) { airport ->
            flight?.let { f ->
                airport?.ident?.let { ident ->
                    if (ident != f.dest)
                        _workingFlight.value = f.copy(dest = ident).autoValues()
                }
            }
        }
        _workingFlight.addSource(aircraft) { aircraft ->
            flight?.let { f ->
                if (!f.isSim)
                    aircraft?.type?.let {type ->
                        _workingFlight.value =
                            f.copy(aircraftType = type.shortName).autoValues()
                    }
            }
        }
        _workingFlight.addSource(externallyUpdatedFlight) { newFlight ->
            _workingFlight.value = newFlight.copy(
                // Check if aircraftType remains the same, if so, keep aircraftType from old value to fix concurrency problem
                aircraftType = if (newFlight.registration == _workingFlight.value?.registration) _workingFlight.value?.aircraftType
                    ?: newFlight.aircraftType else newFlight.aircraftType
            ).autoValues().also {
                if (!newFlight.isSim) origDestAircraftWorker.flight = it
            }

            checkIfFlightShouldBeIfr()
        }


        _workingFlight.addSource(thisFlightIsIFR) {
            flight?.let { f ->
                if (!f.isSim)
                    _workingFlight.value = f.autoValues()
            }
        }


    }


    /**
     *
     */

    init {
        _flightIsOpen.addSource(_openInEditorEventTrigger){
            _flightIsOpen.value =
                FeedbackEvent(if (it) FlightEditorOpenOrClosed.OPENED else FlightEditorOpenOrClosed.CLOSED)
        }
    }

    /**
     * Initially set workingFlight
     * - set flags to false
     * - set backup to initial value
     */
    private fun initialSetWorkingFlight(flight: Flight) {
        saving = false
        savedAndClosed = false
        backupFlight = flight
        origDestAircraftWorker.reset()
        setWithPreviousValuesIfNeeded(flight)
        checkIfFlightShouldBeIfr()


    }


    /*************************
     * Auto fill logic:
     *************************/

    /**
     * Applies all automatically calculated values to a flight
     * Also saves flight if [saving] is true
     */
    private fun Flight.autoValues(): Flight {
        return when {
            //Don't autofill anything
            isSim -> this // Don't auto anything on Sim sessions
            !autoFill -> this.checkIfCopilot()
            else -> this
                .withTakeoffLandings(if (isPF) 1 else 0, origin.value, destination.value)
                .updateNightTime(_workingFlight.value)
                .updateIFRTime()
                .updateMultiPilotTime()
                .checkIfCopilot()
        }.also{
            if (saving) saveWorkingFlight()
        }
    }

    /**
     * Run this after making sure aircraft type is updated
     */
    private fun Flight.checkIfCopilot(): Flight =
        this.copy(isCoPilot = (aircraft.value?.type?.multiPilot == true && !isPIC))


    /**
     * this will return a new flight with updated nighttime, corrected for augmented crew (same ratio)
     */

    private fun Flight.updateNightTime(old: Flight?): Flight {
        return if ((old?.orig != orig || old.dest != dest || old.timeIn != timeIn || old.timeOut != timeOut)) {
            val ratio = duration().toDouble()/calculatedDuration
            val twilightCalculator = TwilightCalculator(timeOut)
            val totalNightTime = twilightCalculator.minutesOfNight(
                this@WorkingFlightRepository.origin.value,
                this@WorkingFlightRepository.destination.value,
                timeOut,
                timeIn
            )
            val correctedNightTime = (crew?.getLogTime(totalNightTime, isPIC) ?: totalNightTime) * ratio
            this.copy(nightTime = correctedNightTime.toInt())
        } else this
    }

    private fun Flight.updateIFRTime(): Flight {
        val ifrTimeToSet: Int = if (thisFlightIsIFR.value == true) duration() else 0
        return copy(ifrTime = ifrTimeToSet)
    }

    private fun Flight.updateMultiPilotTime(): Flight{
        return if (aircraft.value?.type?.multiPilot == true) {
            copy(
                multiPilotTime = duration(),
                isCoPilot = !(isPIC || isPICUS),
            )
        } else this.copy(multiPilotTime = 0, isCoPilot = false)
    }

    private fun checkIfFlightShouldBeIfr() {
        if (flight?.isSim != true) return // don't do anything on null flight or sim
        val mostRecentFlight = flightRepository.getMostRecentFlightAsync()
        launch {
            flight?.let{
                thisFlightIsIFR.value =if (it.duration() > 0) it.ifrTime > 0 else mostRecentFlight.await()?.ifrTime ?: 0 > 0
            } ?: Log.w("WorkingFlightRepository", "Trying to check IFR status on a null flight")

        }
    }
    private fun setWithPreviousValuesIfNeeded(f: Flight){
        launch{
            if (f.isPlanned) {
                flightRepository.getMostRecentFlightAsync().await()?.let{oldFlight ->
                    updateWorkingFlight(
                        f.copy(
                            registration = f.registration.nullIfEmpty() ?: oldFlight.registration,
                            name = f.name.nullIfEmpty() ?: oldFlight.name,
                            name2 = f.name2.nullIfEmpty() ?: oldFlight.name2,
                            isPIC = oldFlight.isPIC
                        )
                    )
                }
            }
            else updateWorkingFlight(f)
        }
    }

    /*************************
     * Planned flight calendar sync check
     *************************/

    /**
     * Checks a bunch of things
     * - Is it a planned flight?
     * - Is calendarSync on?
     * - Is the flight changed in a way that will make it not match a planned flight?     *
     * @return time to disable calendarSync to if conflict, 0 if not
     *
     */
    fun checkConflictingWithCalendarSync(): Long {
        return flight?.let {
            when {
                !Preferences.getFlightsFromCalendar -> 0L                                            // not using calendar sync
                Preferences.calendarDisabledUntil >= backupFlight?.timeIn ?: 0 -> 0L                 // not using calendar sync for flight being edited
                !it.prepareForSave().isPlanned -> 0L                                                 // not planned, no problem
                backupFlight?.isSamedPlannedFlightAs(it.prepareForSave()) == true -> 0L              // editing a planned flight in a way that doesn't break sync
                backupFlight?.prepareForSave()?.timeOut ?: 0 < maxOf(
                    Preferences.calendarDisabledUntil,
                    Instant.now().epochSecond
                ) -> 0L       // editing a flight that starts before calendar sync cutoff
                backupFlight == null && it.timeOut > Instant.now().epochSecond -> it.timeIn + 1L       // If editing a new flight that starts in the future, 1 second after end of that flight
                else -> maxOf(
                    backupFlight?.timeIn ?: 0,
                    it.timeIn
                ) + 1L                         // In other cases, i second after latest timeIn of planned flight and workingFlight
            }
        } ?: 0
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
        get() = Transformations.map(origDestAircraftWorker.origAirport) {
            (it != null).also { go ->
                if (go && saving)
                    saveWorkingFlight(flight?.autoValues())
            }
        }

    val destInDatabase: LiveData<Boolean>
        get() = Transformations.map(origDestAircraftWorker.destAirport) {
            (it != null).also { go ->
                if (go && saving)
                    saveWorkingFlight(flight?.autoValues())
            }
        }

    val origin: LiveData<Airport?>
        get() = origDestAircraftWorker.origAirport

    val destination: LiveData<Airport?>
        get() = Transformations.map(origDestAircraftWorker.destAirport) {
            it.also {
                if (it != null && saving)
                    saveWorkingFlight(flight?.autoValues())
            }
        }

    val aircraft: LiveData<Aircraft?>
        get() = origDestAircraftWorker.aircraft


    var crew: Crew?
        get() = flight?.let { Crew.of(it.augmentedCrew) }
        set(crew) {
            if (crew != null && flight != null)
                updateWorkingFlight(flight!!.copy(augmentedCrew = crew.toInt()))
        }

    var isIfr: Boolean
        get() = thisFlightIsIFR.value ?: (flight?.ifrTime ?: -1 > 0)
        set(isIfr) {
            thisFlightIsIFR.value = isIfr
        }


    /*********************************************************************************************
     * Functions for creating, updating and saving
     *********************************************************************************************/

    /**
     * Updates working flight.
     */
    fun updateWorkingFlight(flight: Flight) {
        externallyUpdatedFlight.value = flight
    }

    /**
     * Saves working flight. Flight to save can be overridden but only private
     */
    fun saveWorkingFlight() = launch {
            _workingFlight.value?.let {
                saving = true
                flightRepository.save(it.prepareForSave())
            }
    }

    private fun saveWorkingFlight(override: Flight?) =
        override ?: _workingFlight.value?.let {
            saving = true
            flightRepository.save(it.prepareForSave())
        }

    fun undoSaveWorkingFlight() = backupFlight?.let {
        saving = false
        flightRepository.save(it)
    }
        ?: workingFlight.value?.let { flightRepository.delete(it) } // if backupFlight is not set, undo means deleting new flight


    fun notifyFinalSave() {
        savedAndClosed = true
        // Log.d("FINAL", "$flight")
    }

    /**
     * Creates an empty flight which is the reverse of the most recent completed flight
     */
    suspend fun createNewWorkingFlight() {
        coroutineScope {
            val highestID = flightRepository.getHighestIdAsync()
            val mostRecentFlight = flightRepository.getMostRecentFlightAsync()
            saving = false
            val done = async {
                updateWorkingFlight(
                    reverseFlight(
                        mostRecentFlight.await()
                            ?: Flight.createEmpty(),
                        highestID.await() + 1))
            }
            backupFlight = null
            done.await()
        }
    }

    /**
     * Fetch a flight from FlightRepository and set it as working flight. Returns that flight if successful
     */
    suspend fun fetchFlightByIdToWorkingFlight(id: Int): Flight? {
        Log.d("lalala","Hupfalderie fetching id $id")
        return flightRepository.fetchFlightByID(id)?.also {
            Log.d("lalala", "Found flight $it")
            withContext(Dispatchers.Main) { initialSetWorkingFlight(it) }
        } ?: null.also{Log.w("NOT_FOUND", "Flight $id not found!")}

    }

    /*
    /**
     * Updates working flight with same aircraft, name, name2 and isPic as last completed flight
     */
    fun updateWorkingFlightWithMostRecentData() {
        launch {
            flightRepository.getMostRecentFlightAsync().await()?.let { f ->
                    _workingFlight.value?.let {
                        val aircraft = it.aircraftType.nullIfEmpty() ?: f.aircraftType
                        val registration = it.registration.nullIfEmpty() ?: f.registration
                        val name = it.name.nullIfEmpty() ?: f.name
                        val name2 = it.name2.nullIfEmpty() ?: f.name2
                        val isPIC = f.isPIC

                        updateWorkingFlight(
                            it.copy(
                                aircraftType = aircraft,
                                registration = registration,
                                name = name,
                                name2 = name2,
                                isPIC = isPIC
                            )
                        )
                    }
            }
        }
    }
    */

    /*********************************************************************************************
     * Observable status variables
     *********************************************************************************************/

    val flightOpenEvents: LiveData<FeedbackEvent>
        get() = _flightIsOpen

    fun setOpenInEditor(isOpen: Boolean) {
        _isOpenInEditor.value = isOpen
    }

    val flightIsChanged: Boolean
        get() = (flight != backupFlight) && savedAndClosed


    /*********************************************************************************************
     * Companion object
     *********************************************************************************************/


    companion object {
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





