/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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

package nl.joozd.logbookapp.ui.utils.viewPagerTransformers

class CustomCollection<T>(private val data: Array<T>) {
    val size
        get() = data.size
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CustomCollection<*>) return false

        if (size != other.size) return false

        // return (0 until size).all { data[it] == other.data[it] }
        /* this will return true if [other] has a longer array that has the same
         * start, not sure if you want that, consider
         */
        return data.contentEquals(other.data)
    }
}