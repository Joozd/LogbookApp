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

package nl.joozd.joozdlogimporter.supportedFileTypes.extractors


import android.util.Base64
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor
import java.time.Instant

class JoozdlogCsvV5Extractor: CompleteLogbookExtractor {
    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight> =
        lines.drop(1).map {
            println ("parsing $it")
            csvFlightToBasicFlightv5(it)
        }

    private fun csvFlightToBasicFlightv5(csvFlight: String): BasicFlight =
        csvFlight.split(';')
            .map{ it.replace('|', ';')}.let { v->
                println("parsing line $v")
                require(BasicFlight.VERSION.version == 5)
                BasicFlight.PROTOTYPE.copy(
                    orig = v[1],
                    dest = v[2],
                    timeOut = Instant.parse(v[3]).epochSecond,
                    timeIn = Instant.parse(v[4]).epochSecond,
                    correctedTotalTime = v[5].toInt(),
                    //multiPilotTime = v[6].toInt(),
                    nightTime = v[6].toInt(),
                    ifrTime = v[7].toInt(),
                    simTime = v[8].toInt(),
                    aircraft = v[9],
                    registration = v[10],
                    name = v[11],
                    name2 = v[12],
                    takeOffDay = v[13].toInt(),
                    takeOffNight = v[14].toInt(),
                    landingDay = v[15].toInt(),
                    landingNight = v[16].toInt(),
                    autoLand = v[17].toInt(),
                    flightNumber = v[18],
                    remarks = v[19],
                    isPIC = v[20] == true.toString(),
                    isPICUS = v[21] == true.toString(),
                    isCoPilot = v[22] == true.toString(),
                    isDual = v[23] == true.toString(),
                    isInstructor = v[24] == true.toString(),
                    isSim = v[25] == true.toString(),
                    isPF = v[26] == true.toString(),
                    isPlanned = v[27] == true.toString(),
                    autoFill = v[28] == true.toString(),
                    augmentedCrew = v[29].toInt(),
                    signature = Base64.decode(v[30], Base64.NO_WRAP).toString(Charsets.UTF_8)
                )
            }
}