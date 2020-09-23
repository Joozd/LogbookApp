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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reverseFlight
import java.lang.NullPointerException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty

/**
 * Worker class for handling changes to a Flight that is being edited.
 * Has functions to retreive a flight from, or save it to [flightRepository]
 * has LiveData for things that need displaying *
 */
class WorkingFlight(flight: Flight): CoroutineScope by MainScope() {
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


    /**
     * Mutable LiveData for feeding UI (possible through it's ViewModels)
     */
    // private val _date = MutableLiveData<LocalDate?>()  comes from timeOut
    private val _flightNumber = MutableLiveData<String?>()
    private val _origin = MutableLiveData<Airport?>()
    private val _destination = MutableLiveData<Airport?>()
    private val _timeOut = MutableLiveData<Instant>()                      // date also comes from this
    private val _timeIn = MutableLiveData<Instant>()
    private val _correctedTotalTime = MutableLiveData<Int?>()
    private val _multiPilotTime = MutableLiveData<Int?>()
    private val _nightTime = MutableLiveData<Int?>()
    private val _ifrTime = MutableLiveData<Int?>()
    private val _isIfr = MutableLiveData<Boolean?>()
    private val _simTime = MutableLiveData<Int?>()
    private val _aircraft = MutableLiveData<Aircraft?>()
    private val _takeoffLanding =
        MutableLiveData<TakeoffLandings?>()        // consolidated into one observable for consequent updating
    private val _name = MutableLiveData<String?>()
    private val _name2 =
        MutableLiveData<String?>()                         // [name2List] also comes from this
    private val _remarks = MutableLiveData<String?>()
    private val _signature =
        MutableLiveData<String?>()                     // [isSigned] also comes from this
    private val _augmentedCrew = MutableLiveData<Crew?>()
    private val _isSim = MutableLiveData<Boolean?>()
    private val _isDual = MutableLiveData<Boolean?>()
    private val _isInstructor = MutableLiveData<Boolean?>()
    private val _isPic = MutableLiveData<Boolean?>()
    private val _isPF = MutableLiveData<Boolean?>()
    private val _isAutoValues = MutableLiveData<Boolean?>()

    /**
     * private vars for getting/setting mutable livedata
     * These will not launch couroutines for looking up/setting aditional data, only to be used private
     */
    private var mFlightNumber: String?
        get() = _flightNumber.value
        set(it) = _flightNumber.setValueOnMain(it)

    private var mOrigin: Airport?
        get() = _origin.value
        set(it) = _origin.setValueOnMain(it)

    private var mDestination: Airport?
        get() = _destination.value
        set(it) = _destination.setValueOnMain(it)

    private var mTimeOut: Instant
        get() = _timeOut.value ?: Instant.EPOCH
        set(it) {
            _timeOut.setValueOnMain(it)
            twilightCalculator = TwilightCalculator(it)
        }

    private var mTimeIn: Instant
        get() = _timeIn.value ?: Instant.EPOCH
        set(it) = _timeIn.setValueOnMain(it)

    private var mCorrectedTotalTime: Int?
        get() = _correctedTotalTime.value
        set(it) = _correctedTotalTime.setValueOnMain(it)

    private var mMultiPilotTime: Int?
        get() = _multiPilotTime.value
        set(it) = _multiPilotTime.setValueOnMain(it)

    private var mNightTime: Int?
        get() = _nightTime.value
        set(it) = _nightTime.setValueOnMain(it)

    private var mIfrTime: Int?
        get() = _ifrTime.value
        set(it) = _ifrTime.setValueOnMain(it)

    private var mIsIfr: Boolean
        get() = _isIfr.value ?: false
        set(it) = _isIfr.setValueOnMain(it)

    private var mSimTime: Int?
        get() = _simTime.value
        set(it) = _simTime.setValueOnMain(it)

    private var mAircraft: Aircraft?
        get() = _aircraft.value
        set(it) = _aircraft.setValueOnMain(it)

    private var mTakeoffLandings: TakeoffLandings
        get() = _takeoffLanding.value ?: TakeoffLandings()
        set(it) = _takeoffLanding.setValueOnMain(it)

    private var mName: String?
        get() = _name.value
        set(it) = _name.setValueOnMain(it)

    private var mName2: String?
        get() = _name2.value
        set(it) = _name2.setValueOnMain(it)

    private var mRemarks: String?
        get() = _remarks.value
        set(it) = _remarks.setValueOnMain(it)

    private var mSignature: String?
        get() = _signature.value
        set(it) = _signature.setValueOnMain(it)

    private var mAugmentedCrew: Crew?
        get() = _augmentedCrew.value
        set(it) = _augmentedCrew.setValueOnMain(it)

    private var mIsSim: Boolean?
        get() = _isSim.value
        set(it) = _isSim.setValueOnMain(it)

    private var mIsDual: Boolean?
        get() = _isDual.value
        set(it) = _isDual.setValueOnMain(it)

    private var mIsInstructor: Boolean?
        get() = _isInstructor.value
        set(it) = _isInstructor.setValueOnMain(it)

    private var mIsPic: Boolean?
        get() = _isPic.value
        set(it) = _isPic.setValueOnMain(it)

    private var mIsPF: Boolean?
        get() = _isPF.value
        set(it) = _isPF.setValueOnMain(it)

    private var mIsAutovalues: Boolean
        get() = _isAutoValues.value ?: false
        set(it) = _isAutoValues.setValueOnMain(it)

    /**
     * Duration as actual time to be logged, eg. with augmented crew correction
     * If correctedTotalTime != 0, it returns that.
     */
    private val mDuration: Duration
        get() = mCorrectedTotalTime?.nullIfZero()?.let { Duration.ofMinutes(it.toLong()) }
            ?: mAugmentedCrew?.let { crew ->
                crew.getLogTime(
                    (mTimeIn ?: Instant.EPOCH) - (mTimeOut ?: Instant.EPOCH),
                    mIsPic ?: false
                )
            } ?: Duration.ZERO.also {
                Log.w("WorkingFlight", "Duration 0 minutes because something is null")
            }


    /**
     * async data filler functions
     */

    /**
     * Set origin and dest with an Airport from AirportRepository
     * These functions should be run on Dispatchers.Main
     */
    private suspend fun setOriginFromId(id: String?) {
        require(Looper.myLooper() == Looper.getMainLooper()) { "run SetOriginFromID on Main thread to prevent unexpected behavior" }
        mOrigin = getAirport(id)
    }

    private suspend fun setDestFromId(id: String?) {
        require(Looper.myLooper() == Looper.getMainLooper()) { "run setDestFromId on Main thread to prevent unexpected behavior" }
        mDestination = getAirport(id)
    }

    /**
     * Set aircraft from flight. If type from Flight found in Aircraftypes it will use that,
     * otherwise it will get it from registration
     * If that doesn't fetch a type, it sets type to null
     */
    private suspend fun setAircraftFromFlight(f: Flight) {
        mAircraft =
            AircraftRepository.getInstance().getAircraftTypeByShortName(f.aircraftType)?.let {
                Aircraft(registration = f.registration, type = it, source = Aircraft.FLIGHT)
            } ?: getAircraftByRegistration(f.registration)
                    ?: Aircraft(registration = f.registration, type = null, source = Aircraft.NONE)
    }


    /**
     * Private helper functions
     */
    private suspend fun getAirport(id: String?): Airport? =
        AirportRepository.getInstance().getAirportByIcaoIdentOrNull(id)

    private suspend fun getAircraftByRegistration(reg: String?) =
        AircraftRepository.getInstance().getAircraftFromRegistration(reg)

    /**
     * Check both origin and destination are not situated at (0.0, 0.0)
     */
    private fun checkOrigandDestHaveLatLongSet() =
        (mOrigin?.latitude_deg ?: 0.0 != 0.0 || mOrigin?.longitude_deg ?: 0.0 != 0.0)
                && (mDestination?.latitude_deg ?: 0.0 != 0.0 || mDestination?.longitude_deg ?: 0.0 != 0.0)

    /**
     * Private extension functions
     */
    private fun <T> MutableLiveData<T>.setValueOnMain(newValue: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) value = newValue
        else launch(Dispatchers.Main) {
            Log.w("WorkingFlight", "Data implicitly set on main thread")
            value = newValue
        }
    }



    /**
     * Will set all data from [flight], directly into data fields
     * Then, will start async functions for fetching data from other sources (aircraft, airports etc)
     */
    private fun setFromFlight(flight: Flight) = with(flight) {
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
        mAugmentedCrew = Crew.of(augmentedCrew)
        mIsSim = isSim
        mIsDual = isDual
        mIsInstructor = isInstructor
        mIsPic = isPIC
        mIsPF = isPF
        // Async setting of values is always locked to prevent unexpected behavior
        launchWithLocks(origMutex, destMutex, aircraftMutex, nightTimeMutex) {
            setOriginFromId(orig)
            setDestFromId(dest)
            setAircraftFromFlight(flight)
        }
    }

    /***********************************************************************************************
     * Initialization block
     ***********************************************************************************************/

    init{
        setFromFlight(flight)
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
                val setNightAsync = async {
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
                if (mIsIfr) {
                    mIfrTime = mDuration.toMinutes().toInt()
                    ifrTimeMutex.unlock() // this can be changed safely on another thread now
                }

                if (mAircraft?.type?.multiPilot == true) {
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                    multiPilotMutex.unlock() // // this can be changed safely on another thread now
                }
                setNightAsync.await()
            }
        } //All used locks are released again

    /**
     * Sets timeIn to new value
     * If timeOut >= timeIn, timeIn will be adjusted to the same date, or one day later if needed
     * If timeOut is >24 hours before timeIn, timeIn will be adjusted also
     * This assumes mTimeOut is not null. Probably correct.
     */
    fun setTimeIn(tIn: Instant) =
        launchWithLocks(timeInOutMutex, nightTimeMutex, ifrTimeMutex, multiPilotMutex) {
            mTimeIn = tIn
            if (mTimeIn < mTimeOut || (mTimeIn) - mTimeOut > Duration.ofDays(1)
            ) mTimeIn = mTimeIn.atDate(tIn.toLocalDate()).let {
                if (it < mTimeOut) it else it.plusDays(1)
            }
            if (mIsAutovalues) {
                val setNightAsync = async {
                    if (checkOrigandDestHaveLatLongSet())
                        mNightTime = withContext(Dispatchers.Default) {
                            TwilightCalculator(tIn.epochSecond).minutesOfNight(
                                mOrigin,
                                mDestination,
                                mTimeOut,
                                mTimeIn
                            )
                        }
                }
                if (mIsIfr) {
                    mIfrTime = mDuration.toMinutes().toInt()
                    ifrTimeMutex.unlock() // this can be changed safely on another thread now
                }

                if (mAircraft?.type?.multiPilot == true) {
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                    multiPilotMutex.unlock() // // this can be changed safely on another thread now
                }
                setNightAsync.await()
            }
        } //All used locks are released again

    /**
     * Sets corrected Total Time. IFR will become the same if IFR active, night time will be per rato
     * multiPilot will become the same as needed
     */
    fun setCorrectedTotalTime(ctt: Int) =
        launchWithLocks(nightTimeMutex, ifrTimeMutex, multiPilotMutex) {
            mCorrectedTotalTime = ctt
            if (mIsAutovalues) {
                val setNightAsync = async {
                    if (checkOrigandDestHaveLatLongSet())
                        mTimeOut?.let { tOut ->
                            mNightTime = withContext(Dispatchers.Default) {
                                TwilightCalculator(tOut.epochSecond).minutesOfNight(
                                    mOrigin,
                                    mDestination,
                                    mTimeOut,
                                    mTimeIn
                                )
                            }
                        }
                }
                if (mIsIfr) {
                    mIfrTime = mDuration.toMinutes().toInt()
                    ifrTimeMutex.unlock() // this can be changed safely on another thread now
                }

                if (mAircraft?.type?.multiPilot == true) {
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                    multiPilotMutex.unlock() // // this can be changed safely on another thread now
                }
                setNightAsync.await()
            }
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
     * because otherwise it will get overwriten next time anything changes.
     * For changing IFRtime to 0 or whole flight, use setIFR(Boolean)
     * If this is set to 0 minutes, flight is set to non-IFR and autoValues is left unchanged
     */
    fun setIfrTime(newIfrTime: Int) = launchWithLocks(ifrTimeMutex) {
        mIfrTime = newIfrTime
        if (mIfrTime == 0) mIsIfr = false
        else mIsAutovalues = false
    }

    /**
     * Set sim time. Nothing depends on this so it just gets done without any locks
     */
    fun setSimTime(newSimTime: Int) {
        mSimTime = newSimTime
    }

    /**
     * Set Aircraft
     * If [mIsAutovalues], it also sets multiPilotTime
     */
    fun setAircraft(registration: String?, type: String? = null) =
        launchWithLocks(aircraftMutex, multiPilotMutex) {
            val repo = AircraftRepository.getInstance()
            if (registration == null && type == null) return@launchWithLocks
            mAircraft =
                if (type == null) repo.getAircraftFromRegistration(registration) else Aircraft(
                    registration = registration ?: mAircraft?.registration ?: "",
                    type = repo.getAircraftTypeByShortName(type),
                    source = Aircraft.FLIGHT
                )
            if (mIsAutovalues) mMultiPilotTime =
                if (mAircraft?.type?.multiPilot == true) mDuration.toMinutes().toInt() else 0
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
                val setNightAsync = async {
                    if (checkOrigandDestHaveLatLongSet())
                        mTimeOut?.let { tOut ->
                            mNightTime = withContext(Dispatchers.Default) {
                                TwilightCalculator(tOut.epochSecond).minutesOfNight(
                                    mOrigin,
                                    mDestination,
                                    mTimeOut,
                                    mTimeIn
                                )
                            }
                        }
                }
                if (mIsIfr) {
                    mIfrTime = mDuration.toMinutes().toInt()
                    ifrTimeMutex.unlock() // this can be changed safely on another thread now
                }

                if (mAircraft?.type?.multiPilot == true) {
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                    multiPilotMutex.unlock() // // this can be changed safely on another thread now
                }
                setNightAsync.await()
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
     * sets isPIC. Nothing depends on this.
     */
    fun setIsPic(isPic: Boolean) {
        mIsPic = isPic
    }

    /**
     * sets isPF. Nothing depends on this.
     */
    fun setIsPF(isPF: Boolean) {
        mIsPF = isPF
    }

    /**
     * sets isAutovalues. Does things when switched to on.
     */
    fun setAutoValues(autovalues: Boolean) =
        launchWithLocks(ifrTimeMutex, multiPilotMutex, nightTimeMutex) {
            mIsAutovalues = autovalues
            if (autovalues) {
                val setNightAsync = async {
                    if (checkOrigandDestHaveLatLongSet())
                        mTimeOut?.let { tOut ->
                            mNightTime = withContext(Dispatchers.Default) {
                                TwilightCalculator(tOut.epochSecond).minutesOfNight(
                                    mOrigin,
                                    mDestination,
                                    mTimeOut,
                                    mTimeIn
                                )
                            }
                        }
                }
                if (mIsIfr) {
                    mIfrTime = mDuration.toMinutes().toInt()
                    ifrTimeMutex.unlock() // this can be changed safely on another thread now
                }

                if (mAircraft?.type?.multiPilot == true) {
                    mMultiPilotTime = mDuration.toMinutes().toInt()
                    multiPilotMutex.unlock() // // this can be changed safely on another thread now
                }
                setNightAsync.await()
            }
        }

    /***************************
     * Save data to repository
     **************************/

    fun save() = launchWithLocks(
        NonCancellable,
        ifrTimeMutex,
        nightTimeMutex,
        multiPilotMutex,
        aircraftMutex,
        destMutex,
        origMutex,
        timeInOutMutex
    ) {
        val flightToSave = try {
            originalFlight.copy(
                orig = mOrigin!!.ident,
                dest = mDestination!!.ident,
                timeOut = mTimeOut!!.epochSecond,
                timeIn = mTimeIn!!.epochSecond,
                correctedTotalTime = mCorrectedTotalTime!!,
                multiPilotTime = mMultiPilotTime!!,
                nightTime = mNightTime!!,
                ifrTime = mIfrTime!!,
                simTime = mSimTime!!,
                registration = mAircraft!!.registration,
                aircraftType = mAircraft!!.type?.shortName ?: "",
                takeOffDay = takeoffDay,
                takeOffNight = takeoffNight,
                landingDay = landingDay,
                landingNight = landingNight,
                autoLand = autoLand,
                name = mName!!,
                name2 = mName2!!,
                remarks = mRemarks!!,
                signature = mSignature!!,
                augmentedCrew = mAugmentedCrew!!.toInt(),
                isSim = mIsSim!!,
                isDual = mIsDual!!,
                isInstructor = mIsInstructor!!,
                isPIC = mIsPic!!,
                isPF = mIsPF!!,
                autoFill = mIsAutovalues,
                timeStamp = TimestampMaker.nowForSycPurposes
            )
        } catch (ex: NullPointerException) {
            Log.w("WorkingFlight", "Null values in WorkingFlight, not saving")
            null
        }
        flightToSave?.let{f ->
            with (FlightRepository.getInstance()){
                save(f)
                setUndoSaveFlight(originalFlight)
                notifyFlightSaved()
            }
        }
    }


    /**
     * Exposed LiveData
     */
    val flightNumber: LiveData<String?>
        get() = _flightNumber

    val origin: LiveData<Airport?>
        get() = _origin

    val destination: LiveData<Airport?>
        get() = _destination

    val timeOut: LiveData<Instant?>
        get() = _timeOut

    val date: LiveData<LocalDate?> = Transformations.map(_timeOut) { it?.toLocalDate() }

    val timeIn: LiveData<Instant?>
        get() = _timeIn

    val correctedTotalTime: LiveData<Int?>
        get() = _correctedTotalTime

    val multiPilotTime: LiveData<Int?>
        get() = _multiPilotTime

    val nightTime: LiveData<Int?>
        get() = _nightTime

    val ifrTime: LiveData<Int?>
        get() = _ifrTime

    val isIfr: LiveData<Boolean?> = Transformations.map(_ifrTime) { it?.let { v -> v > 0 } }

    val simTime: LiveData<Int?>
        get() = _simTime

    val aircraft: LiveData<Aircraft?>
        get() = _aircraft

    val takeoffLandings: LiveData<TakeoffLandings?>
        get() = _takeoffLanding

    val name: LiveData<String?>
        get() = _name

    val name2: LiveData<String?>
        get() = _name2

    val name2List: LiveData<List<String>> =
        Transformations.map(_name2) { it?.split(";") ?: emptyList() }

    val remarks: LiveData<String?>
        get() = _remarks

    val signature: LiveData<String?>
        get() = _signature

    val isSigned: LiveData<Boolean?> = Transformations.map(_signature) { it?.isNotBlank() }

    val isSim: LiveData<Boolean?>
        get() = _isSim

    val isDual: LiveData<Boolean?>
        get() = _isDual

    val isInstructor: LiveData<Boolean?>
        get() = _isInstructor

    val isPic: LiveData<Boolean?>
        get() = _isPic

    val isPF: LiveData<Boolean?>
        get() = _isPF

    val isAutoValues: LiveData<Boolean?>
        get() = _isAutoValues

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
                GENERIC_TAKEOFF -> mTakeoffLandings.takeoffDay + mTakeoffLandings.takeoffNight
                GENERIC_LANDING -> mTakeoffLandings.landingDay + mTakeoffLandings.landingNight
                else -> error("Wrong type provided.")
            }
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            when (type) {
                TAKEOFF_DAY -> TakeoffLandings(takeoffDay = value)
                TAKEOFF_NIGHT -> TakeoffLandings(takeoffNight = value)
                LANDING_DAY -> TakeoffLandings(landingDay = value)
                LANDING_NIGHT -> TakeoffLandings(landingNight = value)
                AUTOLAND -> TakeoffLandings(autoLand = value)
                GENERIC_TAKEOFF -> { // This calls the appropriate version of this delegate
                    launchWithLocks(nightTimeMutex, origMutex){
                        val day = mOrigin == null || twilightCalculator.itIsDayAt(mOrigin!!, mTimeOut)
                        if (day) takeoffDay = value else takeoffNight = value

                    }
                    return
                }
                GENERIC_LANDING -> {    // This calls the appropriate version of this delegate
                    launchWithLocks(nightTimeMutex, destMutex) {
                        val day = mDestination == null || twilightCalculator.itIsDayAt(mDestination!!, mTimeIn)
                        if (day) landingDay = value else landingNight = value
                    }
                    return
                }
                

                else -> null
            }?.let {
                mTakeoffLandings += it
            }
        }
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
            return WorkingFlight(
                repo.getMostRecentFlightAsync().await()?.let { f ->
                    reverseFlight(f, idAsync.await() + 1)
                } ?: Flight(idAsync.await() + 1)
            )
        }
    }
}



