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

package nl.joozd.logbookapp.data.miscClasses.crew

import nl.joozd.logbookapp.extensions.getBit
import nl.joozd.logbookapp.extensions.setBit
import java.time.Duration

/**
 * CrewValue stores info on augmented crews: *
 */
// right bit is [0]
data class AugmentedCrew(
    val isFixedTime: Boolean = false,
    val size: Int = 2,
    val takeoff: Boolean = true,
    val landing: Boolean = true,
    val times: Int = 0,
    val undefined: Boolean = false)
{
    /**
     * @See [fromInt] for format
     */
    fun toInt():Int {
        var value = if (size > MAX_CREW_SIZE) MAX_CREW_SIZE else size
        value = value.setBit(3, isFixedTime)
        value = value.setBit(4, takeoff).setBit(5, landing)
        value += times.shl(6)
        return value
    }

    /**
     * A crew is augmented if it is done by more than 2 pilots, or if a fixed rest time is entered.
     */
    fun isAugmented() = size > 2 || (isFixedTime && times != 0)

    /**
     * Returns a [AugmentedCrew] object with [isFixedTime] set to [fixedTime]
     */
    fun withFixedRestTime(fixedTime: Boolean): AugmentedCrew = this.copy(isFixedTime = fixedTime)

    /**
     * Returns a [AugmentedCrew] object with [size] set to [crewSize]
     */
    fun withCrewSize(crewSize: Int): AugmentedCrew = this.copy (size = crewSize)

    /**
     * Returns a [AugmentedCrew] object with [landing] set to [didLanding]
     */
    fun withLanding(didLanding: Boolean): AugmentedCrew = this.copy (landing = didLanding)

    /**
     * Returns a [AugmentedCrew] object with [takeoff] set to [didTakeoff]
     */
    fun withTakeoff(didTakeoff: Boolean): AugmentedCrew = this.copy (takeoff = didTakeoff)

    /**
     * Returns a [AugmentedCrew] object with [times] set to [newTimes]
     */
    fun withTimes(newTimes: Int): AugmentedCrew = this.copy (times = newTimes)


    /**
     * Return amount of time to log. Cannot be negative, so 3 man ops for a 20 min flight with 30 mins to/landing is 0 minutes to log.
     */
    fun getLogTime(totalTime: Int, pic: Boolean): Int{
        if (pic) return totalTime // PIC logs all time
        if(isFixedTime) return totalTime - times // fixed rest time gets subtracted from
        if (size <=2) return totalTime // less than 2 crew logs all time

        val divideableTime = (totalTime - 2*times).toFloat()
        val timePerShare = divideableTime / size
        val minutesInSeat = (timePerShare*2).toInt()
        return maxOf (minutesInSeat + (if(takeoff) times else 0) + (if (landing) times else 0), 0)
    }

    fun getLogTime(totalTime: Long, pic: Boolean): Long = getLogTime(totalTime.toInt(), pic).toLong()

    fun getLogTime(totalTime: Duration, pic: Boolean): Int =
        getLogTime(totalTime.toMinutes(), pic).toInt()

    operator fun inc(): AugmentedCrew = this.copy (size = (size + 1).putInRange(MIN_CREW_SIZE..MAX_CREW_SIZE))

    operator fun dec(): AugmentedCrew = this.copy (size = (size - 1).putInRange(MIN_CREW_SIZE..MAX_CREW_SIZE))

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
        const val MAX_CREW_SIZE = 7

        fun coco(takeoffLandingTimes: Int): AugmentedCrew = AugmentedCrew(isFixedTime = false, size = 3, takeoff = false, landing = false, times = takeoffLandingTimes)

        /**
         * A value of 0 means "undefined"
         * - bits 0-2: amount of crew (no crews >7 )
         * - bit 3: if 1, this is a fixed time. If 0, time is calculated.
         * This reverse order from what makes sense is for backwards compatibility and easier migration.
         * - bit 4: in seat on takeoff
         * - bit 5: in seat on landing
         * - bit 6-31: amount of time reserved for takeoff/landing (standard in settings)
         */
        fun fromInt(value: Int) =
            if (value == 0) AugmentedCrew(times = 0, undefined = true) // 0 is not augmented.
            else AugmentedCrew(
                size = 7.and(value),
                isFixedTime = value.getBit(3),
                takeoff = value.getBit(4),
                landing = value.getBit(5),
                times = value.ushr(6)
            )
    }
}