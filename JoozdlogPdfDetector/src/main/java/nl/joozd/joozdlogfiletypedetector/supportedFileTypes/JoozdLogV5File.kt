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

package nl.joozd.joozdlogfiletypedetector.supportedFileTypes

import nl.joozd.joozdlogfiletypedetector.interfaces.CompleteLogbookExtractor

class JoozdLogV5File(lines: List<String>): CompleteLogbookFile(lines) {
    override val extractor: CompleteLogbookExtractor
        get() = TODO("Not yet implemented")

    companion object {
        private const val TEXT_TO_SEARCH_FOR =
            "flightID;Origin;dest;timeOut;timeIn;correctedTotalTime;multiPilotTime;nightTime;ifrTime;simTime;aircraftType;registration;name;name2;takeOffDay;takeOffNight;landingDay;landingNight;autoLand;flightNumber;remarks;isPIC;isPICUS;isCoPilot;isDual;isInstructor;isSim;isPF;isPlanned;autoFill;augmentedCrew;signature"

        fun buildIfMatches(lines: List<String>): JoozdLogV5File? =
            if ((lines.firstOrNull() ?: "").startsWith(TEXT_TO_SEARCH_FOR))
                JoozdLogV5File(lines)
            else null
    }
}