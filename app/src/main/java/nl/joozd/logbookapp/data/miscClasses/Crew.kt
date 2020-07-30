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

package nl.joozd.logbookapp.data.miscClasses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.extensions.getBit
import nl.joozd.logbookapp.extensions.setBit
import nl.joozd.logbookapp.extensions.toInt

/************************************************************************************
 * CrewValue will store info on augmented crews:                                    *
 * bits 0-3: amount of crew (no crews >15 ppl :) )                                  *
 * bit 4: in seat on takeoff                                                        *
 * bit 5: in seat on landing                                                        *
 * bit 6-31: amount of time reserved for takeoff/landing (standard in settings)     *
 ************************************************************************************/
class Crew(iCrewSize: Int = 2,
           iDidTakeoff: Boolean = true,
           iDidLanding: Boolean = true,
           iTakeoffLandingTimes: Int = 0)
{
    var crewSize: Int = iCrewSize

    var didTakeoff: Boolean = iDidTakeoff

    var didLanding: Boolean = iDidLanding

    var takeoffLandingTimes: Int = iTakeoffLandingTimes

    fun toInt():Int {
        var value = if (crewSize > 15) 15 else crewSize
        value = value.setBit(4, didTakeoff).setBit(5, didLanding)
        value += takeoffLandingTimes.shl(6)
        return value
    }

    /**
     * Return amount of time to log. Cannot be negative, so 3 man ops for a 20 min flight with 30 mins to/landing is 0 minutes to log.
     */
    fun getLogTime(totalTime: Int, pic: Boolean = false): Int{
        if (pic || crewSize <=2) return totalTime
        return maxOf(0, (((totalTime.toFloat()-2*takeoffLandingTimes)/crewSize) * 2 + takeoffLandingTimes * (didTakeoff.toInt() + didLanding.toInt()) + 0.5).toInt())
    }

    operator fun plus(extraCrewMembers: Int): Crew = Crew ((crewSize + extraCrewMembers).putInRange((1..15)), didTakeoff, didLanding, takeoffLandingTimes)
    operator fun minus(extraCrewMembers: Int): Crew = Crew ((crewSize - extraCrewMembers).putInRange((1..15)), didTakeoff, didLanding, takeoffLandingTimes)


    operator fun plusAssign(extraCrewMembers: Int){
        crewSize += extraCrewMembers
        crewSize = crewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
    }

    operator fun minusAssign(fewerCrewMebers: Int){
        crewSize -= fewerCrewMebers
        crewSize = crewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
    }

    operator fun inc(): Crew{
        crewSize++
        crewSize = crewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
        return this
    }

    operator fun dec(): Crew{
        crewSize--
        crewSize = crewSize.putInRange((MIN_CREW_SIZE..MAX_CREW_SIZE))
        return this
    }

    private fun Int.putInRange(range: IntRange): Int {
        require (!range.isEmpty()) { "cannot put an int in a range without elements"}
        return when {
            this in range -> this
            this < range.min()!! -> range.min()!!
            this > range.max()!! -> range.max()!!
            else -> error ("Value $this neither in our outside of $range...")
        }
    }

    companion object {
        const val MIN_CREW_SIZE = 1
        const val MAX_CREW_SIZE = 15

        fun of(value: Int) = if (value == 0) Crew() else Crew(
            iCrewSize = 15.and(value),
            iDidTakeoff = value.getBit(4),
            iDidLanding = value.getBit(5),
            iTakeoffLandingTimes = value.ushr(6)
        )
        fun of(crewSize: Int, didTakeoff: Boolean, didLanding: Boolean, nonStandardTimes: Int) = Crew(crewSize,didTakeoff,didLanding,nonStandardTimes)
    }
}