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

data class AircraftType(val name: String, val shortName: String, val multiPilot:Boolean, val multiEngine:Boolean):
    JoozdlogSerializable {
    object VERSION {
        const val version = 1
        // version 1: Initial version
    }

    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)

        serialized += wrap(component1())
        serialized += wrap(component2())
        serialized += wrap(component3())
        serialized += wrap(component4())
        return serialized
    }

    companion object: JoozdlogSerializable.Creator {

        /**
         * Unfortunately, I don't know how to do this with non-typed functions
         */
        override fun deserialize(source: ByteArray): AircraftType {
            val wraps = serializedToWraps(source)
            return AircraftType(
                unwrap(wraps[0]),
                unwrap(wraps[1]),
                unwrap(wraps[2]),
                unwrap(wraps[3])
            )
        }
    }
}