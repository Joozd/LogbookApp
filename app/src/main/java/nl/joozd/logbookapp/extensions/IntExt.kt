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

package nl.joozd.logbookapp.extensions

/**
 *  Get a bit in an Integer.
 *  Use value.getBit([0-31])
 *  throws an IllegalArgumentException if requested bit doesn't exist
 *  does not work on sign bit
 *  @return true if bit is 1, or false if bit is 0
 */
fun Int.getBit(n: Int): Boolean {
    require(n < Int.SIZE_BITS-1) { "$n out of range (0 - ${Int.SIZE_BITS-2}" }
    return this.and(1.shl(n)) > 0 // its more than 0 so the set bit is 1, whatever bit it is
}

/**
 *  Set a bit in an Integer.
 *  Use value.setBit([0-31], [true/false])
 *  throws an IllegalArgumentException if requested bit doesn't exist
 *  @return the Integer with the bit set
 */
fun Int.setBit(n: Int, value: Boolean): Int{
    require(n < Int.SIZE_BITS) { "$n out of range (0 - ${Int.SIZE_BITS-1}" }
    return if (value)
        this.or(1.shl(n))
    else
        this.inv().or(1.shl(n)).inv()
}

/**
 * bitwise mask
 * (eg. if [this] is 1100 and [mask] is 0100 this will return 1000
 */
infix fun Int.mask(mask: Int) = this and mask.inv()


fun Int.minusOneWithMinimumValue(minimumValue: Int) = if (this-1 > minimumValue) this-1 else minimumValue

fun Int.nullIfZero() = if (this == 0) null else this



