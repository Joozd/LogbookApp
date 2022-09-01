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

package nl.joozd.joozdlogcommon

import nl.joozd.serializing.*
import java.time.Instant

/**
 * BasicFLight holds all data of a Flight, and has options for serialization (both to byteArray and to Csv)
 */
data class BasicFlight(
    val flightID: Int,
    val orig: String,
    val dest: String,
    val timeOut: Long,                 // timeOut and timeIn are seconds since epoch
    val timeIn: Long,                  // timeOut and timeIn are seconds since epoch
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
    val unknownToServer: Boolean,              // unknownToServer - can be hard-deleted when cast to flight if this is true
    val autoFill: Boolean,
    val augmentedCrew: Int,
    val DELETEFLAG: Boolean,
    val timeStamp: Long = -1,
    val signature: String = ""
): JoozdSerializable {
    object VERSION {
        const val version = 6
        // version 3: Added signature: String
        // version 4: Booleans now actual Booleans
        // version 5: Added multiPilotTime: Int
        // version 6: Signature is not Base64 encoded anymore
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

    fun toCsv(): String {
        return listOf(
            flightID.toString(),
            orig,
            dest,
            Instant.ofEpochSecond(timeOut).toString(),// from original Flight
            Instant.ofEpochSecond(timeIn).toString(), // from original Flight
            correctedTotalTime.toString(),
            multiPilotTime.toString(),
            nightTime.toString(),
            ifrTime.toString(),
            simTime.toString(),
            aircraft,
            registration,
            name,
            name2,
            takeOffDay.toString(),
            takeOffNight.toString(),
            landingDay.toString(),
            landingNight.toString(),
            autoLand.toString(),
            flightNumber,
            remarks,
            isPIC.toString(),
            isPICUS.toString(),
            isCoPilot.toString(),
            isDual.toString(),
            isInstructor.toString(),
            isSim.toString(),
            isPF.toString(),
            isPlanned.toString(),
            // unknownToServer.toString(),
            autoFill.toString(),
            augmentedCrew.toString(),
            // DELETEFLAG,
            // timeStamp,
            signature
        ).joinToString(";") { it.replace(';', '|') }
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

        fun ofCsv(csvFlight: String): BasicFlight =
        csvFlight.split(';')
        .map{ it.replace('|', ';')}.let { v->
            PROTOTYPE.copy(
                orig = v[1],
                dest = v[2],
                timeOut = Instant.parse(v[3]).epochSecond,
                timeIn = Instant.parse(v[4]).epochSecond,
                correctedTotalTime = v[5].toInt(),
                multiPilotTime = v[6].toInt(),
                nightTime = v[7].toInt(),
                ifrTime = v[8].toInt(),
                simTime = v[9].toInt(),
                aircraft = v[10],
                registration = v[11],
                name = v[12],
                name2 = v[13],
                takeOffDay = v[14].toInt(),
                takeOffNight = v[15].toInt(),
                landingDay = v[16].toInt(),
                landingNight = v[17].toInt(),
                autoLand = v[18].toInt(),
                flightNumber = v[19],
                remarks = v[20],
                isPIC = v[21] == true.toString(),
                isPICUS = v[22] == true.toString(),
                isCoPilot = v[23] == true.toString(),
                isDual = v[24] == true.toString(),
                isInstructor = v[25] == true.toString(),
                isSim = v[26] == true.toString(),
                isPF = v[27] == true.toString(),
                isPlanned = v[28] == true.toString(),
                autoFill = v[29] == true.toString(),
                augmentedCrew = v[30].toInt(),
                signature = v[31]
            )
        }

        val PROTOTYPE by lazy{
            BasicFlight(
                flightID = -1,
                orig = "",
                dest = "",
                timeOut = 0,
                timeIn = 0,
                correctedTotalTime = 0,
                multiPilotTime = 0,
                nightTime = 0,
                ifrTime = 0,
                simTime = 0,
                aircraft = "",
                registration = "",
                name = "",
                name2 = "",
                takeOffDay = 0,
                takeOffNight = 0,
                landingDay = 0,
                landingNight = 0,
                autoLand = 0,
                flightNumber = "",
                remarks = "",
                isPIC = false,
                isPICUS = false,
                isCoPilot = false,
                isDual = false,
                isInstructor = false,
                isSim = false,
                isPF = false,
                isPlanned = true,
                unknownToServer = true,
                autoFill = true,
                augmentedCrew = 0,
                DELETEFLAG = false,
                timeStamp = -1,          // timeStamp is time of synch with server for this flight
                signature = ""
            )
        }

        const val CSV_IDENTIFIER_STRING = "flightID;Origin;dest;timeOut;timeIn;correctedTotalTime;multiPilotTime;nightTime;ifrTime;simTime;aircraftType;registration;name;name2;takeOffDay;takeOffNight;landingDay;landingNight;autoLand;flightNumber;remarks;isPIC;isPICUS;isCoPilot;isDual;isInstructor;isSim;isPF;isPlanned;autoFill;augmentedCrew;signatureSVG"
    }
}