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

package nl.joozd.joozdlogcommon.legacy.basicflight

import nl.joozd.joozdlogcommon.BasicFlight

object BasicFlightUpgrader {
    fun upgrade2to3(old: BasicFlight_version2): BasicFlight = with (old) {
        BasicFlight(flightID = flightID, orig = orig, dest = dest, timeOut = timeOut, timeIn = timeIn, correctedTotalTime = correctedTotalTime, nightTime = nightTime, ifrTime = ifrTime, simTime = simTime,
            aircraft = aircraft, registration = registration, name = name, name2 = name2, takeOffDay = takeOffDay, takeOffNight = takeOffNight, landingDay = landingDay, landingNight = landingNight,
            autoLand = autoLand, flightNumber = flightNumber, remarks = remarks, isPIC = isPIC, isPICUS = isPICUS, isCoPilot = isCoPilot, isDual = isDual, isInstructor = isInstructor, isSim = isSim,
            isPF = isPF, isPlanned = isPlanned, changed = changed, autoFill = autoFill, augmentedCrew = augmentedCrew, DELETEFLAG = DELETEFLAG, timeStamp = timeStamp, signature = ""
        )
    }
}