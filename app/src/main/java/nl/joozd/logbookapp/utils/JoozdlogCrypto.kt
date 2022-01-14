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

package nl.joozd.logbookapp.utils

import java.security.SecureRandom

/**
 * Collection of crypto functions
 */
fun generatePassword(length: Int): String{
    // lowest readable UTF-8 encoded character (space)
    val lowestReadable = 0x20.toByte()
    // highest readable UTF-8 encoded character (tilde)
    val highestReadable = 0x7e.toByte()
    val utf8Chars = (lowestReadable..highestReadable)

    val keyBytes = emptyList<Byte>().toMutableList()

    do{
        ByteArray(1).also {
            SecureRandom().nextBytes(it)
            if (it.first() in utf8Chars) {
                keyBytes.add(it.first())
            }
        }
    } while (keyBytes.size < length)

    return keyBytes.toByteArray().toString(Charsets.UTF_8)
}