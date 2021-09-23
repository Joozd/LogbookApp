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
import nl.joozd.logbookapp.extensions.nullIfZero
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
data class Crew(val size: Int = 2,
           val takeoff: Boolean = true,
           val landing: Boolean = true,
           val times: Int = 0)
{
    fun toInt():Int {
        var value = if (size > 15) 15 else size
        value = value.setBit(4, takeoff).setBit(5, landing)
        value += times.shl(6)
        return value
    }

    /**
     * Returns a [Crew] object with [size] set to [crewSize]
     */
    fun withCrewSize(crewSize: Int): Crew = this.copy (size = crewSize)

    /**
     * Returns a [Crew] object with [landing] set to [didLanding]
     */
    fun withLanding(didLanding: Boolean): Crew = this.copy (landing = didLanding)

    /**
     * Returns a [Crew] object with [takeoff] set to [didTakeoff]
     */
    fun withTakeoff(didTakeoff: Boolean): Crew = this.copy (takeoff = didTakeoff)

    /**
     * Returns a [Crew] object with [times] set to [newTimes]
     */
    fun withTimes(newTimes: Int): Crew = this.copy (times = newTimes)


    /**
     * Return amount of time to log. Cannot be negative, so 3 man ops for a 20 min flight with 30 mins to/landing is 0 minutes to log.
     */
    fun getLogTime(totalTime: Int, pic: Boolean): Int{
        if (pic || size <=2) return totalTime
        val t = times.nullIfZero() ?: Preferences.standardTakeoffLandingTimes
        val divideableTime = (totalTime - 2*t).toFloat()
        val timePerShare = divideableTime / size
        val minutesInSeat = (timePerShare*2).toInt()
        return maxOf (minutesInSeat + (if(takeoff) t else 0) + (if (landing) t else 0), 0)
    }

    fun getLogTime(totalTime: Long, pic: Boolean): Long = getLogTime(totalTime.toInt(), pic).toLong()

    fun getLogTime(totalTime: Duration, pic: Boolean): Duration{
        if (pic || size <=2) return totalTime
        return Duration.ofMinutes(getLogTime(totalTime.toMinutes(), pic))
    }

    operator fun inc(): Crew = this.copy (size = (size + 1).putInRange(MIN_CREW_SIZE..MAX_CREW_SIZE))

    operator fun dec(): Crew = this.copy (size = (size - 1).putInRange(MIN_CREW_SIZE..MAX_CREW_SIZE))

    private fun Int.putInRange(range: IntRange): Int {
        require (!range.isEmpty()) { "cannot put an int in a range without elements"}
        return when {
            this in range -> this
            this < range.minOrNull()!! -> range.minOrNull()!!
            this > range.maxOrNull()!! -> range.maxOrNull()!!
            else -> error ("Value $this neither in our outside of $range...")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Crew) return false
        return this.toInt() == other.toInt()
    }

    override fun hashCode(): Int = toInt()

    override fun toString(): String = "Crew(size = $size, takeoff = $takeoff, landing = $landing, toLdgTime = $times)"

    companion object {
        const val MIN_CREW_SIZE = 1
        const val MAX_CREW_SIZE = 15

        val COCO: Crew
            get() = Crew(3, takeoff = false, landing = false, times = Preferences.standardTakeoffLandingTimes)

        fun of(value: Int) = if (value == 0) Crew() else Crew(
            size = 15.and(value),
            takeoff = value.getBit(4),
            landing = value.getBit(5),
            times = value.ushr(6)
        )
        fun of(crewSize: Int, didTakeoff: Boolean, didLanding: Boolean, nonStandardTimes: Int) = Crew(crewSize,didTakeoff,didLanding,nonStandardTimes)

    }
}