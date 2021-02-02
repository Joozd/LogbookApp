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

import java.util.*

/**
 * Removes n Items from front of a LinkedList and returns them as a [List]
 * if List shorted than n, it returns the full list
 *
 */
fun <E> LinkedList<E>.popFirst(n: Int): List<E> {
    return when {
        n >= size -> this.toList().also{ this.clear() }
        n == 0 -> emptyList()
        n < 0 ->  throw IndexOutOfBoundsException("index $n less than 0")
        else -> {
            val subList = subList(0, n)
            subList.toList().also{
                subList.clear()
            }
        }
    }
}