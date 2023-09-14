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

package nl.joozd.joozdlogcommon

import java.time.Duration

/**
 * CrewValue stores info on augmented crews: *
 */
// right bit is [0]
data class AugmentedCrew(
    val isFixedTime: Boolean = false,
    val size: Int = 2, // this gets ignored for calculations if isFixedTime is true
    val takeoff: Boolean = true,
    val landing: Boolean = true,
    val times: Int = 0,
    val isUndefined: Boolean = false)
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
    val isAugmented get() = size > 2 || (isFixedTime && times != 0)

    /**
     * Returns a [AugmentedCrew] object with [isFixedTime] set to [fixedTime]
     */
    @Suppress("unused")
    fun withFixedRestTime(fixedTime: Boolean): AugmentedCrew = this.copy(isFixedTime = fixedTime)

    /**
     * Returns a [AugmentedCrew] object with [size] set to [crewSize]
     */
    @Suppress("unused")
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
        if (pic) return maxOf (0, totalTime) // PIC logs all time
        if (isFixedTime) return maxOf (0, totalTime - times) // fixed rest time gets subtracted from
        if (size <=2) return maxOf (0, totalTime) // 2 or less crew logs all time

        val divideableTime = (totalTime - 2*times).toFloat()
        val timePerShare = divideableTime / size
        val minutesInSeat = (timePerShare*2).toInt()
        return maxOf (minutesInSeat + (if(takeoff) times else 0) + (if (landing) times else 0), 0)
    }

    fun getLogTime(totalTime: Duration, pic: Boolean): Int =
        getLogTime(totalTime.toMinutes().toInt(), pic)

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
        fun fixedRest(fixedRestTime: Int): AugmentedCrew = AugmentedCrew(isFixedTime = true, times = fixedRestTime)

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
            if (value == 0) AugmentedCrew(times = 0, isUndefined = true) // 0 is not augmented.
            else AugmentedCrew(
                size = 7.and(value),
                isFixedTime = value.getBit(3),
                takeoff = value.getBit(4),
                landing = value.getBit(5),
                times = value.ushr(6)
            )

        private fun Int.getBit(n: Int): Boolean {
            require(n < Int.SIZE_BITS-1) { "$n out of range (0 - ${Int.SIZE_BITS-2}" }
            return this.and(1.shl(n)) > 0 // its more than 0 so the set bit is 1, whatever bit it is
        }

        /**
         *  Set a bit in an Integer.
         *  Use value.setBit([0-31], [true/false])
         *  throws an IllegalArgumentException if requested bit doesn't exist
         *  @return the Integer with the bit set
         */
        private fun Int.setBit(n: Int, value: Boolean): Int{
            require(n < Int.SIZE_BITS) { "$n out of range (0 - ${Int.SIZE_BITS-1}" }
            return if (value)
                this.or(1.shl(n))
            else
                this.inv().or(1.shl(n)).inv()
        }
    }
}