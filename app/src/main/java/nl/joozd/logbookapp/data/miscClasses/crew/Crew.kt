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

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.getBit
import nl.joozd.logbookapp.extensions.setBit
import nl.joozd.logbookapp.extensions.toInt
import java.time.Duration

/************************************************************************************
 * CrewValue will store info on augmented crews:                                    *
 * bits 0-3: amount of crew (no crews >15 ppl :) )                                  *
 * bit 4: in seat on takeoff                                                        *
 * bit 5: in seat on landing                                                        *
 * bit 6-31: amount of time reserved for takeoff/landing (standard in settings)     *
 ************************************************************************************/
open class Crew(iCrewSize: Int = 2,
           iDidTakeoff: Boolean = true,
           iDidLanding: Boolean = true,
           iTakeoffLandingTimes: Int = 0)
{
    protected open var mCrewSize: Int = iCrewSize

    protected open var mDidTakeoff: Boolean = iDidTakeoff

    protected open var mDidLanding: Boolean = iDidLanding

    protected open var mTakeoffLandingTimes: Int = iTakeoffLandingTimes

    /**
     * Getters for stored values.
     */
    val size
        get() = mCrewSize

    val takeoff
        get() = mDidTakeoff

    val landing
        get() = mDidLanding

    val times
        get() = mTakeoffLandingTimes

    fun toInt():Int {
        var value = if (mCrewSize > 15) 15 else mCrewSize
        value = value.setBit(4, mDidTakeoff).setBit(5, mDidLanding)
        value += mTakeoffLandingTimes.shl(6)
        return value
    }

    /**
     * Return amount of time to log. Cannot be negative, so 3 man ops for a 20 min flight with 30 mins to/landing is 0 minutes to log.
     */
    fun getLogTime(totalTime: Int, pic: Boolean): Int{
        if (pic || mCrewSize <=2) return totalTime
        return maxOf(0, (((totalTime.toFloat()-2*mTakeoffLandingTimes)/mCrewSize) * 2 + mTakeoffLandingTimes * (mDidTakeoff.toInt() + mDidLanding.toInt()) + 0.5).toInt())
    }

    fun getLogTime(totalTime: Duration, pic: Boolean): Duration{
        if (pic || mCrewSize <=2) return totalTime
        return Duration.ofMinutes(maxOf(0L, (((totalTime.toMinutes().toFloat()-2*mTakeoffLandingTimes)/mCrewSize) * 2 + mTakeoffLandingTimes * (mDidTakeoff.toInt() + mDidLanding.toInt()) + 0.5).toLong()))
    }

    operator fun plus(extraCrewMembers: Int): Crew = Crew ((mCrewSize + extraCrewMembers).putInRange((1..15)), mDidTakeoff, mDidLanding, mTakeoffLandingTimes)
    operator fun minus(extraCrewMembers: Int): Crew = Crew ((mCrewSize - extraCrewMembers).putInRange((1..15)), mDidTakeoff, mDidLanding, mTakeoffLandingTimes)


    operator fun plusAssign(extraCrewMembers: Int){
        mCrewSize += extraCrewMembers
        mCrewSize = mCrewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
    }

    operator fun minusAssign(fewerCrewMebers: Int){
        mCrewSize -= fewerCrewMebers
        mCrewSize = mCrewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
    }

    operator fun inc(): Crew {
        mCrewSize++
        mCrewSize = mCrewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
        return this
    }

    operator fun dec(): Crew {
        mCrewSize--
        mCrewSize = mCrewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
        return this
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

    companion object {
        const val MIN_CREW_SIZE = 1
        const val MAX_CREW_SIZE = 15
        val CREW_RANGE = (MIN_CREW_SIZE..MAX_CREW_SIZE)

        fun of(value: Int) = if (value == 0) Crew() else Crew(
            iCrewSize = 15.and(value),
            iDidTakeoff = value.getBit(4),
            iDidLanding = value.getBit(5),
            iTakeoffLandingTimes = value.ushr(6)
        )
        fun of(crewSize: Int, didTakeoff: Boolean, didLanding: Boolean, nonStandardTimes: Int) = Crew(crewSize,didTakeoff,didLanding,nonStandardTimes)

        fun asCoco(): Crew = Crew(3, iDidTakeoff = false, iDidLanding = false, iTakeoffLandingTimes = Preferences.standardTakeoffLandingTimes)
    }
}