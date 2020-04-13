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

import nl.joozd.joozdlogcommon.serializing.unwrap
import nl.joozd.joozdlogcommon.serializing.wrap
import nl.joozd.joozdlogcommon.serializing.JoozdlogSerializable


data class BasicAirport(val id: Int, val ident: String, val type: String, val name: String, val latitude_deg: Double, val longitude_deg: Double, val elevation_ft: Int,
                        val continent: String, val iso_country: String, val iso_region: String, val municipality: String, val scheduled_service: String,
                        val gps_code: String, val iata_code: String, val local_code: String, val home_link: String, val wikipedia_link: String, val keywords: String):
    JoozdlogSerializable {
    object VERSION {
        const val version = 1
    }

    /**
     * This uses the component() functions as it will be deserialized by position as well. Changing anything in the order above will also change it in the deserialization.
     * Any serialized data will not be usable after changing anything in the constructor!
     */
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
        serialized += wrap(component11())
        serialized += wrap(component12())
        serialized += wrap(component13())
        serialized += wrap(component14())
        serialized += wrap(component15())
        serialized += wrap(component16())
        serialized += wrap(component17())
        serialized += wrap(component18())
        return serialized
    }

    companion object : JoozdlogSerializable.Creator {
        override fun deserialize(source: ByteArray): BasicAirport {
            val wraps = serializedToWraps(source)
            return BasicAirport(
                unwrap(wraps[0]),
                unwrap(wraps[1]),
                unwrap(wraps[2]),
                unwrap(wraps[3]),
                unwrap(wraps[4]),
                unwrap(wraps[5]),
                unwrap(wraps[6]),
                unwrap(wraps[7]),
                unwrap(wraps[8]),
                unwrap(wraps[9]),
                unwrap(wraps[10]),
                unwrap(wraps[11]),
                unwrap(wraps[12]),
                unwrap(wraps[13]),
                unwrap(wraps[14]),
                unwrap(wraps[15]),
                unwrap(wraps[16]),
                unwrap(wraps[17])
            )


            /*
            return BasicAirport(
                unwrapInt(wraps[0]),
                unwrapString(wraps[1]),
                unwrapString(wraps[2]),
                unwrapString(wraps[3]),
                unwrapDouble(wraps[4]),
                unwrapDouble(wraps[5]),
                unwrapInt(wraps[6]),
                unwrapString(wraps[7]),
                unwrapString(wraps[8]),
                unwrapString(wraps[9]),
                unwrapString(wraps[10]),
                unwrapString(wraps[11]),
                unwrapString(wraps[12]),
                unwrapString(wraps[13]),
                unwrapString(wraps[14]),
                unwrapString(wraps[15]),
                unwrapString(wraps[16]),
                unwrapString(wraps[17])
            )

             */
        }
    }
}
