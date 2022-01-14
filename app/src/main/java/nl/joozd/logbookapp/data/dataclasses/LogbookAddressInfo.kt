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

package nl.joozd.logbookapp.data.dataclasses

/**
 * Store and address
 * NOTE no line breaks allowed
 */
data class LogbookAddressInfo(val street1: String, val street2: String, val zipcode: String, val city: String, val state: String, val country: String){
    override fun toString() = listOf(street1, street2, zipcode, city, state).joinToString("\n")
    companion object{
        fun ofString(s: String): LogbookAddressInfo = s.split("\n").let{
            require (it.size == 6) { "$it does not have 6 elements"}
            LogbookAddressInfo(it[0], it[1], it[2], it[3], it[4], it[5])
        }
    }
}

