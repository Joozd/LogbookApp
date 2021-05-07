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

package nl.joozd.logbookapp.data.miscClasses.crew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.extensions.getBit
import nl.joozd.logbookapp.extensions.setBit
import nl.joozd.logbookapp.extensions.toInt
import java.time.Duration

/**
 *  Observable version of [Crew]
 *  Comes with setters for values.
 */
class ObservableCrew(iCrewSize: Int = 2,
           iDidTakeoff: Boolean = true,
           iDidLanding: Boolean = true,
           iTakeoffLandingTimes: Int = 0): Crew(iCrewSize, iDidTakeoff, iDidLanding, iTakeoffLandingTimes)
{
    private val _crewsize =  MutableLiveData(iCrewSize)

    private val _didTakeoff = MutableLiveData(iDidTakeoff)

    private val _didLanding = MutableLiveData(iDidLanding)

    private val _takeoffLandingTimes = MutableLiveData(iTakeoffLandingTimes)

    /**
     * Helper to let functions trigger whenever anything here has changed
     */
    private val _anythingChanged = MediatorLiveData<Boolean>()
    init{
        _anythingChanged.addSource(_crewsize) { _anythingChanged.value = true}
        _anythingChanged.addSource(_didTakeoff) { _anythingChanged.value = true}
        _anythingChanged.addSource(_didLanding) { _anythingChanged.value = true}
        _anythingChanged.addSource(_takeoffLandingTimes) { _anythingChanged.value = true}
    }

    override var mCrewSize: Int
        get() = _crewsize.value!!
        set(it) { _crewsize.value = it }

    override var mDidTakeoff: Boolean
        get() = _didTakeoff.value!!
        set(it) { _didTakeoff.value = it }


    override var mDidLanding: Boolean
        get() = _didLanding.value!!
        set(it) { _didLanding.value = it }

    override var mTakeoffLandingTimes: Int
        get() = _takeoffLandingTimes.value!!
        set(it) { _takeoffLandingTimes.value = it }

    val crewSize: LiveData<Int>
        get() = _crewsize

    val didTakeoff: LiveData<Boolean>
        get() = _didTakeoff

    val didLanding: LiveData<Boolean>
        get() = _didLanding

    val takeoffLandingTimes: LiveData<Int>
        get() = _takeoffLandingTimes

    val changed: LiveData<Boolean>
        get() = _anythingChanged

    fun setCrew(crew: Int){
        mCrewSize = crew.putInRange(CREW_RANGE)
    }

    fun setDidTakeoff(didTakeoff: Boolean){
        mDidTakeoff = didTakeoff
    }

    fun setDidLanding(didLanding: Boolean){
        mDidLanding = didLanding
    }

    /**
     * Will drop some most significant bytes when saving, but 33M minutes are too many anyway
     */
    fun setNonstandardTakeoffLandingTimes(newTimes: Int){
        mTakeoffLandingTimes = newTimes
    }

    /**
     * Set all data to the same as [crewToClone]
     */
    fun clone(crewToClone: Crew){
        mCrewSize = crewToClone.size
        mDidTakeoff = crewToClone.takeoff
        mDidLanding = crewToClone.landing
        mTakeoffLandingTimes = crewToClone.times
    }

    /**
     * Make a crew from [crewInt] and clone that
     */
    fun clone(crewInt: Int){
        clone(Crew.of(crewInt))
    }

    private fun Int.putInRange(range: IntRange): Int {
        require (!range.isEmpty()) { "cannot put an int in a range without elements"}
        return when {
            this in range -> this
            this < range.minOrNull()!! -> range.minOrNull()!!
            this > range.maxOrNull()!! -> range.maxOrNull()!!
            else -> error ("Value $this neither in our outside of $range...")
        }
    }
}