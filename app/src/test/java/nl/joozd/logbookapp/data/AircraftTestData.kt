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

package nl.joozd.logbookapp.data

import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.logbookapp.data.dataclasses.AircraftRegistrationWithType
import nl.joozd.logbookapp.data.room.model.PreloadedRegistration

object AircraftTestData {
    val aircraftType1 = AircraftType("Test Aircraft 1 (MP/ME)", "TAC1", multiPilot = true, multiEngine = true)
    val aircraftType2 = AircraftType("Test Aircraft 2 (SP/SE)", "TAC2", multiPilot = false, multiEngine = false)
    val aircraftTypes = listOf(
        aircraftType1,
        aircraftType2
    )

    val arwt1 = AircraftRegistrationWithType("PH-EZE", aircraftType1)
    val arwt2 = AircraftRegistrationWithType("PH-EZF", aircraftType2)
    val overrulingAircraft = AircraftRegistrationWithType("PH-EZA", aircraftType2)
    val updatedArwt1 = AircraftRegistrationWithType("PH-EZE", aircraftType2)
    val regsWithTypes = listOf(arwt1, arwt2)

    val preloaded1 = ForcedTypeData("PH-EZA", aircraftType1.shortName)
    val preloaded2 = ForcedTypeData("PH-EZB", aircraftType2.shortName)
    val preloadedList = listOf(preloaded1, preloaded2)
}