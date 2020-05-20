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

package nl.joozd.logbookapp.extensions

/**********************************************************************
 *  Functions to get /set bits in an Integer.                         *
 *  Use value.getBit([0-31]) or value.setBit([0-31], [true/false])    *
 *  Returns true for 1 and false for 0                                *
 *  throws an IllegalArgumentException if requested bit doesn't exist *
 **********************************************************************/


// doesnt work on sign bit
fun Int.getBit(n: Int): Boolean {
    require(n < Int.SIZE_BITS-1) { "$n out of range (0 - ${Int.SIZE_BITS-2}" }
    return this.and(1.shl(n)) > 0 // its more than 0 so the set bit is 1, whatever bit it is
}

// this can set your sign bit
fun Int.setBit(n: Int, value: Boolean): Int{
    require(n < Int.SIZE_BITS) { "$n out of range (0 - ${Int.SIZE_BITS-1}" }
    return if (value) this.or(1.shl(n))
    else this.inv().or(1.shl(n)).inv()
}

fun Int.setBit(n: Int, value: Int): Int{
    require(n < Int.SIZE_BITS && n >= 0) { "$n out of range (0 - ${Int.SIZE_BITS-1}" }
    require(value ==0 || value == 1) { "$value not 0 or 1" }
    return if (value == 1) this.or(1.shl(n))
    else this.inv().or(1.shl(n)).inv()
}

fun Int.pow(n: Int): Int{
    var value = 1
    repeat(n){
        value *= this
    }
    return value
}

/**
 * Returns true if >0, false if <1
 */
fun Int.toBoolean() = this > 0


/**
 * Subtracts one, or sets a minimum value.
 * eg. 3.minusOneWithFloor(0) == 2, 3.minusOneWithFloor(5) = 5
 * @param floor: Minimum value
 */
fun Int.minusOneWithFloor(floor: Int) = if (this-1 > floor) this-1 else floor



