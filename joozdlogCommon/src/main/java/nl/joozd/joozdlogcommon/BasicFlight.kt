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

package nl.joozd.joozdlogcommon

import nl.joozd.joozdlogcommon.legacy.basicflight.BasicFlight_version2
import nl.joozd.serializing.*

data class BasicFlight(
    val flightID: Int,
    val orig: String ,
    val dest: String ,
    val timeOut: Long,              // timeOut and timeIn are seconds since epoch
    val timeIn: Long,               // timeOut and timeIn are seconds since epoch
    val correctedTotalTime: Int,
    val multiPilotTime: Int,
    val nightTime: Int,
    val ifrTime:Int,
    val simTime: Int,
    val aircraft: String,
    val registration: String,
    val name: String,
    val name2: String,
    val takeOffDay: Int,
    val takeOffNight: Int,
    val landingDay: Int,
    val landingNight: Int,
    val autoLand: Int,
    val flightNumber: String,
    val remarks: String,
    val isPIC: Boolean,
    val isPICUS: Boolean,
    val isCoPilot: Boolean,
    val isDual: Boolean,
    val isInstructor: Boolean,
    val isSim: Boolean,
    val isPF: Boolean,
    val isPlanned: Boolean,
    val changed: Boolean,
    val autoFill: Boolean,
    val augmentedCrew: Int,
    val DELETEFLAG: Boolean,
    // val signed: Boolean,
    val timeStamp: Long = -1, // timeStamp is time of synch with server for this flight
    val signature: String = ""
): JoozdSerializable {
    object VERSION {
        const val version = 5
        // version 3: Added signature: String
        // version 4: Booleans now actual Booleans
        // version 5: Added multiPilotTime: Int
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
        serialized += wrap(component19())
        serialized += wrap(component20())
        serialized += wrap(component21())
        serialized += wrap(component22())
        serialized += wrap(component23())
        serialized += wrap(component24())
        serialized += wrap(component25())
        serialized += wrap(component26())
        serialized += wrap(component27())
        serialized += wrap(component28())
        serialized += wrap(component29())
        serialized += wrap(component30())
        serialized += wrap(component31())
        serialized += wrap(component32())
        serialized += wrap(component33())
        serialized += wrap(component34())
        serialized += wrap(component35())
//        serialized += wrap(component36())

        return serialized
    }
    companion object: JoozdSerializable.Deserializer<BasicFlight> {

        override fun deserialize(source: ByteArray): BasicFlight {
            val wraps = serializedToWraps(source)
            return BasicFlight(
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
                unwrap(wraps[17]),
                unwrap(wraps[18]),
                unwrap(wraps[19]),
                unwrap(wraps[20]),
                unwrap(wraps[21]),
                unwrap(wraps[22]),
                unwrap(wraps[23]),
                unwrap(wraps[24]),
                unwrap(wraps[25]),
                unwrap(wraps[26]),
                unwrap(wraps[27]),
                unwrap(wraps[28]),
                unwrap(wraps[29]),
                unwrap(wraps[30]),
                unwrap(wraps[31]),
                unwrap(wraps[32]),
                unwrap(wraps[33]),
                unwrap(wraps[34])
            )
        }
    }
}