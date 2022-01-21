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

package nl.joozd.logbookapp.model.workingFlight

import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.crew.Crew
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryImpl
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAs
import nl.joozd.logbookapp.data.repository.helpers.prepareForSave
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reverseFlight
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

/**
 * A flight that is being worked on
 * All updates will trigger LiveData or MediatorLiveData updates.
 * LiveData Updates might trigger other LiveData updates.
 * MediatorLiveData updates might update the Flight being worked on, which might trigger new LiveData updates.
 * @param flight: The flight to edit. Will be considered "new" if flightID < 0 (for undo purposes)
 * @NOTE
 */
class WorkingFlight private constructor(flight: Flight): CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    private val airportRepository = AirportRepository.getInstance()

    // private var airportDataCache = airportRepository.getStaleOrEmptyAirportDataCache()
    private var aircraftDataCache = async(DispatcherProvider.io()) { AircraftRepository.getInstance().getAircraftDataCache() }


    // For undo purposes; if flightID < 0 this is a new flight so undo means delete
    private val originalFlight = if (flight.flightID < 0) null else flight.copy()

    private var _wfLiveData = MutableLiveData(flight)
    private var wf: Flight // only for internal updates, if Flight updated from outside, use [updateFlight]
        get() = _wfLiveData.value!!
        set(it) {
            _wfLiveData.value = it
        }


    /************************************************************************
     * Update logic
     ***********************************************************************/
    // This holds all update listeners
    private val updateListeners = HashMap<String, List<UpdateListener>>()

    /**
     * Runs all updates associated with the tags in [fieldsUpdated]
     * if [fieldsUpdated] contains [ALL] it will run all listeners.
     */
    private fun updated(vararg fieldsUpdated: String) {
        if (fieldsUpdated.contains(ALL))
            updateListeners.values.flatten().distinct().forEach {
                it()
            }
        else
            fieldsUpdated.forEach {
                updateListeners[it]?.forEach {
                    it()
                }
            }
    }

    /**
     * Add a listener to [updateListeners]
     */
    private fun addUpdateListener(tag: String, listener: UpdateListener) {
        updateListeners[tag] = (updateListeners[tag] ?: emptyList()) + listOf(listener)
    }

    /**
     * Remove a listener from [updateListeners]
     */
    private fun removeUpdateListener(tag: String, listener: UpdateListener) {
        updateListeners[tag] = (updateListeners[tag] ?: emptyList()).filter { it != listener }
    }


    /************************************************************************
     * Mutable LiveData
     ***********************************************************************/
    private val _originLiveData = MutableLiveData<Airport?>(Airport.placeholderWithIdentOnly(flight.orig))       // null if none found
    private val _destinationLiveData = MutableLiveData<Airport?>(Airport.placeholderWithIdentOnly(flight.dest))  // null if none found
    private val _aircraftLiveData = MutableLiveData<Aircraft?>()    // null if none found
    private val _isIfrLiveData = MutableLiveData(flight.ifrTime > 0)

    /************************************************************************
     * Mutexes
     ***********************************************************************/
    private val getAircraftMutex = Mutex()
    private val originMutex = Mutex()
    private val destinationMutex = Mutex()

    /***********************************************************************
     * Helper functions
     **********************************************************************/

    /**
     * Get airport from ICAO ID
     */
    private suspend fun getAirport(id: String): Airport? =
        airportRepository.getAirportByIcaoIdentOrNull(id)

    /**
     * Calculate night time. Doesn't check if autoValues is on.
     */
    private fun calculateNightTime(): Int {
        if (origin?.checkIfValidCoordinates() != true || destination?.checkIfValidCoordinates() != true) return 0
        if (instantOut == null || instantIn == null) return 0
        val totalNightMinutes = TwilightCalculator(instantOut!!).minutesOfNight(
            origin,
            destination,
            instantOut,
            instantIn
        )

        //Night time gets by ratio for durations other than entire flight (eg correctedTotalTime or augmented crew)
        val ratio = duration.toDouble() / Duration.between(instantOut, instantIn).toMinutes()
        return (totalNightMinutes * ratio).toInt()
    }

    /**
     * Function that retrieves aircraft from type and/or registration.
     */
    private suspend fun getAircraft(): Aircraft {
        val reg = wf.registration
        val typeString = wf.aircraftType
        return if (typeString.isBlank()) {
            findAircraftTypeFromRegistration(reg).also { ac ->
                // if we are here, aircraftType was not set.
                // It is set here if we found it from the registration.
                // This will get this function to run again, but that's OK as no blank values
                // will be written here.
                updateAircraftTypeIfNotNullOrBlank(ac)
            }
        } else {
            makeAircraftFromDataInFlight(reg, typeString)
        }
    }

    /*
     * If an aircraftType was entered in flight, but its registration is not known in AircraftRepository
     * (this can happen after an import, for instance), try to find the aircraft type in AircraftTypes dabatase,
     * or make a new (temporary) aircraft with only a short name (which is what will end up in the PDF logbook)
     *
     */
    private suspend fun makeAircraftFromDataInFlight(
        reg: String,
        typeString: String
    ) = Aircraft(
        registration = reg,
        type = aircraftDataCache.await().getAircraftTypeByShortName(typeString)
            ?: makeUnknownAircraftType(typeString),
        source = Aircraft.FLIGHT
    )

    private fun makeUnknownAircraftType(typeString: String) = AircraftType(
        "",
        typeString,
        multiPilot = isMultipilot,
        multiEngine = false
    )

    private fun updateAircraftTypeIfNotNullOrBlank(ac: Aircraft) {
        ac.type?.shortName?.takeIf { it.isNotBlank() }?.let { aircraftType = it }
    }

    private suspend fun findAircraftTypeFromRegistration(reg: String) =
        findAircraftByRegistrationFromRepositoryOrNull(reg)
            ?: Aircraft(reg)

    private suspend fun findAircraftByRegistrationFromRepositoryOrNull(reg: String): Aircraft? =
        aircraftDataCache.await().getAircraftFromRegistration(reg)


    /**
     * Returns multipilot time in minutes
     * Will return 0 if not all data available
     */
    private fun calculateMultiPilotTime(): Int = when {
        instantOut == null || instantIn == null || (aircraft?.type?.multiPilot != true && !isMultipilot) -> 0
        else -> duration
    }

    /**
     * Gets IFR Minutes, 0 if data missing or flight seems to be not IFR
     */
    private fun calculateIfrTime(): Int? = when {
        !isIfr -> 0
        instantOut == null || instantIn == null -> null
        else -> duration
    }

    /**
     * Calculates day and night landings
     * If not one takeoff and one landing, will leave it as is.
     * Same if not all required info available
     */
    private fun calculateDayNightLandings(): TakeoffLandings {
        with(takeoffLandings) {
            if (takeOffs != 1 || landings != 1) return this
            if (instantOut == null || instantIn == null || origin == null || destination == null) return this
            if (!origin!!.checkIfValidCoordinates() || !destination!!.checkIfValidCoordinates()) return this
            val calculator = TwilightCalculator(instantOut!!)
            val toDay = if (calculator.itIsDayAt(origin!!, instantOut!!)) 1 else 0
            val ldgDay = if (calculator.itIsDayAt(destination!!, instantIn!!)) 1 else 0
            return TakeoffLandings(toDay, 1 - toDay, ldgDay, 1 - ldgDay, this.autoLand)
        }

    }

    /**
     * Change times of flight to start at [date]
     */
    private fun changeDate(date: LocalDate) {
        val crossingMidnight: Boolean = instantIn?.toLocalTime() ?: LocalTime.MIDNIGHT < instantOut?.toLocalTime() ?: LocalTime.MIDNIGHT
        val tOut =
            instantOut?.atDate(date)?.epochSecond ?: 0
        val tIn = (instantIn?.atDate(date)?.plusDays(if (crossingMidnight) 1 else 0)
            ?: Instant.EPOCH)
            .epochSecond
        wf = wf.copy(timeOut = tOut, timeIn = tIn)
    }


    /************************************************************************
     * UpdateListeners
     ************************************************************************/
    private val originListener = UpdateListener {
        launch {
            originMutex.lock()
            origin = getAirport(wf.orig)
            originMutex.unlock()
        }
    }.also { it.addTo(ORIG, AIRPORT_DB) }

    private val destinationListener = UpdateListener {
        launch {
            destinationMutex.lock()
            destination = getAirport(wf.dest)
            destinationMutex.unlock()
        }
    }.also { it.addTo(DEST, AIRPORT_DB) }

    private val timeOutListener = UpdateListener {
        instantOut = Instant.ofEpochSecond(wf.timeOut)
    }.also { it.addTo(TIMEOUT, TIMES) }

    private val timeInListener = UpdateListener {
        instantIn = Instant.ofEpochSecond(wf.timeIn)
    }.also { it.addTo(TIMEIN, TIMES) }

    private val aircraftListener = UpdateListener {
        launch {
            getAircraftMutex.lock()
            aircraft = getAircraft().also {
                it.type?.let { type ->
                    if (isMultipilot == multiPilotTime > 0)  // isMultiPilot has not been overridden
                        isMultipilot = type.multiPilot
                }
            }
            getAircraftMutex.unlock()
        }
    }.addTo(AIRCRAFTTYPE, REGISTRATION, REG_AND_TYPE, AIRCRAFT_DB)

    /**
     * Updates night time on any of the following conditions:
     * - Origin or destination changed
     * - Instants in or out changed
     * - CorrectedTotalTime changed
     * - Augmented crew changed (or PIC changed which influences augmented times)
     * - Autofill set to on
     */
    private val nightTimeListener = UpdateListener {
        if (autoFill) nightTime = calculateNightTime()
    }.addTo(ORIGIN, DESTINATION, INSTANTOUT, INSTANTIN, CORRECTEDTOTALTIME, AUGMENTEDCREW, ISPIC, AUTOFILL)

    /**
     * Updates IFR time on any of the following conditions:
     * - isIFR changed
     * - Instants in or out changed
     * - CorrectedTotalTime changed
     * - Augmented crew changed (or PIC changed which influences augmented times)
     * - Autofill set to on
     *
     * NOTE: If setting ifrTime to anything other than 0 or [duration], make sure to disable autoFill first.
     */
    private val ifrTimeListener = UpdateListener {
        if (autoFill) calculateIfrTime()?.let { ifrTime = it }
    }.addTo(ISIFR, INSTANTOUT, INSTANTIN, CORRECTEDTOTALTIME, ISPIC, AUGMENTEDCREW, AUTOFILL)

    //This will trigger ISIFR. If IFRtime is set to >0 and autoFill is true, that means IFRTIME is reset to entire flight.
    private val isIFRListener = UpdateListener {
        isIfr = ifrTime > 0
    }.addTo(IFRTIME)

    private val multiPilotListener = UpdateListener {
        if (autoFill) multiPilotTime = calculateMultiPilotTime()
    }.addTo(INSTANTOUT, INSTANTIN, TIMES, AIRCRAFT, CORRECTEDTOTALTIME, ISMULTIPILOT, AUTOFILL)

    private val copilotListener = UpdateListener {
        isCoPilot = !isPIC && aircraft?.type?.multiPilot ?: false
    }.addTo(ISPIC, AIRCRAFT)

    private val dayNightLandingsListener = UpdateListener {
        setTakeOffLandings(calculateDayNightLandings(), noUpdate = true)
    }.addTo(TAKEOFFLANDINGS, AUTOFILL)

    private val correctedTotalTimeListener = UpdateListener {
        if (wf.correctedTotalTime != 0 && autoFill)
            autoFill = false
    }.also { it.addTo(CORRECTEDTOTALTIME) }

    // Most autoFills are taken care of by their own listeners,
    // things that need specific resetting go here
    private val autoFillListener = UpdateListener {
        if (autoFill) {
            correctedTotalTime = 0
        }
    }.addTo(AUTOFILL)



    /************************************************************************
     * public LiveData
     ************************************************************************/

    /**
     * From MutableLiveData
     */

    val originLiveData: LiveData<Airport?>
        get() = _originLiveData

    val destinationLiveData: LiveData<Airport?>
        get() = _destinationLiveData

    val aircraftLiveData: LiveData<Aircraft?>
        get() = _aircraftLiveData

    val isIfrLiveData: LiveData<Boolean>
        get() = _isIfrLiveData

    /**
     * Mapped from _workingFlight:
     */

    val origLiveData = _wfLiveData.map { it.orig }
    val destLiveData = _wfLiveData.map { it.dest }
    val timeOutLiveData = _wfLiveData.map { it.timeOut }
    val timeInLiveData = _wfLiveData.map { it.timeIn }
    val correctedTotalTimeLiveData = _wfLiveData.map { it.correctedTotalTime }
    val multiPilotTimeLiveData = _wfLiveData.map { it.multiPilotTime }

    val isMultipilotLiveData = _wfLiveData.map { it.multiPilotTime > 0}

    val nightTimeLiveData = _wfLiveData.map { it.nightTime }
    val ifrTimeLiveData = _wfLiveData.map { it.ifrTime }
    val simTimeLiveData = _wfLiveData.map { it.simTime }
    val aircraftTypeLiveData = _wfLiveData.map { it.aircraftType }
    val registrationLiveData = _wfLiveData.map { it.registration }
    val nameLiveData = _wfLiveData.map { it.name }
    val name2LiveData = _wfLiveData.map { it.name2 }


    val name2ListLiveData = _wfLiveData.map { it.name2.split(';').map {f -> f.trim()}}

    //allNamesListLiveData is a list of all names entered in this flight (name + all name2's)
    val allNamesListLiveData = _wfLiveData.map { listOf (it.name) + it.name2.split(';').map {f -> f.trim()} }

    val takeOffDayLiveData = _wfLiveData.map { it.takeOffDay }
    val takeOffNightLiveData = _wfLiveData.map { it.takeOffNight }
    val landingDayLiveData = _wfLiveData.map { it.landingDay }
    val landingNightLiveData = _wfLiveData.map { it.landingNight }
    val autoLandLiveData = _wfLiveData.map { it.autoLand }
    val takeoffLandingsLiveData = _wfLiveData.map { TakeoffLandings.fromFlight(it) }
    val flightNumberLiveData = _wfLiveData.map { it.flightNumber }
    val remarksLiveData = _wfLiveData.map { it.remarks }
    val isPICLiveData = _wfLiveData.map { it.isPIC }
    val isPICUSLiveData = _wfLiveData.map { it.isPICUS }
    val isCoPilotLiveData = _wfLiveData.map { it.isCoPilot }
    val isDualLiveData = _wfLiveData.map { it.isDual }
    val isInstructorLiveData = _wfLiveData.map { it.isInstructor }
    val isSimLiveData = _wfLiveData.map { it.isSim }
    val isPFLiveData = _wfLiveData.map { it.isPF }
    val isPlannedLiveData = _wfLiveData.map { it.isPlanned }
    val unknownToServerLiveData = _wfLiveData.map { it.unknownToServer }
    val autoFillLiveData = _wfLiveData.map { it.autoFill }
    val augmentedCrewLiveData = _wfLiveData.map { it.augmentedCrew }
    val crewLiveData = _wfLiveData.map { Crew.of(it.augmentedCrew) }
    val isAugmented = _wfLiveData.map { Crew.of(it.augmentedCrew).size > 2}
    val signatureLiveData = _wfLiveData.map { it.signature }
    val durationLiveData = _wfLiveData.map { it.duration() }

    /**
     * Other
     */


    /*
     * Things that can be set from anywhere:
     */
    var orig: String
        get() = wf.orig
        set(it) {
            wf = wf.copy(orig = it)
            updated(ORIG)
        }

    var dest: String
        get() = wf.dest
        set(it) {
            wf = wf.copy(dest = it)
            updated(DEST)
        }

    var timeOut: Long
        get() = wf.timeOut
        set(it) {
            wf = wf.copy(timeOut = it)
            updated(TIMEOUT)
        }

    var timeIn: Long
        get() = wf.timeIn
        set(it) {
            wf = wf.copy(timeIn = it)
            updated(TIMEIN)
        }

    // This will set autovalues to false if set to non-zero
    var correctedTotalTime: Int
        get() = wf.correctedTotalTime
        set(it) {
            wf = wf.copy(correctedTotalTime = it)
            updated(CORRECTEDTOTALTIME)
        }

    var multiPilotTime
        get() = wf.multiPilotTime
        set(it) {
            wf = wf.copy(multiPilotTime = it)
            updated((MULTIPILOTTIME))
        }

    var nightTime
        get() = wf.nightTime
        set(it) {
            wf = wf.copy(nightTime = it)
            updated(NIGHTTIME)
        }

    var ifrTime
        get() = wf.ifrTime
        set(it) {
            wf = wf.copy(ifrTime = it)
            updated(IFRTIME)
        }

    var simTime
        get() = wf.simTime
        set(it) {
            wf = wf.copy(simTime = it)
            updated(SIMTIME)
        }

    // Setting registration will reset type; if a type should be forced use setAircraft(reg, type)
    var registration
        get() = wf.registration
        set(it) {
            wf = wf.copy(registration = it)
            aircraftType = ""
            updated(REGISTRATION)
        }

    // If this is to be forced, use setAircraft(reg, type) when changing registration
    var aircraftType
        get() = wf.aircraftType
        set(it) {
            wf = wf.copy(aircraftType = it)
            updated(AIRCRAFTTYPE)
        }

    var name
        get() = wf.name
        set(it) {
            wf = wf.copy(name = it)
            updated(NAME)
        }

    var name2
        get() = wf.name2
        set(it) {
            wf = wf.copy(name2 = it)
            updated(NAME2)
        }

    var name2List
        get() = wf.name2.split(';').filter { it.isNotBlank() }
        set(it) {
            wf = wf.copy(name2 = it.joinToString(";"){it.trim()})
            updated(NAME2)
        }

    // cannot be negative, will round up to 0
    var takeOffDay
        get() = wf.takeOffDay
        set(it) {
            wf = wf.copy(takeOffDay = maxOf(it, 0))
            updated(TAKEOFFDAY)
        }

    // cannot be negative, will round up to 0
    var takeOffNight
        get() = wf.takeOffNight
        set(it) {
            wf = wf.copy(takeOffNight = maxOf(it, 0))
            updated(TAKEOFFNIGHT)
        }

    // cannot be negative, will round up to 0
    var landingDay
        get() = wf.landingDay
        set(it) {
            wf = wf.copy(landingDay = maxOf(it, 0))
            updated(LANDINGDAY)
        }

    // cannot be negative, will round up to 0
    var landingNight
        get() = wf.landingNight
        set(it) {
            wf = wf.copy(landingNight = maxOf(it, 0))
            updated(LANDINGNIGHT)
        }

    // cannot be negative, will round up to 0
    var autoLand
        get() = wf.autoLand
        set(it) {
            wf = wf.copy(autoLand = maxOf(it, 0))
            updated(AUTOLAND)
        }

    var flightNumber
        get() = wf.flightNumber
        set(it) {
            wf = wf.copy(flightNumber = it)
            updated(FLIGHTNUMBER)
        }

    var remarks
        get() = wf.remarks
        set(it) {
            wf = wf.copy(remarks = it)
            updated(REMARKS)
        }

    var isPIC
        get() = wf.isPIC
        set(it) {
            wf = wf.copy(isPIC = it)
            updated(ISPIC)
        }

    var isPICUS
        get() = wf.isPICUS
        set(it) {
            wf = wf.copy(isPICUS = it)
            updated(ISPICUS)
        }

    var isCoPilot
        get() = wf.isCoPilot
        set(it) {
            wf = wf.copy(isCoPilot = it)
            updated(ISCOPILOT)
        }

    var isDual
        get() = wf.isDual
        set(it) {
            wf = wf.copy(isDual = it)
            updated(ISDUAL)
        }

    var isInstructor
        get() = wf.isInstructor
        set(it) {
            wf = wf.copy(isInstructor = it)
            updated(ISINSTRUCTOR)
        }

    var isSim
        get() = wf.isSim
        set(it) {
            wf = wf.copy(isSim = it)
            updated(ISSIM)
        }

    var isPF
        get() = wf.isPF
        set(it) {
            wf = wf.copy(isPF = it)
            updated(ISPF)
        }

    var isPlanned
        get() = wf.isPlanned
        set(it) {
            wf = wf.copy(isPlanned = it)
            updated(ISPLANNED)
        }

    //TODO don't forget to set correctedTotalTime to 0 if on
    var autoFill
        get() = wf.autoFill
        set(it) {
            wf = wf.copy(autoFill = it)
            updated(AUTOFILL)
        }

    var augmentedCrew
        get() = wf.augmentedCrew
        set(it) {
            wf = wf.copy(augmentedCrew = it)
            updated(AUGMENTEDCREW)
        }

    var crew: Crew
        get() = Crew.of(wf.augmentedCrew)
        set(it) {
            wf = wf.copy (augmentedCrew = it.toInt())
            updated(AUGMENTEDCREW)
        }

    var signature
        get() = wf.signature
        set(it) {
            wf = wf.copy(signature = it)
            updated(SIGNATURE)
        }

    // This is not saved in flight but derived from [ifrTime].
    var isIfr
        get() = _isIfrLiveData.value!! // this is set at construction so will always have a value
        set(it) {
            if (it != isIfr) { // to prevent loops
                _isIfrLiveData.value = it
                updated(ISIFR)
            }
        }

    var isMultipilot
        get() = isMultipilotLiveData.value ?: false
        set(it) {
            if (it != isMultipilot) { // to prevent loops
                wf = wf.copy (multiPilotTime = if (it) wf.duration() else 0)
                updated(ISMULTIPILOT)
            }
        }

    /////////////////
    // Below this have private setters, set source data if you want to change them
    /////////////////
    var origin: Airport?
        get() = _originLiveData.value
        set(it) {
            _originLiveData.value = it
            updated(ORIGIN)
        }

    var destination: Airport?
        get() = _destinationLiveData.value
        set(it) {
            _destinationLiveData.value = it
            updated(DESTINATION)
        }

    var instantOut: Instant? = null
        private set(it) {
            field = it
            updated(INSTANTOUT)
        }

    var instantIn: Instant? = null
        private set(it) {
            field = it
            updated(INSTANTIN)
        }

    var aircraft: Aircraft?
        get() = _aircraftLiveData.value
        private set(it) {
            _aircraftLiveData.value = it
            updated(AIRCRAFT)
        }

    var date: LocalDate?
        get() = instantOut?.toLocalDate()
        set(it) {
            it?.let {
                changeDate(it)
                updated(TIMES)
            }
        }


    var takeoffLandings: TakeoffLandings
        get() = TakeoffLandings(
            wf.takeOffDay,
            wf.takeOffNight,
            wf.landingDay,
            wf.landingNight,
            wf.autoLand
        )
        set(it) = setTakeOffLandings(it, false)

    /**
     * Immutable data:
     */

    //If you want to directly set this, set [correctedTotalTime]
    val duration: Int
        get() = wf.duration()

    //Change either name or name2
    val allNamesList: List<String> get() = allNamesListLiveData.value ?: emptyList()


    /**
     * Check if this flight could cause a calendar sync conflict
     * This is the case in case of a new flight (original flight == null)
     * or the flight has been changed in a way that original will return false on [isSameFlightAs]
     * A not-planned flight will never cause a conflict.
     */
    val canCauseCalendarConflict: Boolean
        get() = wf.isPlanned && originalFlight?.let {
            !it.isSameFlightAs(this.toFlight())
        } ?: true

    /**
     * Whether or not this is a new flight
     */
    val newFlight: Boolean = originalFlight == null

    /************************************************************************
     * Public functions
     ***********************************************************************/

    /**
     * Sets takeoffs and landings
     * @param noUpdate: Do not trigger an update (if this is run from an update, to prevent loops)
     */
    fun setTakeOffLandings(takeoffLandings: TakeoffLandings, noUpdate: Boolean) {
        wf = wf.copy(
            takeOffDay = takeoffLandings.takeoffDay,
            takeOffNight = takeoffLandings.takeoffNight,
            landingDay = takeoffLandings.landingDay,
            landingNight = takeoffLandings.landingNight,
            autoLand = takeoffLandings.autoLand
        )
        if (!noUpdate) {
            updated(TAKEOFFLANDINGS)
        }

    }

    /**
     * Set aircraft reg and type
     */
    fun setAircraft(reg: String?, type: String?) {
        wf = wf.copy(registration = reg ?: "", aircraftType = type ?: "")
        updated(REG_AND_TYPE)
    }

    /**
     * Sets aircraft from an Aircraft
     */
    fun setAircraftHard(ac: Aircraft?) {
        this.aircraft = ac
        wf = wf.copy(registration = ac?.registration ?: "", aircraftType = ac?.type?.shortName ?: "")
        updated(AIRCRAFT)
    }

    /**
     * Update whole flight, runs all updateListeners.
     */
    fun setFromFlight(flight: Flight) {
        wf = flight
        updated(ALL)
    }

    /**
     * Set [timeOut] from a [LocalTime]
     */
    fun setTimeOut(time: LocalTime) {
        timeOut = instantOut?.atTime(time)?.epochSecond ?: 0
        fixTimeInIfNeeded()
    }

    /**
     * Set [timeIn] from a [LocalTime]
     */
    fun setTimeIn(time: LocalTime) {
        timeIn = instantIn?.atTime(time)?.epochSecond ?: 0
        fixTimeInIfNeeded()
    }

    /**
     * Notify airport DB updated
     */
    fun notifyAirportDbUpdated(){
        updated(AIRPORT_DB)
    }

    /**
     * Notify aircraft DB updated
     */
    fun notifyAircraftDbUpdated(){
        updated(AIRCRAFT_DB)
    }


    /**
     * Make sure [timeIn] is within (0..24) hours from [timeOut]
     */
    private fun fixTimeInIfNeeded() {
        println("FIXING TIME IF NEEDED: $instantOut - $instantIn")
        if (instantIn!! !in (instantOut!!..instantOut!!.plusDays(1))) {
            println("It is not in ${(instantOut!!..instantOut!!.plusDays(1))}")
            val initialIn = instantIn!!.atDate(instantOut!!.toLocalDate()).also{ println("this is $it")} .epochSecond
            println("InitialIn: $initialIn")
            println("InitialIn > timeOut: ${initialIn > timeOut}")
            timeIn = if (initialIn > timeOut) initialIn else initialIn + 86400 // plus one day
            println("TimeIn: $timeIn")
        }
    }

    /**
     * Will (async) set isPIC and isIFR to same as in last completed flight
     */
    fun autoSetIfrAndPic(ifr: Boolean = true, pic: Boolean = true, names: Boolean = true) = launch {
        FlightRepositoryImpl.getInstance().getMostRecentFlightAsync().await()?.let{ lastCompleted ->
            if (pic) isPIC = lastCompleted.isPIC
            if (ifr) isIfr = lastCompleted.ifrTime > 0
            if (names) {
                if (name.isBlank()) name = lastCompleted.name
                if (name2.isBlank()) name2 = lastCompleted.name2
            }
        }
    }

    /**
     * Gives a snapshot
     * This is used for canceling a dialog; any background work will be done again after sending
     * this snapshot to [setFromFlight] so it is no problem is any background work is still
     * running before snapshot is taken as it will be done again anyway.
     */
    fun toFlight() = wf.copy()

    /**
     * Waits for all currently running jobs to finish, at which time [wf] should be ready to save
     * Also tells [FlightRepositoryImpl] to remove this flight when done.
     */
    fun saveAndClose(): Job {
        // All jobs running in [this]. Snapshotted here so next launch won't wait for itself to finish
        val runningTasks = job.children.toList()
        return launch {
            runningTasks.joinAll()
            FlightRepositoryImpl.getInstance().let { repo ->
                repo.save(wf.prepareForSave(), addToUndo = true)
                repo.changedFlight = originalFlight

                repo.closeWorkingFlight()
            }
        }
    }

    /**
     * Interface for Update Listeners
     */
    fun interface UpdateListener {
        operator fun invoke()
    }

    /**
     * Add listener to [tags]
     */
    private fun UpdateListener.addTo(vararg tags: String) {
        tags.forEach { addUpdateListener(it, this) }
    }

    //on initial creation, run all updates
    init{
        updated(ALL)
    }

    companion object {
        /**
         * Identifiers for values
         */
        private const val FLIGHTID = "FLIGHTID"
        private const val ORIG = "ORIG"
        private const val DEST = "DEST"
        private const val TIMEOUT = "TIMEOUT"
        private const val TIMEIN = "TIMEIN"
        private const val CORRECTEDTOTALTIME = "CORRECTEDTOTALTIME"
        private const val MULTIPILOTTIME = "MULTIPILOTTIME"
        private const val NIGHTTIME = "NIGHTTIME"
        private const val IFRTIME = "IFRTIME:IN"
        private const val SIMTIME = "SIMTIME"
        private const val AIRCRAFTTYPE = "AIRCRAFTTYPE"
        private const val REGISTRATION = "REGISTRATION"
        private const val NAME = "NAME"
        private const val NAME2 = "NAME2"
        private const val TAKEOFFDAY = "TAKEOFFDAY"
        private const val TAKEOFFNIGHT = "TAKEOFFNIGHT"
        private const val LANDINGDAY = "LANDINGDAY"
        private const val LANDINGNIGHT = "LANDINGNIGHT"
        private const val AUTOLAND = "AUTOLAND"
        private const val FLIGHTNUMBER = "FLIGHTNUMBER"
        private const val REMARKS = "REMARKS"
        private const val ISPIC = "ISPIC"
        private const val ISPICUS = "ISPICUS"
        private const val ISCOPILOT = "ISCOPILOT"
        private const val ISDUAL = "ISDUAL"
        private const val ISINSTRUCTOR = "ISINSTRUCTOR"
        private const val ISSIM = "ISSIM"
        private const val ISPF = "ISPF"
        private const val ISPLANNED = "ISPLANNED"
        private const val UNKNOWNTOSERVER = "UNKNOWNTOSERVER"
        private const val AUTOFILL = "AUTOFILL"
        private const val AUGMENTEDCREW = "AUGMENTEDCREW"
        private const val DELETEFLAG = "DELETEFLAG"
        private const val TIMESTAMP = "TIMESTAMP"
        private const val SIGNATURE = "SIGNATURE"

        // Identifiers meaning multiple things were updated
        private const val ALL = "ALL"
        private const val REG_AND_TYPE = "REG_AND_TYPE"
        private const val TAKEOFFLANDINGS = "TAKEOFFLANDINGS"
        private const val TIMES = "TIMES"

        // Identifiers for own calculated values:
        private const val ORIGIN = "ORIGIN"
        private const val DESTINATION = "DESTINATION"
        private const val AIRCRAFT = "AIRCRAFT"
        private const val INSTANTOUT = "INSTANTOUT"
        private const val INSTANTIN = "INSTANTIN"
        private const val CREW = "CREW"
        private const val ISIFR = "ISIFR"
        private const val ISMULTIPILOT = "ISMULTIPILOT"

        // Identifiers for external sources:
        private const val AIRPORT_DB = "AIRPORT_DB"
        private const val AIRCRAFT_DB = "AIRPORT_DB"

        /**
         * Gets a flight from a flightID, null if none found.
         * @return a [WorkingFlight] object of this flight and runs all updates on it.
         */
        suspend fun fromFlightID(id: Int) = withContext(Dispatchers.Main){
            FlightRepositoryImpl.getInstance().fetchFlightByID(id)?.let { WorkingFlight(it) }
        }

        /**
         * Creates a new flight with standard data from most recent completed flight in flight repository
         * If none found, creates an empty flight
         */
        suspend fun createNew(): WorkingFlight {
            val repo = FlightRepositoryImpl.getInstance()
            val fAsync = repo.getMostRecentFlightAsync()
            val f = fAsync.await()?.let { f ->
                reverseFlight(f, repo.lowestFreeFlightID())
            } ?: Flight(repo.lowestFreeFlightID())
            return withContext(Dispatchers.Main){WorkingFlight(f)
            }
        }
    }
}

