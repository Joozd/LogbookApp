/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.joozdlogcommon

import nl.joozd.joozdlogcommon.serializing.*
import nl.joozd.joozdlogcommon.serializing.unwrap
import nl.joozd.joozdlogcommon.serializing.wrap

data class BasicAircraft(
    val id: Int,
    val registration: String,
    val manufacturer: String,
    val model: String,
    val engine_type: String,
    val mtow: Int,
    val se: Int,
    val me: Int,
    val multipilot: Int,
    val isIfr: Int
): JoozdlogSerializable {
    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)

        serialized += wrap(component1())
        serialized += wrap(component2())
        serialized += wrap(component3())
        serialized += wrap(component4())
        serialized += wrap(component5())
        serialized += wrap(component6())
        serialized += wrap(component7())
        serialized += wrap(component8())
        serialized += wrap(component9())
        serialized += wrap(component10())

        return serialized
    }
    companion object: JoozdlogSerializable.Creator {

        /**
         * Unfortunately, I don't know how to do this with non-typed functions
         */
        override fun deserialize(source: ByteArray): BasicAircraft {
            val wraps = serializedToWraps(source)
            return BasicAircraft(
                unwrap(wraps[0]),
                unwrap(wraps[1]),
                unwrap(wraps[2]),
                unwrap(wraps[3]),
                unwrap(wraps[4]),
                unwrap(wraps[5]),
                unwrap(wraps[6]),
                unwrap(wraps[7]),
                unwrap(wraps[8]),
                unwrap(wraps[9])
            )
        }
    }



}