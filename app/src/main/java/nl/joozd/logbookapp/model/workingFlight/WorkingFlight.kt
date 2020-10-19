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

package nl.joozd.logbookapp.model.workingFlight

import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.crew.Crew
import nl.joozd.logbookapp.data.miscClasses.crew.ObservableCrew
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.prepareForSave
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reverseFlight
import java.time.*
import kotlin.reflect.KProperty

/**
 * Worker class for handling changes to a Flight that is being edited.
 * Has functions to retreive a flight from, or save it to [FlightRepository]
 * has LiveData for things that need displaying *
 */
class WorkingFlight(flight: Flight, val newFlight: Boolean = false): CoroutineScope by MainScope() {

    private val initialFlight = flight
    /***********************************************************************************************
     * Private parts
     ***********************************************************************************************/
    private lateinit var originalFlight: Flight
    private lateinit var twilightCalculator: TwilightCalculator // this is set whenever mTimeOut is set

    // Use all locks of data that will be changed in changing function.
    private val origMutex = Mutex()
    private val destMutex = Mutex()
    private val timeInOutMutex = Mutex()
    private val nightTimeMutex = Mutex()
    private val ifrTimeMutex = Mutex()
    private val aircraftMutex = Mutex()
    private val multiPilotMutex = Mutex()
    private val takeoffLandingMutex = Mutex()


    /**
     * Mutable LiveData for feeding UI (possible through it's ViewModels)
     */
    // private val _date = MutableLiveData<LocalDate?>()  comes from timeOut
    private val _flightNumber = MutableLiveData<String>()
    private val _origin = MutableLiveData<Airport?>()
    private val _destination = MutableLiveData<Airport?>()                  // null while searching
    private val _timeOut = MutableLiveData<Instant>()                      // date also comes from this
    private val _timeIn = MutableLiveData<Instant>()
    private val _correctedTotalTime = MutableLiveData<Int>()
    private val _multiPilotTime = MutableLiveData<Int>()
    private val _nightTime = MutableLiveData<Int>()
    private val _ifrTime = MutableLiveData<Int>()
    private val _isIfr = MutableLiveData<Boolean>()
    private val _simTime = MutableLiveData<Int>()
    private val _aircraft = MutableLiveData<Aircraft?>()                    // null while searching
    private val _takeoffLanding =
        MutableLiveData<TakeoffLandings>()        // consolidated into one observable for consequent updating
    private val _name = MutableLiveData<String>()
    private val _name2 =
        MutableLiveData<String>()                         // [name2List] also comes from this
    private val _remarks = MutableLiveData<String>()
    private val _signature =
        MutableLiveData<String>()                     // [isSigned] also comes from this
    // private val _augmentedCrew = MutableLiveData(Crew()) // this is moved to [crew] which will hold all things crew.
    private val _isSim = MutableLiveData<Boolean>()
    private val _isDual = MutableLiveData<Boolean>()
    private val _isInstructor = MutableLiveData<Boolean>()
    private val _isPic = MutableLiveData<Boolean>()
    private val _isPF = MutableLiveData<Boolean>()
    private val _isAutoValues = MutableLiveData<Boolean>()

    private val _isCopilot = MediatorLiveData<Boolean>()
    private val _duration = MediatorLiveData<Duration>()
    private val _allNamesList = MediatorLiveData<List<String>>()

    /**
     * crew composition:
     * This is a var so operator functions like ++ and -- can be used
     */
    @Suppress("SetterBackingFieldAssignment")
    val crew = ObservableCrew()



    /**
     * private vars for getting/setting mutable livedata
     * These will not launch couroutines for looking up/setting aditional data, only to be used private
     */
    private val mDate: LocalDate
        get() = date.value!!

    private var mFlightNumber: String
        get() = _flightNumber.value!!
        set(it) = _flightNumber.postValue(it)

    /**
     * Don't set this to null, Only nullable because first use is async.
     */
    private var mOrigin: Airport?
        get() = _origin.value
        set(it) {
            require (it != null) { "Don't set mOrigin to null" }
            _origin.postValue(it)
        }

    /**
     * Don't set this to null, Only nullable because first use is async.
     */
    private var mDestination: Airport?
        get() = _destination.value
        set(it) {
            require (it != null) { "Don't set mDestination to null" }
            _destination.postValue(it)
        }

    private var mTimeOut: Instant
        get() = _timeOut.value ?: Instant.EPOCH
        set(it) {
            _timeOut.postValue(it)
            twilightCalculator = TwilightCalculator(it)
        }

    private var mTimeIn: Instant
        get() = _timeIn.value ?: Instant.EPOCH
        set(it) = _timeIn.postValue(it)

    private var mCorrectedTotalTime: Int
        get() = _correctedTotalTime.value!!
        set(it) = _correctedTotalTime.postValue(it)

    private var mMultiPilotTime: Int
        get() = _multiPilotTime.value!!
        set(it) = _multiPilotTime.postValue(it)

    private var mNightTime: Int
        get() = _nightTime.value!!
        set(it) = _nightTime.postValue(it)

    private var mIfrTime: Int
        get() = _ifrTime.value!!
        set(it) = _ifrTime.postValue(it)

    private var mIsIfr: Boolean
        get() = _isIfr.value!!
        set(it) = _isIfr.postValue(it)

    private var mSimTime: Int
        get() = _simTime.value!!
        set(it) = _simTime.postValue(it)

    private var mAircraft: Aircraft?
        get() = _aircraft.value
        set(it) = _aircraft.postValue(it)

    private var mTakeoffLandings: TakeoffLandings
        get() = _takeoffLanding.value ?: TakeoffLandings()
        set(it) = _takeoffLanding.postValue(it)

    private var mName: String
        get() = _name.value!!
        set(it) = _name.postValue(it)

    private var mName2: String
        get() = _name2.value!!
        set(it) = _name2.postValue(it)

    private var mName2List: List<String>
        get() = name2List.value!!
        set(namesList) { mName2 = namesList.joinToString(";") }

    private var mAllNamesList: List<String>
        get() = allNamesList.value ?: emptyList()
        set(namesList) { _allNamesList.value = namesList }

    private var mRemarks: String
        get() = _remarks.value!!
        set(it) = _remarks.postValue(it)

    private var mSignature: String
        get() = _signature.value!!
        set(it) = _signature.postValue(it)

    private var mAugmentedCrew: Crew
        get() = crew
        set(it) = crew.clone(it)

    private var mIsSim: Boolean
        get() = _isSim.value!!
        set(it) = _isSim.postValue(it)

    private var mIsDual: Boolean
        get() = _isDual.value!!
        set(it) = _isDual.postValue(it)

    private var mIsInstructor: Boolean
        get() = _isInstructor.value!!
        set(it) = _isInstructor.postValue(it)

    private var mIsPic: Boolean
        get() = _isPic.value!!
        set(it) = _isPic.postValue(it)

    private var mIsPF: Boolean
        get() = _isPF.value!!
        set(it) = _isPF.postValue(it)

    private var mIsAutovalues: Boolean
        get() = _isAutoValues.value!!
        set(it) = _isAutoValues.postValue(it)

    private var mIsCopilot: Boolean
        get() = _isCopilot.value ?: isPic.value == false && _aircraft.value?.type?.multiPilot == true // generate value if _isCopilot hasn't been observed yet
        set(it) = _isCopilot.postValue(it)

    private var mDuration: Duration
        get() = _duration.value ?: correctedDuration() // generate value if _duration hasn't been observed yet
        set(it) = _duration.postValue(it)


    /**
     * async data filler functions
     */

    /**
     * Set origin and dest with an Airport from AirportRepository
     * These functions should be run on Dispatchers.Main
     */
    private suspend fun setOriginFromId(id: String) {
        require(Looper.myLooper() == Looper.getMainLooper()) { "run SetOriginFromID on Main thread to prevent unexpected behavior" }
        mOrigin = getAirport(id) ?: Airport(ident = id)
    }

    private suspend fun setDestFromId(id: String) {
        require(Looper.myLooper() == Looper.getMainLooper()) { "run setDestFromId on Main thread to prevent unexpected behavior" }
        mDestination = getAirport(id) ?: Airport(ident = id)
    }

    /**
     * Set aircraft from flight. If type from Flight found in Aircraftypes it will use that,
     * otherwise it will get it from registration
     * If that doesn't fetch a type, it sets type to null
     */
    private fun setAircraftFromFlight(f: Flight) {
        mAircraft =
            AircraftRepository.getInstance().getAircraftTypeByShortName(f.aircraftType)?.let {
                Aircraft(registration = f.registration, type = it, source = Aircraft.FLIGHT)
            } ?: getAircraftByRegistration(f.registration)
                    ?: Aircraft(registration = f.registration, type = null, source = Aircraft.NONE)
    }


    /**
     * Private helper functions
     */
    private suspend fun getAirport(id: String): Airport? =
        AirportRepository.getInstance().getAirportByIcaoIdentOrNull(id)

    private fun getAircraftByRegistration(reg: String) =
        AircraftRepository.getInstance().getAircraftFromRegistration(reg)

    /**
     * Check if takeoff and landing being day or night are in concert with Airports and times
     * This is done by setting takeoff and landing to itself.
     * [TakeOffLandingDelegate] will take care of things.
     */
    private fun setDayOrNightForTakeoffLandings() {
        if (!checkOrigandDestHaveLatLongSet()) return
        takeoff = takeoff
        landing = landing
    }

    /**
     * Set nighttime async
     */
    private fun calculateNightJob(): Job = launch {
        if (checkOrigandDestHaveLatLongSet())
            mNightTime = withContext(Dispatchers.Default) {
                twilightCalculator.minutesOfNight(
                    mOrigin,
                    mDestination,
                    mTimeOut,
                    mTimeIn
                )
            }
    }

    /**
     * Duration as actual time to be logged, eg. with augmented crew correction
     * If correctedTotalTime != 0, it returns that.
     */
    private fun correctedDuration(): Duration = mCorrectedTotalTime.nullIfZero()?.let { Duration.ofMinutes(it.toLong()) }
        ?: mAugmentedCrew.getLogTime(mTimeIn - mTimeOut,mIsPic)


    /**
     * Check both origin and destination are not situated at (0.0, 0.0)
     */
    private fun checkOrigandDestHaveLatLongSet() =
        (mOrigin?.checkIfValidCoordinates() ?: false)
                && (mDestination?.checkIfValidCoordinates() ?: false)

    /**
     * Gives all data in this WorkingFlight as a [Flight]
     */
    fun toFlight() = originalFlight.copy(
        flightNumber = mFlightNumber,
        orig = mOrigin!!.ident,
        dest = mDestination!!.ident,
        timeOut = mTimeOut.epochSecond,
        timeIn = mTimeIn.epochSecond,
        correctedTotalTime = mCorrectedTotalTime,
        multiPilotTime = mMultiPilotTime,
        nightTime = mNightTime,
        ifrTime = mIfrTime,
        simTime = mSimTime,
        registration = mAircraft!!.registration,
        aircraftType = mAircraft!!.type?.shortName ?: "",
        takeOffDay = takeoffDay,
        takeOffNight = takeoffNight,
        landingDay = landingDay,
        landingNight = landingNight,
        autoLand = autoLand,
        name = mName,
        name2 = mName2,
        remarks = mRemarks,
        signature = mSignature,
        augmentedCrew = mAugmentedCrew.toInt(),
        isSim = mIsSim,
        isDual = mIsDual,
        isInstructor = mIsInstructor,
        isPIC = mIsPic,
        isCoPilot = mIsCopilot,
        isPF = mIsPF,
        autoFill = mIsAutovalues
    )


    /**
     * Will set all data from [flight], directly into data fields
     * Then, will start async functions for fetching data from other sources (aircraft, airports etc)
     */
    fun setFromFlight(flight: Flight) = with(flight) {
        originalFlight = this
        mFlightNumber = flightNumber
        // mOrigin = Airport()                          // This gets to be filled async
        // mDestination = Airport()                     // This gets to be filled async
        mTimeOut = Instant.ofEpochSecond(timeOut)       // this also sets [twilightCalculator]
        mTimeIn = Instant.ofEpochSecond(timeIn)
        mCorrectedTotalTime = correctedTotalTime
        mMultiPilotTime = multiPilotTime
        mNightTime = nightTime
        mIfrTime = ifrTime
        mIsIfr = ifrTime > 0
        mSimTime = simTime
        // mAicraft = Aircraft()                        // This gets to be filled async
        mTakeoffLandings = TakeoffLandings(
            takeoffDay = takeOffDay,
            takeoffNight = takeOffNight,
            landingNight = landingNight,
            landingDay = landingDay,
            autoLand = autoLand
        )
        mName = name
        mName2 = name2
        mRemarks = remarks
        mSignature = signature
        mIsSim = isSim
        mIsDual = isDual
        mIsInstructor = isInstructor
        mIsPic = isPIC
        mIsPF = isPF
        // Async setting of values is always locked to prevent unexpected behavior
        launchWithLocks(origMutex, destMutex, aircraftMutex, nightTimeMutex, multiPilotMutex) {
            crew.clone(augmentedCrew)
            setOriginFromId(orig)
            setDestFromId(dest)
            setAircraftFromFlight(flight)

            // Do stuff for planned flight that has autoValues enabled:
            if (isPlanned && autoFill) FlightRepository.getInstance().getMostRecentFlightAsync().await()?.let { f ->
                calculateNightJob().join() // set night time
                mIsIfr = f.ifrTime > 0
                if (mIsIfr) mIfrTime = duration()
                if (mName.isBlank()) mName = f.name
                if (mName2.isBlank()) mName2 = f.name2
                mIsPic = f.isPIC
                if (flight.registration.isBlank()) setAircraft(f.registration)
            }
        }
        mIsAutovalues = autoFill
    }


    /***********************************************************************************************
     * Public parts
     ***********************************************************************************************/

    /***************
     * Setters
     **************/


    /**
     * changes timeOut to a specific date, and timeIn to the same date or one day later, if it would mean <= 0 flight time
     * Also check night time since that is different on a different date.
     */
    fun setDate(date: LocalDate) = launchWithLocks(timeInOutMutex, nightTimeMutex) {
        mTimeOut = mTimeOut.atDate(date)
        mTimeIn = mTimeIn.atDate(date).let {
            if (it > mTimeOut) it else it.plusDays(1)
        }
        if (mIsAutovalues) {

            if (checkOrigandDestHaveLatLongSet())
                mNightTime = twilightCalculator.minutesOfNight(
                    mOrigin,
                    mDestination,
                    mTimeOut,
                    mTimeIn
                )
        }
    }

    /**
     * Set Flightnumber. Nothing depends on this so it just gets done without any locks
     */
    fun setFlightNumber(newFlightNumber: String){
        mFlightNumber = newFlightNumber
    }

    /**
     * Sets [origin] to the [Airport] of which that is the [Airport.ident] (ICAO code)
     * If not found, sets [origin] to Airport(ident = orig)
     * if found and [isAutoValues], calculates night time
     */
    fun setOrig(orig: String) = launchWithLocks(origMutex, nightTimeMutex){
        setOriginFromId(AirportRepository.getInstance().searchAirportOnce(orig)?.ident ?: orig)
    }

    /**
     * Sets [origin] to [orig]
     * if found and [isAutoValues], calculates night time
     * Can be null but shouldn't
     */
    fun setOrig(orig: Airport?) = launchWithLocks(origMutex, nightTimeMutex){
        mOrigin = orig ?: Airport(ident = "null")
        if (mIsAutovalues){
            calculateNightJob().join()
            setDayOrNightForTakeoffLandings()
        }
    }

    /**
     * Sets [origin] to the [Airport] of which that is the [Airport.ident] (ICAO code)
     * If not found, sets [origin] to to Airport(ident = dest)
     * if found and [isAutoValues], calculates night time
     */
    fun setDest(dest: String) = launchWithLocks(destMutex, nightTimeMutex){
        setDestFromId(AirportRepository.getInstance().searchAirportOnce(dest)?.ident ?: dest)
    }

    /**
     * Sets [destination] to [dest]
     * if found and [isAutoValues], calculates night time
     * Can be null but shouldn't
     */
    fun setDest(dest: Airport?) = launchWithLocks(destMutex, nightTimeMutex){
        mDestination = dest ?: Airport(ident = "null")
        if (mIsAutovalues){
            calculateNightJob().join()
            setDayOrNightForTakeoffLandings()
        }
    }


    /**
     * Sets timeOut to new value
     * If that value is >= timeIn, timeIn will be adjusted to the same date, or one day later if needed
     * If that value is >24 hours before timeIn, timeIn will be adjusted also
     */
    fun setTimeOut(tOut: Instant) =
        launchWithLocks(timeInOutMutex, nightTimeMutex, ifrTimeMutex, multiPilotMutex) {
            mTimeOut = tOut
            if (mTimeIn < tOut || (mTimeIn) - tOut > Duration.ofDays(1)
            ) mTimeIn = mTimeIn.atDate(tOut.toLocalDate()).let {
                if (it > tOut) it else it.plusDays(1)
            }
            twilightCalculator = TwilightCalculator(tOut.epochSecond)
            if (mIsAutovalues) {
                val setNightJob = calculateNightJob()
                if (mIsIfr)
                    mIfrTime = mDuration.toMinutes().toInt()

                if (mAircraft?.type?.multiPilot == true)
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                setNightJob.join()
                setDayOrNightForTakeoffLandings()
            }
        } //All used locks are released again

    /**
     * Set time Out to a time at current date
     */
    fun setTimeOut(time: LocalTime) = setTimeOut(LocalDateTime.of(mDate, time).toInstant(ZoneOffset.UTC))

    /**
     * Sets timeIn to new value
     * If timeOut >= timeIn, timeIn will be adjusted to the same date, or one day later if needed
     * If timeOut is >24 hours before timeIn, timeIn will be adjusted also
     * This assumes mTimeOut is not null. Probably correct.
     */
    fun setTimeIn(tIn: Instant) =
        launchWithLocks(timeInOutMutex, nightTimeMutex, ifrTimeMutex, multiPilotMutex) {
            mTimeIn = if (tIn > mTimeOut) tIn else tIn + Duration.ofDays(1)
            if (mIsAutovalues) {
                val setNightJob = calculateNightJob()
                if (mIsIfr)
                    mIfrTime = mDuration.toMinutes().toInt()

                if (mAircraft?.type?.multiPilot == true)
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                setNightJob.join()
                setDayOrNightForTakeoffLandings()
            }
        } //All used locks are released again

    /**
     * Set time In to a time at current date
     */
    fun setTimeIn(time: LocalTime) = setTimeIn(LocalDateTime.of(mDate, time).toInstant(ZoneOffset.UTC))

    /**
     * Sets corrected Total Time. IFR will become the same if IFR active, night time will be per rato
     * multiPilot will become the same as needed
     *
     * If not set to 0, it will set autoValues to false after applying ifr and night corrections if autoValues was true
     *
     * Set to 0 if no total time is not corrected (off blocks - on blocks with possible augmentedCrew corrections)
     */
    fun setCorrectedTotalTime(ctt: Int) =
        launchWithLocks(nightTimeMutex, ifrTimeMutex, multiPilotMutex) {
            mCorrectedTotalTime = ctt
            if (mIsAutovalues) {
                val setNightJob = calculateNightJob()
                if (mIsIfr)
                    mIfrTime = mDuration.toMinutes().toInt()

                if (mAircraft?.type?.multiPilot == true)
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                setNightJob.join()
            }
            if (ctt != 0) mIsAutovalues = false
        }

    /**
     * Set multipilotTime. Setting this will result in autoValues being set to off
     * because otherwise it will get overwriten next time anything changes
     */
    fun setMultipilotTime(newMPTime: Int) = launchWithLocks(multiPilotMutex) {
        mMultiPilotTime = newMPTime
        mIsAutovalues = false
    }

    /**
     * Set nightTime. Setting this will result in autoValues being set to off
     * because otherwise it will get overwriten next time anything changes
     */
    fun setNightTime(newNightTime: Int) = launchWithLocks(nightTimeMutex) {
        mNightTime = newNightTime
        mIsAutovalues = false
    }

    /**
     * Set ifrTime. Setting this will result in autoValues being set to off
     * because otherwise it will get overwritten next time anything changes.
     * For changing IFRtime to 0 or whole flight, use setIsIfr(Boolean)
     * @see setIsIfr
     */
    fun setIfrTime(newIfrTime: Int) = launchWithLocks(ifrTimeMutex) {
        mIfrTime = newIfrTime
        mIsAutovalues = false
    }

    /**
     * Set sim time. Nothing depends on this so it just gets done without any locks
     */
    fun setSimTime(newSimTime: Int) {
        mSimTime = newSimTime
    }

    /**
     * Set Aircraft
     * If both reg and type set, sets registration and type accordingly
     * If only reg set, looks for type, if none found leaves that at null
     * If only type set (ie. when [isSim]) sets type to correct type, or a blank placeholder type which will set [AircraftType.shortName] in [Flight.aircraftType] when saved.
     * If [mIsAutovalues], it also sets multiPilotTime
     */
    fun setAircraft(registration: String? = null, type: String? = null) =
        launchWithLocks(aircraftMutex, multiPilotMutex) {
            val repo = AircraftRepository.getInstance()
            if (registration == null && type == null) return@launchWithLocks
            mAircraft =
                if (type == null) repo.getAircraftFromRegistration(registration)?: Aircraft(registration ?: "") else Aircraft(
                    registration = registration ?: mAircraft?.registration ?: "",
                    type = repo.getAircraftTypeByShortName(type) ?: AircraftType("", type, multiPilot = false, multiEngine = false), // If type not found, use entered data as type shortname (which is what will end up in logbook)
                    source = Aircraft.FLIGHT
                )
            if (mIsAutovalues) mMultiPilotTime =
                if (mAircraft?.type?.multiPilot == true) mDuration.toMinutes().toInt() else 0
        }

    /**
     * Set aircraft
     * If [aircraft] is null, it will reset from flight (this can happen when AircraftPicker dialog wants to reset after being initialized before [mAircraft] was filled.)
     * @see [setAircraft] above
     */
    fun setAircraft(aircraft: Aircraft?){
        if (aircraft != null) setAircraft(aircraft.registration, aircraft.type?.shortName)
        else {
            launchWithLocks(multiPilotMutex, aircraftMutex) {
                setAircraftFromFlight(initialFlight)
            }
        }
    }

    /**
     * Setters for takeoff/landing/autoLand
     * Use with takeOffDay++ or TakeOffDay = 3
     */
    var takeoffDay: Int by TakeOffLandingDelegate(TAKEOFF_DAY)
    var takeoffNight: Int by TakeOffLandingDelegate(TAKEOFF_NIGHT)
    var landingDay: Int by TakeOffLandingDelegate(LANDING_DAY)
    var landingNight: Int by TakeOffLandingDelegate(LANDING_NIGHT)
    var autoLand: Int by TakeOffLandingDelegate(AUTOLAND)
    var takeoff: Int by TakeOffLandingDelegate(GENERIC_TAKEOFF)
    var landing: Int by TakeOffLandingDelegate(GENERIC_LANDING)

    /**
     * Set name. Nothing depends on this so can be done without any locks
     */
    fun setName(name: String) {
        mName = name
    }

    /**
     * @see setName
     */
    fun setName2(name2: String) {
        mName2 = name2
    }

    /**
     * Set name2 to current list of names through [mName2List]
     */
    fun setNames2List(names: List<String>) {
        mName2List = names
    }

    /**
     * Joins all names to one string and puts it in name2
     */
    fun setName2(names: List<String>) {
        mName2 = names.joinToString(";")
    }

    /**
     * Set remarks. Nothing depends on this so can be done without any locks
     */
    fun setRemarks(remarks: String) {
        mRemarks = remarks
    }

    /**
     * Set signature. Nothing depends on this so can be done without any locks
     */
    fun setSignature(sig: String) {
        mSignature = sig
    }

    /**
     * Sets augmented crew. If [mIsAutovalues] it will update times as well
     */
    fun setAugmentedCrew(augmentedCrew: Int) = setAugmentedCrew(Crew.of(augmentedCrew))
    fun setAugmentedCrew(crew: Crew) =
        launchWithLocks(ifrTimeMutex, nightTimeMutex, multiPilotMutex) {
            mAugmentedCrew = crew
            if (mIsAutovalues) {
                val setNightJob = calculateNightJob()
                if (mIsIfr)
                    mIfrTime = mDuration.toMinutes().toInt()
                if (mAircraft?.type?.multiPilot == true)
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                setNightJob.join()
                takeoff = if (crew.takeoff) 1 else 0
                landing = if (crew.landing) 1 else 0
            }
        }

    /**
     * sets isSim. Nothing depends on this.
     */
    fun setIsSim(isSim: Boolean) {
        mIsSim = isSim
    }

    /**
     * sets isDual. Nothing depends on this.
     */
    fun setIsDual(isDual: Boolean) {
        mIsDual = isDual
    }

    /**
     * sets isInstructor. Nothing depends on this.
     */
    fun setIsInstructor(isInstructor: Boolean) {
        mIsInstructor = isInstructor
    }

    /**
     * Sets IFR time to 0 or to whole flight
     */
    fun setIsIfr(isIFR: Boolean) = launchWithLocks(ifrTimeMutex){
        mIsIfr = isIFR
        mIfrTime = if (isIFR) mDuration.toMinutes().toInt() else 0
    }

    /**
     * sets isPIC. Nothing depends on this.
     */
    fun setIsPic(isPic: Boolean) {
        mIsPic = isPic
    }

    /**
     * sets isCopilot. Nothing depends on this.
     * Will be reset if [mAircraft] or [mIsPic] change
     */
    fun setIsCopilot(isCopilot: Boolean) {
        mIsCopilot = isCopilot
    }



    /**
     * sets isPF. If mAugmentedCrew.crewSize > 2 it will also adjust all times
     * (shortcut do doing that is resetting timeOut to itself so it will recalculate)
     */
    fun setIsPF(isPF: Boolean) {
        mIsPF = isPF
        if (crew.crewSize.value!! > 2) setTimeOut(mTimeOut)
    }

    /**
     * sets isAutovalues. Does things when switched to on.
     */
    fun setAutoValues(autovalues: Boolean) =
        launchWithLocks(ifrTimeMutex, multiPilotMutex, nightTimeMutex, takeoffLandingMutex) {
            mIsAutovalues = autovalues
            if (autovalues) {
                val setNightJob = calculateNightJob()
                if (mIsIfr) {
                    mIfrTime = mDuration.toMinutes().toInt()
                }

                if (mAircraft?.type?.multiPilot == true) {
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                }
                setNightJob.join()
                setDayOrNightForTakeoffLandings()
            }
        }

    /**
     * Set TakeoffLandings to a TakeoffLandings object, eg for undo purposes
     */
    fun setTakeoffLandings(takeoffLandings: TakeoffLandings){
        mTakeoffLandings = takeoffLandings
    }

    /***************************
     * Save data to repository
     **************************/

    fun saveAndClose(): Job = launchWithLocks(
        NonCancellable,
        ifrTimeMutex,
        nightTimeMutex,
        multiPilotMutex,
        aircraftMutex,
        destMutex,
        origMutex,
        timeInOutMutex,
        takeoffLandingMutex
    ) {
        Log.d("lalalala", "huppakeeee")
        val flightToSave = try {
            toFlight()
        } catch (ex: NullPointerException) {
            Log.w("WorkingFlight", "Null values in WorkingFlight, not saving:\n${ex.printStackTrace()}")
            null
        }
        flightToSave?.let{f ->
            with (FlightRepository.getInstance()){
                save(f.prepareForSave(), notify = true)
                undoSaveFlight = if (newFlight) null else originalFlight

                closeWorkingFlight()
            }
        }
    }


    /**
     * Exposed LiveData
     * Things that are filled async can be null, rest cannot. (will cause nullpointerexception)
     */
    val flightNumber: LiveData<String>
        get() = _flightNumber

    val origin: LiveData<Airport?>
        get() = _origin

    val destination: LiveData<Airport?>
        get() = _destination

    val timeOut: LiveData<Instant>
        get() = _timeOut

    val date: LiveData<LocalDate> = Transformations.map(_timeOut) { it!!.toLocalDate() }

    val timeIn: LiveData<Instant>
        get() = _timeIn

    val correctedTotalTime: LiveData<Int>
        get() = _correctedTotalTime

    val multiPilotTime: LiveData<Int>
        get() = _multiPilotTime

    val nightTime: LiveData<Int>
        get() = _nightTime

    val ifrTime: LiveData<Int>
        get() = _ifrTime

    val isIfr: LiveData<Boolean>
    get() = _isIfr

    val simTime: LiveData<Int>
        get() = _simTime

    val aircraft: LiveData<Aircraft?>
        get() = _aircraft

    val takeoffLandings: LiveData<TakeoffLandings>
        get() = _takeoffLanding

    val name: LiveData<String>
        get() = _name

    val name2: LiveData<String>
        get() = _name2

    val name2List: LiveData<List<String>> =
        Transformations.map(_name2) { it?.split(";") ?: emptyList() }

    val allNamesList: LiveData<List<String>>
        get() = _allNamesList

    val remarks: LiveData<String>
        get() = _remarks

    val signature: LiveData<String>
        get() = _signature

    val isSigned: LiveData<Boolean> = Transformations.map(_signature) { it.isNotBlank() }

    val isSim: LiveData<Boolean>
        get() = _isSim

    val isDual: LiveData<Boolean>
        get() = _isDual

    val isInstructor: LiveData<Boolean>
        get() = _isInstructor

    val isPic: LiveData<Boolean>
        get() = _isPic

    val isPF: LiveData<Boolean>
        get() = _isPF

    val isAutoValues: LiveData<Boolean>
        get() = _isAutoValues

    val isCopilot: LiveData<Boolean>
        get() = _isCopilot

    val duration: LiveData<Duration>
        get() = _duration

    /**
     * Inner classes
     */
    private inner class TakeOffLandingDelegate(val type: Int) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return when (type) {
                TAKEOFF_DAY -> mTakeoffLandings.takeoffDay
                TAKEOFF_NIGHT -> mTakeoffLandings.takeoffNight
                LANDING_DAY -> mTakeoffLandings.landingDay
                LANDING_NIGHT -> mTakeoffLandings.landingNight
                AUTOLAND -> mTakeoffLandings.autoLand
                GENERIC_TAKEOFF -> takeoffDay + takeoffNight
                GENERIC_LANDING -> landingDay + landingNight
                else -> error("Wrong type provided.")
            }
        }

        /**
         * setValue will set values to given value or 0, whichever is greater (cannot do -2 landings)
         */
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            when (type) {
                TAKEOFF_DAY -> mTakeoffLandings.copy(takeoffDay = maxOf(value, 0))
                TAKEOFF_NIGHT -> mTakeoffLandings.copy(takeoffNight = maxOf(value, 0))
                LANDING_DAY -> mTakeoffLandings.copy(landingDay = maxOf(value, 0))
                LANDING_NIGHT -> mTakeoffLandings.copy(landingNight = maxOf(value, 0))
                AUTOLAND -> mTakeoffLandings.copy(autoLand = maxOf(value, 0))
                GENERIC_TAKEOFF -> { // This calls the appropriate version of this delegate
                    launchWithLocks(nightTimeMutex, origMutex, takeoffLandingMutex){
                        Log.d("GENERIC_TAKEOFF", "Setting $thisRef to $value")
                        val day = mOrigin == null || twilightCalculator.itIsDayAt(mOrigin!!, mTimeOut)
                        Log.d("GENERIC_TAKEOFF", "day == $day")
                        if (day) takeoffDay = value else takeoffNight = value
                    }
                    return
                }
                GENERIC_LANDING -> {    // This calls the appropriate version of this delegate
                    launchWithLocks(nightTimeMutex, destMutex, takeoffLandingMutex) {
                        Log.d("GENERIC_LANDING", "Setting $thisRef to $value")
                        val day = mDestination == null || twilightCalculator.itIsDayAt(mDestination!!, mTimeIn)
                        if (day) landingDay = value else landingNight = value
                    }
                    return
                }


                else -> null
            }?.let {
                mTakeoffLandings = it
            }
        }
    }


    /***********************************************************************************************
     * Initialization block
     ***********************************************************************************************/

    init{
        /**
         * connect mediatorLiveData
         */
        _isCopilot.addSource(_isPic){isPic ->
            Log.d("LALALALA", "yolo yolo swag $isPic")
            _isCopilot.value = (!isPic && _aircraft.value?.type?.multiPilot == true)
        }
        _isCopilot.addSource(_aircraft){ac ->
            _isCopilot.value = (isPic.value == false && ac?.type?.multiPilot == true)
        }

        _duration.addSource(_timeIn) { mDuration = correctedDuration() }
        _duration.addSource(_timeOut) { mDuration = correctedDuration() }
        _duration.addSource(crew.changed) { mDuration = correctedDuration() }
        _duration.addSource(_isPic) { mDuration = correctedDuration() }
        _duration.addSource(_correctedTotalTime) { mDuration = correctedDuration() }

        _allNamesList.addSource(_name) { mAllNamesList = (name2List.value ?: emptyList<String>() + mName).distinct() } // null catcher because name2List might not be observed yet
        _allNamesList.addSource(name2List) { mAllNamesList = (mName2List + mName).distinct() }

        //Set all data
        setFromFlight(flight)
    }


    companion object {
        /**
         * Fixed identifiers for takeOffLanding delegates
         */
        const val TAKEOFF_DAY = 1
        const val TAKEOFF_NIGHT = 2
        const val LANDING_DAY = 3
        const val LANDING_NIGHT = 4
        const val AUTOLAND = 5
        const val GENERIC_TAKEOFF = 6
        const val GENERIC_LANDING = 7


        /**
         * Create a WorkingFlight object from a flight in Flight repository, found by flightId.
         * @param flightId: ID of the flight to be retreived from flight repository
         * @return: a WorkingFlight object filled with data form this flight, or null if nothing found with that ID
         */
        suspend fun fromFlightId(flightId: Int): WorkingFlight? =
            withContext(Dispatchers.Main) {
                FlightRepository.getInstance().fetchFlightByID(flightId)?.let { f ->
                    WorkingFlight(f)
                }
            }

        /**
         * Creates a new flight with standard data from most recent completed flight in flight repository
         * If none found, creates an empty flight
         */
        suspend fun createNew(): WorkingFlight {
            val repo = FlightRepository.getInstance()
            val idAsync = repo.getHighestIdAsync()
            val f = repo.getMostRecentFlightAsync().await()?.let { f ->
                reverseFlight(f, idAsync.await() + 1)
            } ?: Flight(idAsync.await() + 1)
            return WorkingFlight(f, newFlight = true)
        }
    }
}



