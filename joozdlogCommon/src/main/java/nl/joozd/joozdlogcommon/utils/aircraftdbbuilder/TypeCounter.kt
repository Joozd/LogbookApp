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

package nl.joozd.joozdlogcommon.utils.aircraftdbbuilder

import nl.joozd.joozdlogcommon.serializing.unwrap
import nl.joozd.joozdlogcommon.serializing.wrap
import nl.joozd.joozdlogcommon.serializing.JoozdlogSerializable


data class TypeCounter(val type: AircraftType, val count: Int): JoozdlogSerializable {

    override fun equals(other: Any?): Boolean {
        if (other !is TypeCounter) return false
        return other.type == type
    }
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + count
        return result
    }



    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)
        serialized += wrap(component1().serialize())
        serialized += wrap(component2())
        return serialized
    }


    companion object: JoozdlogSerializable.Creator {

        /**
         * Unfortunately, I don't know how to do this with non-typed functions
         */
        override fun deserialize(source: ByteArray): TypeCounter {
            val wraps = TypeCounter.serializedToWraps(source)
            return TypeCounter(
                AircraftType.deserialize(unwrap(wraps[0])),
                unwrap(wraps[1])
            )
        }
    }

}