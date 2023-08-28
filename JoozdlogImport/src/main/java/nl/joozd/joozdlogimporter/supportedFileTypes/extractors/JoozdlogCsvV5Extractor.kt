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


import java.util.Base64
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor
import java.time.Instant

class JoozdlogCsvV5Extractor: CompleteLogbookExtractor {
    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight> =
        when {
            lines.size <= 1 -> emptyList()
            else ->
                lines.drop(1).map {
                    csvFlightToBasicFlightv5(it)
                }
        }

        private fun csvFlightToBasicFlightv5(csvFlight: String): BasicFlight =
            csvFlight.split(';')
                .map{ it.replace('|', ';')}.let { v->
                    BasicFlight.PROTOTYPE.copy(
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
                        signature = Base64.getDecoder().decode(v[31]).toString(Charsets.UTF_8)
                    )
                }

}